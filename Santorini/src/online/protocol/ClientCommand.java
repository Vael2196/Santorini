package online.protocol;
/** Session tokens are sent inside HELLO, never in URLs or public snapshots. */
public record ClientCommand(String type, String requestId, String token, String name,
                            String roomId, String rules, String god, Long version, GameAction action) {}
