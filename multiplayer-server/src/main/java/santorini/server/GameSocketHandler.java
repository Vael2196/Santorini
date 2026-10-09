package santorini.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.protocol.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSocketHandler extends TextWebSocketHandler {
    private final RoomService rooms;
    private final ObjectMapper mapper;
    private final AccountService accounts;
    private final Map<String, SocketPeer> peers = new ConcurrentHashMap<>();
    public GameSocketHandler(RoomService rooms, ObjectMapper mapper, AccountService accounts) { this.rooms = rooms; this.mapper = mapper; this.accounts = accounts; }

    @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (peers.size() >= 3000) { session.close(CloseStatus.SERVICE_OVERLOAD); return; }
        session.setTextMessageSizeLimit(4096);
        peers.put(session.getId(), new SocketPeer(session));
    }

    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        SocketPeer peer = peers.get(session.getId());
        if (peer == null) return;
        if (peer.account != null && !accounts.valid(peer.account)) { peer.close(); return; }
        peer.lastActivity = System.currentTimeMillis();
        long second = System.currentTimeMillis() / 1000;
        if (peer.second != second) { peer.second = second; peer.messages = 0; }
        if (++peer.messages > 30) { peer.close(); return; }
        try {
            ClientCommand command = mapper.readValue(message.getPayload(), ClientCommand.class);
            if (command == null || (command.requestId() != null && command.requestId().length() > 80)) throw new IllegalArgumentException();
            rooms.receive(peer, command);
        } catch (Exception invalid) {
            peer.send(ServerEvent.error(null, "BAD_REQUEST", "Invalid command JSON"));
        }
    }

    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SocketPeer peer = peers.remove(session.getId());
        if (peer != null) rooms.disconnected(peer);
    }
    @Override public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception { session.close(); }
    @Override protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        SocketPeer peer = peers.get(session.getId());
        if (peer != null) peer.lastActivity = System.currentTimeMillis();
    }

    @Scheduled(fixedDelay = 20_000)
    public void heartbeat() {
        for (SocketPeer peer : peers.values()) {
            if (peer.account != null && !accounts.valid(peer.account)) { peer.close(); continue; }
            if (System.currentTimeMillis() - peer.lastActivity > 60_000) { peer.close(); continue; }
            if (!peer.authenticated && System.currentTimeMillis() - peer.createdAt > 10_000) { peer.close(); continue; }
            try { peer.session.sendMessage(new PingMessage(ByteBuffer.wrap(new byte[]{1}))); }
            catch (Exception failed) { peer.close(); }
        }
    }

    private final class SocketPeer implements Peer {
        final WebSocketSession session;
        final AccountService.Session account;
        final long createdAt = System.currentTimeMillis();
        volatile long lastActivity = createdAt;
        long second;
        int messages;
        volatile boolean authenticated;
        SocketPeer(WebSocketSession session) {
            this.account = (AccountService.Session) session.getAttributes().get("account");
            this.session = new ConcurrentWebSocketSessionDecorator(session, 3000, 65_536);
        }
        public String id() { return session.getId(); }
        public ServerEvent.Identity identity() {
            return account == null ? null : new ServerEvent.Identity(account.account().id(), account.account().name(), false);
        }
        public void send(ServerEvent event) {
            if (event.type().equals("WELCOME")) authenticated = true;
            try { session.sendMessage(new TextMessage(mapper.writeValueAsString(event))); }
            catch (Exception failed) { close(); }
        }
        public void close() { try { session.close(); } catch (Exception ignored) { } }
    }
}
