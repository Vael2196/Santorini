package santorini.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@Service
@DependsOn("flywayInitializer")
public class AccountService {
    public record Account(String id, String name) {}
    public record Session(String hash, Account account, long expiresAt) {}
    public record Login(String token, Session session) {}
    public record Preferences(String cursorColour, Boolean showLevels, Boolean soundEnabled, Integer volume) {
        public Preferences(String cursorColour, Boolean showLevels, Boolean soundEnabled) { this(cursorColour, showLevels, soundEnabled, 80); }
        public Preferences {
            if (volume == null) volume = 80;
            if (volume < 0 || volume > 100) throw new IllegalArgumentException("Choose a volume from 0 to 100");
            if (cursorColour == null || !List.of("brown", "charcoal", "teal", "ivory").contains(cursorColour)
                    || showLevels == null || soundEnabled == null) {
                throw new IllegalArgumentException("Choose a cursor colour and set both the tower-level and sound options.");
            }
        }
    }
    private final JdbcTemplate db;
    private final NameRegistry names;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
    private final Semaphore passwordSlots = new Semaphore(2);
    private final String dummyHash = passwords.encode(UUID.randomUUID().toString());
    private final long lifetime;
    private final SecureRandom random = new SecureRandom();

    public AccountService(JdbcTemplate db, NameRegistry names, @Value("${auth.session-hours:168}") long hours) {
        this.db = db; this.names = names; lifetime = Math.max(1, Math.min(hours, 720)) * 3_600_000L;
        db.query("SELECT id, display_name FROM player_accounts", rs -> {
            names.reserveAccount(rs.getString("display_name"), rs.getString("id"));
        });
    }
    public Login register(String suppliedName, String password) {
        String name = NameRegistry.display(suppliedName);
        validatePassword(password);
        acquirePasswordSlot();
        try {
            String encoded = passwords.encode(password);
            synchronized (names) {
                if (!names.available(name)) throw taken();
                Account account = new Account(UUID.randomUUID().toString(), name);
                try {
                    db.update("INSERT INTO player_accounts (id, name_key, display_name, password_hash, created_at) VALUES (?, ?, ?, ?, ?)",
                            account.id(), hash(NameRegistry.key(name)), name, encoded, System.currentTimeMillis());
                } catch (DuplicateKeyException duplicate) { throw taken(); }
                names.reserveAccount(name, account.id());
                return startSession(account);
            }
        } finally { passwordSlots.release(); }
    }
    public Login login(String suppliedName, String password) {
        String key;
        try { key = NameRegistry.key(suppliedName); }
        catch (IllegalArgumentException bad) { key = ""; }
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) throw credentials();
        var records = db.query("SELECT id, display_name, password_hash FROM player_accounts WHERE name_key = ?",
                (rs, row) -> new Stored(new Account(rs.getString("id"), rs.getString("display_name")), rs.getString("password_hash")), hash(key));
        acquirePasswordSlot();
        try {
            Stored record = records.isEmpty() ? null : records.get(0);
            boolean matches = passwords.matches(password, record == null ? dummyHash : record.hash());
            if (!matches || record == null) throw credentials();
            return startSession(record.account());
        } finally { passwordSlots.release(); }
    }
    public Session session(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return null;
        return sessionByHash(hash(token));
    }
    private Session sessionByHash(String tokenHash) {
        List<Session> result = db.query("SELECT s.token_hash, s.expires_at, a.id, a.display_name FROM account_sessions s JOIN player_accounts a ON a.id = s.account_id WHERE s.token_hash = ? AND s.expires_at > ?",
                (rs, row) -> new Session(rs.getString("token_hash"), new Account(rs.getString("id"), rs.getString("display_name")), rs.getLong("expires_at")), tokenHash, System.currentTimeMillis());
        return result.isEmpty() ? null : result.get(0);
    }
    public boolean valid(Session session) { return session != null && session.expiresAt() > System.currentTimeMillis() && sessionByHash(session.hash()) != null; }
    public void revoke(Session session) { if (session != null) db.update("DELETE FROM account_sessions WHERE token_hash = ?", session.hash()); }
    public long cookieSeconds() { return lifetime / 1000; }
    public Preferences preferences(Account account) {
        return db.queryForObject("SELECT cursor_colour, show_levels, sound_enabled, sound_volume FROM player_accounts WHERE id = ?",
                (rs, row) -> new Preferences(rs.getString("cursor_colour"), rs.getBoolean("show_levels"), rs.getBoolean("sound_enabled"), rs.getInt("sound_volume")), account.id());
    }
    public void savePreferences(Account account, Preferences preferences) {
        db.update("UPDATE player_accounts SET cursor_colour = ?, show_levels = ?, sound_enabled = ?, sound_volume = ? WHERE id = ?",
                preferences.cursorColour(), preferences.showLevels(), preferences.soundEnabled(), preferences.volume(), account.id());
    }
    private Login startSession(Account account) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Session session = new Session(hash(token), account, System.currentTimeMillis() + lifetime);
        // Bound sessions per account, including repeated logins from clients.
        db.update("DELETE FROM account_sessions WHERE account_id = ? AND expires_at <= ?", account.id(), System.currentTimeMillis());
        var old = db.query("SELECT token_hash FROM account_sessions WHERE account_id = ? ORDER BY expires_at DESC", (rs, row) -> rs.getString(1), account.id());
        for (int i = 9; i < old.size(); i++) db.update("DELETE FROM account_sessions WHERE token_hash = ?", old.get(i));
        db.update("INSERT INTO account_sessions (token_hash, account_id, expires_at) VALUES (?, ?, ?)", session.hash(), account.id(), session.expiresAt());
        return new Login(token, session);
    }
    @Scheduled(fixedDelay = 60_000) public void cleanup() { db.update("DELETE FROM account_sessions WHERE expires_at <= ?", System.currentTimeMillis()); }
    private void acquirePasswordSlot() {
        if (!passwordSlots.tryAcquire()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account services are busy. Try again in a moment.");
    }
    private static void validatePassword(String password) {
        if (password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Use a password of at least 12 characters and no more than 72 UTF-8 bytes.");
        }
    }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception unavailable) { throw new IllegalStateException(unavailable); }
    }
    private static ResponseStatusException taken() { return new ResponseStatusException(HttpStatus.CONFLICT, "That name already belongs to someone. Try another."); }
    private static ResponseStatusException credentials() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The name or password does not match."); }
    private record Stored(Account account, String hash) {}
}
