package online.protocol;
import java.util.List;
public record RoomState(String id, boolean publicRoom, String rules, String status, long version,
                        List<Seat> players, int yourSeat, boolean paused, long reconnectDeadline,
                        MatchSnapshot game, String result, String mode, int capacity, boolean ranked,
                        String matchId, RatingChange ratingChange) {
    public RoomState(String id, boolean publicRoom, String rules, String status, long version, List<Seat> players,
                     int yourSeat, boolean paused, long reconnectDeadline, MatchSnapshot game, String result) {
        this(id, publicRoom, rules, status, version, players, yourSeat, paused, reconnectDeadline, game, result, "DUEL", 2, false, null, null);
    }
    public record RatingChange(int before, int after, int delta) {}
    public record Seat(String name, String god, boolean ready, boolean connected, boolean bot, int team, Integer rating) {
        public Seat(String name, String god, boolean ready, boolean connected) { this(name, god, ready, connected, false, 0, null); }
    }
}
