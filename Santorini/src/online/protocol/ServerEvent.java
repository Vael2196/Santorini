package online.protocol;
public record ServerEvent(String type, String requestId, String token, RoomState room,
                          String code, String message, Identity identity) {
    public record Identity(String id, String name, boolean guest) {}
    public ServerEvent(String type, String requestId, String token, RoomState room, String code, String message) {
        this(type, requestId, token, room, code, message, null);
    }
    public static ServerEvent state(RoomState state) { return new ServerEvent("STATE", null, null, state, null, null); }
    public static ServerEvent error(String requestId, String code, String message) {
        return new ServerEvent("ERROR", requestId, null, null, code, message);
    }
}
