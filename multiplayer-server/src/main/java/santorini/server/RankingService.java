package santorini.server;

import online.protocol.RoomState.RatingChange;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** A match UUID is the settlement key. Both ratings and the result commit together. */
@Service
@DependsOn("flywayInitializer")
public class RankingService {
    public record Profile(int rating, int wins, int losses, int games, boolean provisional) {}
    public record Entry(int position, String name, int rating, int wins, int losses, int games) {}
    private final JdbcTemplate db;
    public RankingService(JdbcTemplate db) {
        this.db = db;
        // Boards are in memory: a restart cannot adjudicate an unfinished match.
        db.update("UPDATE ranked_matches SET status = 'ABORTED', finished_at = ? WHERE status = 'ACTIVE'", System.currentTimeMillis());
    }
    public Profile profile(String id) {
        return db.queryForObject("SELECT rating, ranked_wins, ranked_losses FROM player_accounts WHERE id = ?", (rs, row) -> {
            int wins=rs.getInt(2), losses=rs.getInt(3);
            return new Profile(rs.getInt(1), wins, losses, wins+losses, wins+losses < 10);
        }, id);
    }
    public List<Entry> leaderboard(int page) {
        if (page < 0 || page > 2000) throw new IllegalArgumentException("Choose a valid leaderboard page");
        return db.query("SELECT display_name, rating, ranked_wins, ranked_losses FROM player_accounts WHERE ranked_wins + ranked_losses > 0 ORDER BY rating DESC, ranked_wins DESC, id ASC LIMIT 50 OFFSET ?",
                (rs, row) -> new Entry(page*50+row+1, rs.getString(1),rs.getInt(2),rs.getInt(3),rs.getInt(4),rs.getInt(3)+rs.getInt(4)), page*50);
    }
    public boolean canPair(String a, String b) {
        if (a.equals(b)) return false;
        Integer count = db.queryForObject("SELECT COUNT(*) FROM ranked_matches WHERE status = 'COMPLETE' AND finished_at > ? AND ((player_a = ? AND player_b = ?) OR (player_a = ? AND player_b = ?))",
                Integer.class,System.currentTimeMillis()-86_400_000L,a,b,b,a);
        return count != null && count < 3;
    }
    private void lockPlayers(String a, String b) {
        for (String id : List.of(a,b).stream().sorted().toList()) db.queryForList("SELECT id FROM player_accounts WHERE id = ? FOR UPDATE",id);
    }
    @Transactional
    public void begin(String match, String a, String b) {
        lockPlayers(a,b);
        if (!canPair(a,b)) throw new IllegalArgumentException("These players have already played three ranked games together today");
        int active = Objects.requireNonNull(db.queryForObject("SELECT COUNT(*) FROM ranked_matches WHERE status = 'ACTIVE' AND (player_a IN (?,?) OR player_b IN (?,?))",Integer.class,a,b,a,b));
        if (active > 0) throw new IllegalArgumentException("A player already has an active ranked match");
        db.update("INSERT INTO ranked_matches (match_id, player_a, player_b, status, rating_a_before, rating_b_before, started_at) VALUES (?, ?, ?, 'ACTIVE', ?, ?, ?)",match,a,b,profile(a).rating(),profile(b).rating(),System.currentTimeMillis());
    }
    @Transactional
    public List<RatingChange> finish(String match, int winner) {
        if (winner != 0 && winner != 1) throw new IllegalArgumentException("Invalid ranked winner");
        var initial=db.queryForMap("SELECT * FROM ranked_matches WHERE match_id = ?",match);
        String a=(String)initial.get("player_a"), b=(String)initial.get("player_b");
        lockPlayers(a,b);
        var row=db.queryForMap("SELECT * FROM ranked_matches WHERE match_id = ? FOR UPDATE",match);
        int ra=((Number)row.get("rating_a_before")).intValue(), rb=((Number)row.get("rating_b_before")).intValue();
        if ("COMPLETE".equals(row.get("status"))) {
            int afterA=((Number)row.get("rating_a_after")).intValue(), afterB=((Number)row.get("rating_b_after")).intValue();
            return List.of(new RatingChange(ra,afterA,afterA-ra),new RatingChange(rb,afterB,afterB-rb));
        }
        if (!"ACTIVE".equals(row.get("status"))) throw new IllegalArgumentException("This ranked match was voided");
        double expected=1.0/(1+Math.pow(10,(rb-ra)/400.0));
        int delta=(int)Math.round(32*((winner == 0 ? 1 : 0)-expected));
        delta=Math.max(100-ra,Math.min(rb-100,delta));
        db.update("UPDATE player_accounts SET rating = ?, ranked_wins = ranked_wins + ?, ranked_losses = ranked_losses + ? WHERE id = ?",ra+delta,winner==0?1:0,winner==1?1:0,a);
        db.update("UPDATE player_accounts SET rating = ?, ranked_wins = ranked_wins + ?, ranked_losses = ranked_losses + ? WHERE id = ?",rb-delta,winner==1?1:0,winner==0?1:0,b);
        db.update("UPDATE ranked_matches SET status = 'COMPLETE', winner_seat = ?, rating_a_after = ?, rating_b_after = ?, finished_at = ? WHERE match_id = ?",winner,ra+delta,rb-delta,System.currentTimeMillis(),match);
        return List.of(new RatingChange(ra,ra+delta,delta),new RatingChange(rb,rb-delta,-delta));
    }
}
