package online.core;

import online.protocol.*;
import java.util.Comparator;

/** Bounded, deterministic practice opponent; it only proposes actions allowed by Match. */
public final class ComputerPlayer {
    private ComputerPlayer() {}
    public static GameAction choose(Match match, int actionsThisTurn) {
        MatchSnapshot s = match.snapshot();
        if (actionsThisTurn >= 12) for (GameAction a : s.legalActions())
            if (a.type().equals("END_MOVE") || a.type().equals("END_TURN")) return a;
        return s.legalActions().stream().filter(a -> !a.type().equals("CANCEL"))
                .max(Comparator.comparingInt(a -> score(s, a))).orElseThrow();
    }
    private static int centre(int r, int c) { return 4 - Math.abs(2-r) - Math.abs(2-c); }
    private static boolean adjacent(MatchSnapshot.Cell a, MatchSnapshot.Cell b) {
        return a != b && Math.abs(a.row()-b.row()) <= 1 && Math.abs(a.col()-b.col()) <= 1;
    }
    private static int move(MatchSnapshot s, MatchSnapshot.Cell origin, MatchSnapshot.Cell dest) {
        if (origin.level() == 2 && dest.level() == 3) return 100_000;
        return dest.level()*100 + centre(dest.row(),dest.col())*5 + (int)s.cells().stream()
                .filter(c -> adjacent(c,dest) && c.worker() == null && !c.dome() && Math.abs(c.level()-dest.level()) <= 1).count();
    }
    private static int score(MatchSnapshot s, GameAction a) {
        if (a.type().equals("SELECT")) {
            var origin = s.cells().stream().filter(c -> a.worker().equals(c.worker())).findFirst().orElseThrow();
            return s.cells().stream().filter(c -> adjacent(origin,c) && c.worker() == null && !c.dome() && Math.abs(c.level()-origin.level()) <= 1)
                    .mapToInt(c -> move(s,origin,c)).max().orElse(-1000);
        }
        if (a.row() == null) return -100;
        var target = s.cells().get(a.row()*5+a.col());
        if (a.type().equals("MOVE")) {
            var origin = s.cells().stream().filter(c -> s.selectedWorker().equals(c.worker())).findFirst().orElseThrow();
            return move(s,origin,target);
        }
        int value = centre(a.row(),a.col()) + target.level()*3;
        if (a.type().equals("BUILD")) {
            for (var c : s.cells()) if (c.owner() != null && c.owner() != s.turn() && c.level() == 2 && adjacent(c,target)) {
                if (target.level() == 3) value += 1000;
                if (target.level() == 2) value -= 1000;
            }
        }
        return value;
    }
}
