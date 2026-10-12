package santorini.server;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
public class LeaderboardController {
    private final RankingService rankings;
    public LeaderboardController(RankingService rankings) { this.rankings = rankings; }
    @GetMapping("/api/leaderboard") public ResponseEntity<?> list(@RequestParam(defaultValue="0") int page) {
        if (page < 0 || page > 2000) return ResponseEntity.badRequest().body(java.util.Map.of("message","Choose a valid page"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(rankings.leaderboard(page));
    }
}
