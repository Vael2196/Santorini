package santorini.server;

import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** One name namespace for guests and accounts in this single-server deployment. */
@Component
public final class NameRegistry {
    private final Map<String, String> guests = new HashMap<>();
    private final Map<String, String> accounts = new HashMap<>();

    public static String display(String value) {
        String name = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC).strip().replaceAll("\\s+", " ");
        if (!name.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} _'\\-]{0,23}")) {
            throw new IllegalArgumentException("Choose a name of 1–24 letters, numbers, spaces, apostrophes, hyphens or underscores.");
        }
        return name;
    }
    public static String key(String name) { return display(name).toLowerCase(Locale.ROOT); }
    public synchronized boolean available(String name) {
        String key = key(name); return !guests.containsKey(key) && !accounts.containsKey(key);
    }
    public synchronized boolean reserveGuest(String name, String token) {
        String key = key(name);
        if (accounts.containsKey(key) || guests.containsKey(key)) return false;
        guests.put(key, token); return true;
    }
    public synchronized void releaseGuest(String name, String token) { guests.remove(key(name), token); }
    public synchronized void reserveAccount(String name, String id) { accounts.put(key(name), id); }
}
