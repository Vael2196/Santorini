package santorini.server;

import online.core.Match;
import online.core.GameMode;
import online.core.ComputerPlayer;
import org.springframework.dao.DataAccessException;
import org.slf4j.LoggerFactory;
import online.protocol.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Membership changes lock the lobby, then the room; game actions lock only their room. */
@Service
public class RoomService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final Object lobby = new Object();
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final Map<String, Client> connections = new ConcurrentHashMap<>();
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Clock clock;
    private final NameRegistry names;
    private final RankingService rankings;
    private final long reconnectMillis;
    private final int maxRooms;

    @Autowired
    public RoomService(@Value("${game.reconnect-seconds:60}") long reconnectSeconds,
                       @Value("${game.max-rooms:500}") int maxRooms, NameRegistry names, RankingService rankings) {
        this(Clock.systemUTC(), reconnectSeconds, maxRooms, names, rankings);
    }

    RoomService(Clock clock, long reconnectSeconds, int maxRooms) {
        this(clock, reconnectSeconds, maxRooms, new NameRegistry());
    }

    RoomService(Clock clock, long reconnectSeconds, int maxRooms, NameRegistry names) {
        this(clock, reconnectSeconds, maxRooms, names, null);
    }
    RoomService(Clock clock, long reconnectSeconds, int maxRooms, NameRegistry names, RankingService rankings) {
        this.rankings = rankings;
        this.names = names; this.clock = clock; this.reconnectMillis = reconnectSeconds * 1000; this.maxRooms = maxRooms;
    }

    public void receive(Peer peer, ClientCommand command) {
        try {
            if (command.type() == null) throw failure("BAD_REQUEST", "A command type is required");
            if ("HELLO".equals(command.type())) {
                synchronized (lobby) { hello(peer, command); }
                return;
            }
            Client client = connections.get(peer.id());
            if (client == null || client.peer != peer) throw failure("NOT_CONNECTED", "Connect before joining a room");
            client.lastSeen = clock.millis();
            switch (command.type()) {
                case "PING" -> peer.send(new ServerEvent("PONG", command.requestId(), null, null, null, null));
                case "GOODBYE" -> { synchronized (lobby) { endSession(client); } }
                case "CREATE", "QUEUE", "JOIN", "LEAVE", "SOLO" -> {
                    synchronized (lobby) { membership(client, command); }
                }
                case "READY", "CHOOSE_GOD", "ACTION", "RESIGN", "CHAT" -> {
                    Room room = client.room;
                    if (room == null) throw failure("NO_ROOM", "Join a room first");
                    synchronized (room) { if (command.type().equals("CHAT")) chat(client, room, command); else play(client, room, command); }
                }
                default -> throw failure("BAD_REQUEST", "Unknown command");
            }
        } catch (GameFailure failure) {
            peer.send(ServerEvent.error(command.requestId(), failure.code, failure.getMessage()));
            Client client = connections.get(peer.id());
            Room room = client == null ? null : client.room;
            if (room != null && !"CHAT".equals(command.type())) {
                synchronized (room) { if (client.room == room) sendState(client, room); }
            }
        } catch (DataAccessException failure) {
            peer.send(ServerEvent.error(command.requestId(), "SERVICE_UNAVAILABLE", "Account services are unavailable. Try again shortly."));
        } catch (IllegalArgumentException failure) {
            peer.send(ServerEvent.error(command.requestId(), "ILLEGAL_ACTION", failure.getMessage()));
        }
    }

    private void hello(Peer peer, ClientCommand command) {
        if (connections.containsKey(peer.id())) throw failure("BAD_REQUEST", "Already connected");
        Client client;
        if (command.token() != null && !command.token().isBlank()) {
            client = clients.get(command.token());
            if (client == null) throw failure("SESSION_EXPIRED", "Your session expired. Start a new session.");
            if (!Objects.equals(client.accountId, peer.identity() == null ? null : peer.identity().id())) {
                throw failure("SESSION_EXPIRED", "Please log in again to return to that seat.");
            }
            if (client.peer != null) {
                Peer previous = client.peer;
                connections.remove(previous.id());
                client.peer = null;
                previous.close();
            }
        } else {
            if (clients.size() >= maxRooms * 6) throw failure("SERVER_FULL", "The server is busy. Try again later.");
            ServerEvent.Identity identity = peer.identity();
            if (identity != null) {
                client = clients.values().stream().filter(c -> identity.id().equals(c.accountId)).findFirst().orElse(null);
                if (client != null && client.peer != null) throw failure("ACCOUNT_IN_USE", "You already have a seat open in another window.");
                if (client == null) client = new Client(UUID.randomUUID().toString(), identity.name(), identity.id());
            } else {
                String name;
                try { name = NameRegistry.display(command.name()); }
                catch (IllegalArgumentException invalid) { throw failure("BAD_NAME", invalid.getMessage()); }
                client = new Client(UUID.randomUUID().toString(), name, null);
                if (!names.reserveGuest(name, client.token)) throw failure("NAME_TAKEN", "That name already belongs to someone. Try another.");
            }
            clients.put(client.token, client);
        }
        client.peer = peer;
        client.lastSeen = clock.millis();
        connections.put(peer.id(), client);
        peer.send(new ServerEvent("WELCOME", command.requestId(), client.token, null, null, null, new ServerEvent.Identity(client.accountId, client.name, client.accountId == null)));
        if (client.room != null) {
            Room room = client.room;
            synchronized (room) {
                expireDisconnected(room);
                room.disconnectedAt.remove(client.token);
                startIfReady(room);
                room.version++;
                broadcast(room);
                if (client.room == room) history(client, room);
                if (client.room == null) peer.send(new ServerEvent("LEFT", null, null, null, null, "Your room reservation expired"));
            }
        }
    }

    private void membership(Client client, ClientCommand command) {
        if ("LEAVE".equals(command.type())) { leave(client); return; }
        if (client.room != null) throw failure("ALREADY_IN_ROOM", "Leave your current room first");
        if ("JOIN".equals(command.type())) {
            String code = command.roomId() == null ? "" : command.roomId().strip().toUpperCase(Locale.ROOT);
            Room room = rooms.get(code);
            if (room == null) throw failure("ROOM_NOT_FOUND", "No room found with that code");
            synchronized (room) {
                if (room.ranked) throw failure("RANKED_QUEUE_ONLY", "Ranked matches can only be joined through the public queue");
                seat(client, room);
            }
            return;
        }
        String rules = command.rules();
        if (!List.of("CLASSIC", "GODS").contains(rules == null ? "" : rules)) {
            throw failure("BAD_REQUEST", "Choose Classic or God powers");
        }
        GameMode mode = GameMode.parse(command.mode());
        boolean ranked = Boolean.TRUE.equals(command.ranked());
        if (mode.seats == 4 && !rules.equals("CLASSIC")) throw failure("BAD_REQUEST", "Four-player games use Classic rules");
        if (ranked && (!command.type().equals("QUEUE") || mode != GameMode.DUEL || !rules.equals("CLASSIC")))
            throw failure("BAD_REQUEST", "Ranked play uses the public Classic duel queue");
        if (ranked && (client.accountId == null || rankings == null)) throw failure("ACCOUNT_REQUIRED", "Log in to play ranked games");
        if (command.type().equals("SOLO") && (mode != GameMode.DUEL || ranked)) throw failure("BAD_REQUEST", "Computer practice uses an unranked duel");
        if (ranked) client.rating = rankings.profile(client.accountId).rating();
        if (command.type().equals("QUEUE")) {
            Room candidate = ranked ? rankedCandidate(client, clock.millis(), null) : rooms.values().stream()
                    .filter(r -> r.publicRoom && !r.ranked && r.mode == mode && r.rules.equals(rules) && r.status.equals("WAITING")
                            && r.players.size() < mode.seats && r.players.stream().allMatch(c -> c.peer != null))
                    .min(Comparator.comparingLong(r -> r.createdAt)).orElse(null);
            if (candidate != null) { synchronized (candidate) { seat(client, candidate); } return; }
        }
        if (rooms.size() >= maxRooms) throw failure("SERVER_FULL", "The server has reached its room limit");
        String code;
        do { code = roomCode(); } while (rooms.containsKey(code));
        Room room = new Room(code, command.type().equals("QUEUE"), rules, clock.millis(), mode, ranked);
        rooms.put(code, room);
        synchronized (room) {
            seat(client, room);
            if (command.type().equals("SOLO")) {
                Client bot = new Client(UUID.randomUUID().toString(), "Computer (AI)", null, true);
                seat(bot, room); room.ready.set(1, true); room.version++; broadcast(room);
            }
        }
    }
    private long ratingWindow(long waited) { return Math.min(2000, 100 + Math.max(0,waited)/15_000*50); }
    private Room rankedCandidate(Client client, long queuedAt, Room excluded) {
        return rooms.values().stream().filter(r -> r != excluded && r.ranked && r.status.equals("WAITING") && r.players.size() == 1
                && r.players.get(0).peer != null && rankings.canPair(client.accountId,r.players.get(0).accountId)
                && Math.abs(client.rating-r.players.get(0).rating) <= Math.max(ratingWindow(clock.millis()-r.createdAt),ratingWindow(clock.millis()-queuedAt)))
                .min(Comparator.<Room>comparingInt(r -> Math.abs(client.rating-r.players.get(0).rating)).thenComparingLong(r -> r.createdAt)).orElse(null);
    }

    private void seat(Client client, Room room) {
        if (!room.status.equals("WAITING") || room.players.size() == room.mode.seats) {
            throw failure("ROOM_FULL", "This room is full or its match has started");
        }
        room.players.add(client);
        room.gods.add(room.rules.equals("CLASSIC") ? "None" : Match.GODS.stream().filter(g -> !room.gods.contains(g)).findFirst().orElseThrow());
        room.ready.add(false);
        client.room = room;
        room.version++;
        room.updatedAt = clock.millis();
        broadcast(room);
        history(client, room);
    }

    private void play(Client client, Room room, ClientCommand command) {
        if (client.room != room) throw failure("NO_ROOM", "You left this room");
        int seat = room.players.indexOf(client);
        if (seat < 0) throw failure("NO_ROOM", "You are not a player in this room");
        expireDisconnected(room);
        if (command.version() == null || command.version() != room.version) {
            throw failure("STALE_STATE", "The room changed. Your board has been refreshed; try again.");
        }
        if ("RESIGN".equals(command.type())) {
            if (room.match == null || !room.status.equals("PLAYING")) throw failure("BAD_REQUEST", "No active match");
            if (!room.match.active(seat)) throw failure("BAD_REQUEST", "You have already left play");
            room.match.resign(seat); complete(room, "Resignation");
        } else if ("READY".equals(command.type()) || "CHOOSE_GOD".equals(command.type())) {
            if (!room.status.equals("WAITING")) throw failure("BAD_REQUEST", "The match has already started");
            if ("CHOOSE_GOD".equals(command.type())) {
                if (!room.rules.equals("GODS") || !Match.GODS.contains(command.god())) throw failure("BAD_REQUEST", "Unknown god power");
                // A card is unique; changing it also revokes this player's ready state.
                for (int other = 0; other < room.gods.size(); other++) {
                    if (other != seat && room.gods.get(other).equals(command.god())) {
                        if (!room.players.get(other).bot) throw failure("GOD_TAKEN", "Another player has that god power");
                        room.gods.set(other, Match.GODS.stream().filter(g -> !g.equals(command.god()) && !room.gods.contains(g)).findFirst().orElseThrow());
                    }
                }
                room.gods.set(seat, command.god()); room.ready.set(seat, false);
            } else {
                room.ready.set(seat, !room.ready.get(seat));
            }
            startIfReady(room);
        } else {
            if (!room.status.equals("PLAYING")) throw failure("BAD_REQUEST", "The match is not active");
            if (!room.disconnectedAt.isEmpty()) throw failure("PAUSED", "Waiting for your opponent to reconnect");
            room.match.apply(seat, command.action());
            complete(room, "Game over");
        }
        room.version++;
        room.updatedAt = clock.millis();
        broadcast(room);
    }

    private void leave(Client client) {
        Room room = client.room;
        if (room == null) { client.peer.send(new ServerEvent("LEFT", null, null, null, null, null)); return; }
        synchronized (room) {
            int index = room.players.indexOf(client);
            if (room.status.equals("WAITING")) {
                room.players.remove(index); room.gods.remove(index); room.ready.remove(index);
                room.ready.replaceAll(ignored -> false);
            } else if (room.status.equals("PLAYING")) {
                room.match.resign(index); complete(room, "Player left");
            }
            room.disconnectedAt.remove(client.token);
            client.room = null;
            room.version++;
            room.updatedAt = clock.millis();
            broadcast(room);
            if (room.players.stream().noneMatch(p -> !p.bot && p.room == room) && !pendingRating(room)) rooms.remove(room.id, room);
        }
        client.peer.send(new ServerEvent("LEFT", null, null, null, null, null));
    }

    private void startIfReady(Room room) {
        if (room.status.equals("WAITING") && room.players.size() == room.mode.seats
                && room.ready.stream().allMatch(Boolean::booleanValue)
                && room.players.stream().allMatch(p -> p.bot || p.peer != null)) {
            String matchId = UUID.randomUUID().toString();
            if (room.ranked) {
                try { rankings.begin(matchId, room.players.get(0).accountId, room.players.get(1).accountId); }
                catch (DataAccessException | IllegalArgumentException failure) {
                    room.ready.replaceAll(ignored -> false); room.version++; broadcast(room);
                    throw failure("RANKING_UNAVAILABLE", "The ranked match could not start. Please ready up again shortly.");
                }
            }
            room.matchId = matchId;
            room.match = new Match(List.copyOf(room.gods), room.mode); room.status = "PLAYING";
        }
    }

    public void disconnected(Peer peer) {
        synchronized (lobby) {
            Client client = connections.remove(peer.id());
            if (client == null || client.peer != peer) return;
            client.peer = null;
            client.lastSeen = clock.millis();
            if (client.room != null) synchronized (client.room) {
                if (client.room.match == null || client.room.match.active(client.room.players.indexOf(client)))
                    client.room.disconnectedAt.put(client.token, clock.millis());
                client.room.version++;
                broadcast(client.room);
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void cleanup() {
        synchronized (lobby) {
            for (Room waiting : rooms.values().stream().sorted(Comparator.comparingLong(r -> r.createdAt)).toList()) {
                if (!waiting.ranked || !waiting.status.equals("WAITING") || waiting.players.size() != 1 || waiting.players.get(0).peer == null) continue;
                Client candidate = waiting.players.get(0);
                Room target = rankedCandidate(candidate, waiting.createdAt, waiting);
                if (target != null) synchronized (waiting) { synchronized (target) {
                    waiting.players.clear(); waiting.gods.clear(); waiting.ready.clear(); rooms.remove(waiting.id,waiting);
                    seat(candidate,target);
                } }
            }
            for (Room room : rooms.values()) synchronized (room) {
                expireDisconnected(room);
                if (pendingRating(room)) {
                    complete(room,room.result);
                    if (!pendingRating(room)) { room.version++; broadcast(room); }
                }
                long lifetime = room.status.equals("WAITING") ? 10 * 60_000L : 5 * 60_000L;
                if (!room.status.equals("PLAYING") && !pendingRating(room) && clock.millis() - room.updatedAt > lifetime) {
                    for (Client client : room.players) if (client.room == room) {
                        client.room = null;
                        if (client.peer != null) client.peer.send(new ServerEvent("LEFT", null, null, null, null, "Room expired"));
                    }
                    rooms.remove(room.id, room);
                }
            }
            for (Client client : new ArrayList<>(clients.values())) {
                if (client.peer == null && clock.millis() - client.lastSeen >= reconnectMillis) removeSession(client);
            }
        }
    }

    public void signOutAccount(String accountId) {
        synchronized (lobby) {
            for (Client client : new ArrayList<>(clients.values())) if (accountId.equals(client.accountId)) endSession(client);
        }
    }

    private void endSession(Client client) {
        Peer peer = client.peer;
        if (client.room != null && peer != null) leave(client);
        removeSession(client);
        if (peer != null) {
            connections.remove(peer.id()); client.peer = null;
            peer.send(new ServerEvent("BYE", null, null, null, null, null)); peer.close();
        }
    }

    private void removeSession(Client client) {
        if (client.room != null) {
            Room room = client.room;
            synchronized (room) {
                expireDisconnected(room);
                int index = room.players.indexOf(client);
                if (index >= 0 && room.status.equals("WAITING")) {
                    room.players.remove(index); room.gods.remove(index); room.ready.remove(index);
                    room.ready.replaceAll(ignored -> false);
                } else if (index >= 0 && room.status.equals("PLAYING")) {
                    room.match.resign(index); complete(room, "Player signed out");
                }
                client.room = null; room.disconnectedAt.remove(client.token);
                room.version++; broadcast(room);
                if (room.players.stream().noneMatch(p -> !p.bot && p.room == room) && !pendingRating(room)) rooms.remove(room.id, room);
            }
        }
        clients.remove(client.token, client);
        if (client.accountId == null) names.releaseGuest(client.name, client.token);
    }

    private void expireDisconnected(Room room) {
        boolean changed = false;
        for (var entry : new ArrayList<>(room.disconnectedAt.entrySet())) {
            if (clock.millis() - entry.getValue() < reconnectMillis) continue;
            Client client = clients.get(entry.getKey());
            int seat = room.players.indexOf(client);
            if (seat < 0) { room.disconnectedAt.remove(entry.getKey()); continue; }
            if (room.status.equals("PLAYING")) {
                room.match.resign(seat); complete(room, "Reconnect time expired");
            } else if (room.status.equals("WAITING")) {
                room.players.remove(seat); room.gods.remove(seat); room.ready.remove(seat);
                room.ready.replaceAll(ignored -> false);
                client.room = null;
            }
            room.disconnectedAt.remove(entry.getKey()); changed = true;
        }
        if (changed) { room.version++; room.updatedAt = clock.millis(); broadcast(room); }
    }

    private void broadcast(Room room) {
        for (Client client : room.players) if (client.room == room && client.peer != null) sendState(client, room);
    }

    private void sendState(Client client, Room room) {
        List<RoomState.Seat> seats = new ArrayList<>();
        for (int i = 0; i < room.players.size(); i++) {
            Client player = room.players.get(i);
            seats.add(new RoomState.Seat(player.name, room.gods.get(i), room.ready.get(i), player.room == room && (player.bot || player.peer != null), player.bot, room.mode.team(i), player.rating));
        }
        boolean paused = room.status.equals("PLAYING") && !room.disconnectedAt.isEmpty();
        long deadline = paused ? Collections.min(room.disconnectedAt.values()) + reconnectMillis : 0;
        if (client.peer != null) client.peer.send(ServerEvent.state(new RoomState(room.id, room.publicRoom, room.rules,
                room.status, room.version, List.copyOf(seats), room.players.indexOf(client), paused, deadline,
                room.match == null ? null : room.match.snapshot(), room.result, room.mode.name(), room.mode.seats, room.ranked, room.matchId,
                room.ratingChanges == null ? null : room.ratingChanges.get(room.players.indexOf(client)))));
    }

    private static boolean pendingRating(Room room) { return room.ranked && room.status.equals("FINISHED") && room.ratingChanges == null; }
    private void complete(Room room, String reason) {
        if (room.match.snapshot().winner() == null) return;
        room.status = "FINISHED"; room.result = reason;
        if (pendingRating(room)) {
            try { room.ratingChanges = rankings.finish(room.matchId, room.match.snapshot().winner()); }
            catch (DataAccessException error) {
                if (!room.ratingFailureLogged) LoggerFactory.getLogger(RoomService.class).warn("Ranked settlement delayed for {}",room.matchId,error);
                room.ratingFailureLogged = true;
            }
        }
    }
    @Scheduled(fixedDelay=300)
    public void computerTurns() {
        for (Room room : rooms.values()) synchronized (room) {
            if (!room.status.equals("PLAYING") || !room.disconnectedAt.isEmpty()) continue;
            int turn = room.match.snapshot().turn();
            if (!room.players.get(turn).bot) continue;
            if (room.botTurn != turn) { room.botTurn = turn; room.botActions = 0; }
            room.match.apply(turn, ComputerPlayer.choose(room.match, room.botActions++));
            complete(room,"Game over"); room.version++; room.updatedAt = clock.millis(); broadcast(room);
            if (room.match.snapshot().turn() != turn) room.botTurn = -1;
        }
    }
    private void history(Client client, Room room) {
        if (client.peer != null) client.peer.send(new ServerEvent("CHAT_HISTORY",null,null,null,null,null,null,room.id,null,List.copyOf(room.chat)));
    }
    private void chat(Client client, Room room, ClientCommand command) {
        if (client.room != room || !room.players.contains(client)) throw failure("NO_ROOM","Join this room to chat");
        if (room.players.stream().anyMatch(c -> c.bot)) throw failure("NO_CHAT","Chat is unavailable in computer practice");
        String text = ChatFilter.clean(command.text());
        long now = clock.millis();
        while (!client.chatTimes.isEmpty() && now-client.chatTimes.peekFirst() >= 60_000) client.chatTimes.removeFirst();
        if (!client.chatTimes.isEmpty() && now-client.chatTimes.peekLast() < 1000 || client.chatTimes.size() >= 10)
            throw failure("CHAT_LIMIT","Please wait before sending another message");
        if (text.equalsIgnoreCase(client.lastText) && now-client.lastChat < 30_000) throw failure("CHAT_LIMIT","Please avoid repeating the same message");
        client.chatTimes.addLast(now); client.lastText = text; client.lastChat = now;
        ChatMessage message = new ChatMessage(UUID.randomUUID().toString(),room.players.indexOf(client),client.name,text,now);
        room.chat.addLast(message); if (room.chat.size()>50) room.chat.removeFirst();
        for (Client c : room.players) if (c.room == room && c.peer != null)
            c.peer.send(new ServerEvent("CHAT",command.requestId(),null,null,null,null,null,room.id,message,null));
    }

    private static String roomCode() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 8; i++) value.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        return value.toString();
    }
    private static GameFailure failure(String code, String message) { return new GameFailure(code, message); }
    private static final class GameFailure extends RuntimeException {
        final String code;
        GameFailure(String code, String message) { super(message); this.code = code; }
    }
    private static final class Client {
        final String token, name, accountId;
        final boolean bot;
        Integer rating;
        final Deque<Long> chatTimes = new ArrayDeque<>();
        String lastText;
        long lastChat;
        volatile Peer peer;
        volatile Room room;
        volatile long lastSeen;
        Client(String token, String name, String accountId) { this(token,name,accountId,false); }
        Client(String token, String name, String accountId, boolean bot) { this.token = token; this.name = name; this.accountId = accountId; this.bot = bot; }
    }
    private static final class Room {
        final String id, rules;
        final boolean publicRoom, ranked;
        final GameMode mode;
        String matchId;
        List<RoomState.RatingChange> ratingChanges;
        final Deque<ChatMessage> chat = new ArrayDeque<>();
        int botTurn = -1, botActions;
        boolean ratingFailureLogged;
        final long createdAt;
        final List<Client> players = new ArrayList<>();
        final List<String> gods = new ArrayList<>();
        final List<Boolean> ready = new ArrayList<>();
        final Map<String, Long> disconnectedAt = new HashMap<>();
        long updatedAt, version;
        volatile String status = "WAITING";
        String result;
        Match match;
        Room(String id, boolean publicRoom, String rules, long now, GameMode mode, boolean ranked) {
            this.mode = mode; this.ranked = ranked;
            this.id = id; this.publicRoom = publicRoom; this.rules = rules; createdAt = updatedAt = now;
        }
    }
}
