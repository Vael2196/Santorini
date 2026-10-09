package santorini.server;

import online.core.Match;
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
    private final long reconnectMillis;
    private final int maxRooms;

    @Autowired
    public RoomService(@Value("${game.reconnect-seconds:60}") long reconnectSeconds,
                       @Value("${game.max-rooms:500}") int maxRooms, NameRegistry names) {
        this(Clock.systemUTC(), reconnectSeconds, maxRooms, names);
    }

    RoomService(Clock clock, long reconnectSeconds, int maxRooms) {
        this(clock, reconnectSeconds, maxRooms, new NameRegistry());
    }

    RoomService(Clock clock, long reconnectSeconds, int maxRooms, NameRegistry names) {
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
                case "CREATE", "QUEUE", "JOIN", "LEAVE" -> {
                    synchronized (lobby) { membership(client, command); }
                }
                case "READY", "CHOOSE_GOD", "ACTION", "RESIGN" -> {
                    Room room = client.room;
                    if (room == null) throw failure("NO_ROOM", "Join a room first");
                    synchronized (room) { play(client, room, command); }
                }
                default -> throw failure("BAD_REQUEST", "Unknown command");
            }
        } catch (GameFailure failure) {
            peer.send(ServerEvent.error(command.requestId(), failure.code, failure.getMessage()));
            Client client = connections.get(peer.id());
            Room room = client == null ? null : client.room;
            if (room != null) {
                synchronized (room) { if (client.room == room) sendState(client, room); }
            }
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
            synchronized (room) { seat(client, room); }
            return;
        }
        String rules = command.rules();
        if (!List.of("CLASSIC", "GODS").contains(rules == null ? "" : rules)) {
            throw failure("BAD_REQUEST", "Choose Classic or God powers");
        }
        if ("QUEUE".equals(command.type())) {
            // FIFO matching: a private room is never considered by the queue.
            for (Room room : rooms.values().stream().sorted(Comparator.comparingLong(r -> r.createdAt)).toList()) {
                synchronized (room) {
                    if (room.publicRoom && room.rules.equals(rules) && room.status.equals("WAITING")
                            && room.players.size() == 1 && room.players.get(0).peer != null) {
                        seat(client, room); return;
                    }
                }
            }
        }
        if (rooms.size() >= maxRooms) throw failure("SERVER_FULL", "The server has reached its room limit");
        String code;
        do { code = roomCode(); } while (rooms.containsKey(code));
        Room room = new Room(code, "QUEUE".equals(command.type()), rules, clock.millis());
        rooms.put(code, room);
        synchronized (room) { seat(client, room); }
    }

    private void seat(Client client, Room room) {
        if (!room.status.equals("WAITING") || room.players.size() == 2) {
            throw failure("ROOM_FULL", "This room is full or its match has started");
        }
        room.players.add(client);
        room.gods.add(room.rules.equals("CLASSIC") ? "None" : Match.GODS.stream().filter(g -> !room.gods.contains(g)).findFirst().orElseThrow());
        room.ready.add(false);
        client.room = room;
        room.version++;
        room.updatedAt = clock.millis();
        broadcast(room);
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
            room.match.resign(seat); room.status = "FINISHED"; room.result = "Resignation";
        } else if ("READY".equals(command.type()) || "CHOOSE_GOD".equals(command.type())) {
            if (!room.status.equals("WAITING")) throw failure("BAD_REQUEST", "The match has already started");
            if ("CHOOSE_GOD".equals(command.type())) {
                if (!room.rules.equals("GODS") || !Match.GODS.contains(command.god())) throw failure("BAD_REQUEST", "Unknown god power");
                // A card is unique; changing it also revokes this player's ready state.
                for (int other = 0; other < room.gods.size(); other++) {
                    if (other != seat && room.gods.get(other).equals(command.god())) throw failure("GOD_TAKEN", "Your opponent has that god power");
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
            if (room.match.snapshot().winner() != null) { room.status = "FINISHED"; room.result = "Game over"; }
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
                room.match.resign(index); room.status = "FINISHED"; room.result = "Opponent left";
            }
            room.disconnectedAt.remove(client.token);
            client.room = null;
            room.version++;
            room.updatedAt = clock.millis();
            broadcast(room);
            if (room.players.stream().noneMatch(p -> p.room == room)) rooms.remove(room.id, room);
        }
        client.peer.send(new ServerEvent("LEFT", null, null, null, null, null));
    }

    private void startIfReady(Room room) {
        if (room.status.equals("WAITING") && room.players.size() == 2
                && room.ready.stream().allMatch(Boolean::booleanValue)
                && room.players.stream().allMatch(p -> p.peer != null)) {
            room.match = new Match(List.copyOf(room.gods)); room.status = "PLAYING";
        }
    }

    public void disconnected(Peer peer) {
        synchronized (lobby) {
            Client client = connections.remove(peer.id());
            if (client == null || client.peer != peer) return;
            client.peer = null;
            client.lastSeen = clock.millis();
            if (client.room != null) synchronized (client.room) {
                client.room.disconnectedAt.put(client.token, clock.millis());
                client.room.version++;
                broadcast(client.room);
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void cleanup() {
        synchronized (lobby) {
            for (Room room : rooms.values()) synchronized (room) {
                expireDisconnected(room);
                long lifetime = room.status.equals("WAITING") ? 10 * 60_000L : 5 * 60_000L;
                if (!room.status.equals("PLAYING") && clock.millis() - room.updatedAt > lifetime) {
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
                    room.match.resign(index); room.status = "FINISHED"; room.result = "Player signed out";
                }
                client.room = null; room.disconnectedAt.remove(client.token);
                room.version++; broadcast(room);
                if (room.players.stream().noneMatch(p -> p.room == room)) rooms.remove(room.id, room);
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
                room.match.resign(seat); room.status = "FINISHED"; room.result = "Reconnect time expired";
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
            seats.add(new RoomState.Seat(player.name, room.gods.get(i), room.ready.get(i), player.room == room && player.peer != null));
        }
        boolean paused = room.status.equals("PLAYING") && !room.disconnectedAt.isEmpty();
        long deadline = paused ? Collections.min(room.disconnectedAt.values()) + reconnectMillis : 0;
        if (client.peer != null) client.peer.send(ServerEvent.state(new RoomState(room.id, room.publicRoom, room.rules,
                room.status, room.version, List.copyOf(seats), room.players.indexOf(client), paused, deadline,
                room.match == null ? null : room.match.snapshot(), room.result)));
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
        volatile Peer peer;
        volatile Room room;
        volatile long lastSeen;
        Client(String token, String name, String accountId) { this.token = token; this.name = name; this.accountId = accountId; }
    }
    private static final class Room {
        final String id, rules;
        final boolean publicRoom;
        final long createdAt;
        final List<Client> players = new ArrayList<>();
        final List<String> gods = new ArrayList<>();
        final List<Boolean> ready = new ArrayList<>();
        final Map<String, Long> disconnectedAt = new HashMap<>();
        long updatedAt, version;
        String status = "WAITING", result;
        Match match;
        Room(String id, boolean publicRoom, String rules, long now) {
            this.id = id; this.publicRoom = publicRoom; this.rules = rules; createdAt = updatedAt = now;
        }
    }
}
