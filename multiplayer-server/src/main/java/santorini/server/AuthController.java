package santorini.server;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public static final String COOKIE = "santorini-account";
    public record Credentials(String name, String password) {}
    private final AccountService accounts;
    private final RoomService rooms;
    private final boolean secure;
    private final Map<String, Attempts> limits = new HashMap<>();
    public AuthController(AccountService accounts, RoomService rooms, @Value("${auth.secure-cookie:false}") boolean secure) {
        this.accounts = accounts; this.rooms = rooms; this.secure = secure;
    }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }
    @GetMapping("/me") public ResponseEntity<?> me(HttpServletRequest request) {
        AccountService.Session session = accounts.session(cookie(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(session == null ? Map.of("authenticated", false) : profile(session.account()));
    }
    @PostMapping(value = "/preferences", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> preferences(@RequestBody AccountService.Preferences body, HttpServletRequest request) {
        AccountService.Session session = accounts.session(cookie(request));
        if (session == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Log in to save settings to your account.");
        accounts.savePreferences(session.account(), body);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profile(session.account()));
    }
    private Map<String, Object> profile(AccountService.Account account) {
        return Map.of("authenticated", true, "account", account, "preferences", accounts.preferences(account));
    }
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@RequestBody Credentials body, HttpServletRequest request) {
        limit("register:" + request.getRemoteAddr(), 6, 3_600_000L);
        return welcome(accounts.register(body.name(), body.password()), request);
    }
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody Credentials body, HttpServletRequest request) {
        limit("login:" + request.getRemoteAddr(), 15, 300_000L);
        String key;
        try { key = NameRegistry.key(body.name()); } catch (IllegalArgumentException bad) { key = "invalid"; }
        limit("name:" + key, 15, 300_000L);
        return welcome(accounts.login(body.name(), body.password()), request);
    }
    @PostMapping("/logout") public ResponseEntity<?> logout(HttpServletRequest request) {
        AccountService.Session session = accounts.session(cookie(request));
        accounts.revoke(session);
        if (session != null) rooms.signOutAccount(session.account().id());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, accountCookie("", 0)).cacheControl(CacheControl.noStore()).body(Map.of("authenticated", false));
    }
    private ResponseEntity<?> welcome(AccountService.Login login, HttpServletRequest request) {
        accounts.revoke(accounts.session(cookie(request)));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, accountCookie(login.token(), accounts.cookieSeconds()))
                .cacheControl(CacheControl.noStore()).body(profile(login.session().account()));
    }
    private String accountCookie(String token, long seconds) {
        return ResponseCookie.from(COOKIE, token).httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(Duration.ofSeconds(seconds)).build().toString();
    }
    public static String cookie(HttpServletRequest request) {
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies()) if (COOKIE.equals(cookie.getName())) return cookie.getValue();
        return null;
    }
    private synchronized void limit(String key, int maximum, long period) {
        long now = System.currentTimeMillis();
        limits.entrySet().removeIf(e -> now >= e.getValue().until);
        if (limits.size() >= 10_000 && !limits.containsKey(key)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Account services are busy. Try again later.");
        Attempts attempts = limits.computeIfAbsent(key, ignored -> new Attempts(now + period));
        if (++attempts.count > maximum) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Give it a few minutes before trying again.");
    }
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalid(IllegalArgumentException error) { return Map.of("message", error.getMessage()); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> failure(ResponseStatusException error) { return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("message", error.getReason())); }
    private static final class Attempts { final long until; int count; Attempts(long until) { this.until = until; } }
}
