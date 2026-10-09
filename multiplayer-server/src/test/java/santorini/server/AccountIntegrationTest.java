package santorini.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.desktop.NetworkClient;
import online.protocol.ServerEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import java.net.*;
import java.net.http.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountIntegrationTest {
    @LocalServerPort int port;
    @Autowired AccountService accounts;
    @Autowired JdbcTemplate db;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String PASSWORD = "a rather sturdy password";
    private String freshName() { return "Scribe-" + UUID.randomUUID().toString().substring(0, 8); }
    private HttpClient browser() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build(); }
    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/" + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> post(HttpClient client, String path, Object body) throws Exception {
        String csrf = mapper.readTree(get(client, "csrf").body()).path("token").asText();
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/" + path))
                .header("Content-Type", "application/json").header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private ServerEvent event(BlockingQueue<ServerEvent> queue, String type) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < until) { var event = queue.poll(500, TimeUnit.MILLISECONDS); if (event != null && event.type().equals(type)) return event; }
        throw new AssertionError("Missing " + type);
    }
    private WebSocket socket(HttpClient client, BlockingQueue<ServerEvent> queue) {
        return client.newWebSocketBuilder().header("Origin", "http://localhost:5173")
                .buildAsync(URI.create("ws://localhost:" + port + "/ws"), new WebSocket.Listener() {
                    StringBuilder text = new StringBuilder();
                    public void onOpen(WebSocket socket) { socket.request(1); }
                    public CompletionStage<?> onText(WebSocket socket, CharSequence part, boolean last) {
                        text.append(part);
                        if (last) { try { queue.add(mapper.readValue(text.toString(), ServerEvent.class)); } catch (Exception error) { throw new RuntimeException(error); } text.setLength(0); }
                        socket.request(1); return null;
                    }
                }).join();
    }
    @Test void registrationPersistsStableIdsHashesAndReservedNames() throws Exception {
        String name = freshName(); HttpClient first = browser();
        var registered = post(first, "register", Map.of("name", name, "password", PASSWORD));
        assertEquals(200, registered.statusCode(), registered.body());
        JsonNode account = mapper.readTree(registered.body()).path("account");
        assertTrue(registered.headers().firstValue("Set-Cookie").orElseThrow().contains("HttpOnly"));
        String encoded = db.queryForObject("SELECT password_hash FROM player_accounts WHERE id = ?", String.class, account.path("id").asText());
        assertNotEquals(PASSWORD, encoded); assertTrue(encoded.startsWith("$2a$12$"));
        assertEquals(409, post(browser(), "register", Map.of("name", " " + name.toUpperCase() + " ", "password", PASSWORD)).statusCode());
        assertEquals(401, post(browser(), "login", Map.of("name", name, "password", "the wrong password")).statusCode());
        HttpClient returning = browser();
        assertEquals(account, mapper.readTree(post(returning, "login", Map.of("name", name.toLowerCase(), "password", PASSWORD)).body()).path("account"));
        NameRegistry restartedNames = new NameRegistry(); new AccountService(db, restartedNames, 168);
        assertFalse(restartedNames.available(name));
        assertEquals(account, mapper.readTree(get(returning, "me").body()).path("account"));
        assertEquals(200, post(returning, "logout", Map.of()).statusCode());
        assertFalse(mapper.readTree(get(returning, "me").body()).path("authenticated").asBoolean());
    }
    @Test void guestAndAccountNamesShareOneNamespaceAndAccountSocketsRequireTheCookie() throws Exception {
        String name = freshName(); HttpClient authenticated = browser();
        assertEquals(200, post(authenticated, "register", Map.of("name", name, "password", PASSWORD)).statusCode());
        BlockingQueue<ServerEvent> accountEvents = new LinkedBlockingQueue<>(), guestEvents = new LinkedBlockingQueue<>();
        WebSocket registered = socket(authenticated, accountEvents), guest = socket(browser(), guestEvents);
        try {
            registered.sendText("{\"type\":\"HELLO\",\"name\":\"Pretend name\"}", true).join();
            ServerEvent welcome = event(accountEvents, "WELCOME");
            assertEquals(name, welcome.identity().name()); assertFalse(welcome.identity().guest());
            guest.sendText(mapper.writeValueAsString(Map.of("type", "HELLO", "name", name.toUpperCase())), true).join();
            assertEquals("NAME_TAKEN", event(guestEvents, "ERROR").code());
            guest.sendText(mapper.writeValueAsString(Map.of("type", "HELLO", "token", welcome.token())), true).join();
            assertEquals("SESSION_EXPIRED", event(guestEvents, "ERROR").code());
            assertEquals(200, post(authenticated, "logout", Map.of()).statusCode());
            event(accountEvents, "BYE");
        } finally { registered.abort(); guest.abort(); }
    }
    @Test void aGuestNameCannotBeRegisteredUntilItsVisitEnds() throws Exception {
        String name = freshName(); BlockingQueue<ServerEvent> events = new LinkedBlockingQueue<>();
        WebSocket guest = socket(browser(), events);
        try {
            guest.sendText(mapper.writeValueAsString(Map.of("type", "HELLO", "name", name)), true).join(); event(events, "WELCOME");
            HttpClient client = browser();
            assertEquals(409, post(client, "register", Map.of("name", name, "password", PASSWORD)).statusCode());
            guest.sendText("{\"type\":\"GOODBYE\"}", true).join(); event(events, "BYE");
            assertEquals(200, post(client, "register", Map.of("name", name, "password", PASSWORD)).statusCode());
        } finally { guest.abort(); }
    }
    @Test void accountMutationsRejectMissingCsrfTokens() throws Exception {
        var response = browser().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/register"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("name", freshName(), "password", PASSWORD)))).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
    }
    @Test void theRealDesktopClientCanCreateAndUseAnAccount() throws Exception {
        String name = freshName(); BlockingQueue<ServerEvent> events = new LinkedBlockingQueue<>();
        try (NetworkClient desktop = new NetworkClient(events::add, status -> {})) {
            char[] password = PASSWORD.toCharArray();
            desktop.connectAccount("http://localhost:" + port, name, password, true).get(15, TimeUnit.SECONDS);
            var welcome = event(events, "WELCOME");
            assertEquals(name, welcome.identity().name()); assertFalse(welcome.identity().guest());
            for (char value : password) assertEquals('\0', value);
            desktop.command("CREATE", null, "CLASSIC", null, null, null);
            assertEquals(name, event(events, "STATE").room().players().get(0).name());
            desktop.disconnect().get(15, TimeUnit.SECONDS);
            assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM account_sessions WHERE account_id = ?", Integer.class, welcome.identity().id()));
        }
    }
}
