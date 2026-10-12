package online.core;

/** Seats and worker ownership belong to the authoritative rules, not to a client. */
public enum GameMode {
    DUEL(2, 2, false), THREE(3, 2, false), FOUR_FFA(4, 1, false), TEAMS(4, 1, true);
    public final int seats, workers;
    public final boolean teams;
    GameMode(int seats, int workers, boolean teams) { this.seats = seats; this.workers = workers; this.teams = teams; }
    public int team(int seat) { return teams ? seat % 2 : seat; }
    public static GameMode parse(String value) {
        if (value == null) return DUEL;
        try { return valueOf(value); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("Choose a valid game mode"); }
    }
}
