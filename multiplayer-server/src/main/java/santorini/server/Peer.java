package santorini.server;
import online.protocol.ServerEvent;
/** Transport boundary; room logic can be tested without a WebSocket container. */
public interface Peer {
    String id();
    default ServerEvent.Identity identity() { return null; }
    void send(ServerEvent event);
    void close();
}
