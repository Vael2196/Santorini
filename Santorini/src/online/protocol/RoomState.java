package online.protocol;
import java.util.List;
public record RoomState(String id, boolean publicRoom, String rules, String status, long version,
                        List<Seat> players, int yourSeat, boolean paused, long reconnectDeadline,
                        MatchSnapshot game, String result) {
    public record Seat(String name, String god, boolean ready, boolean connected) {}
}
