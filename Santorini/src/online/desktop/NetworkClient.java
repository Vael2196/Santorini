package online.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.protocol.*;
import java.net.URI;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.net.http.*;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.nio.charset.StandardCharsets;

/** The desktop client uses the same JSON protocol as React, with no local online-game authority. */
public final class NetworkClient implements AutoCloseable {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    private final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(10)).build();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "santorini-network"); thread.setDaemon(true); return thread;
    });
    private final Consumer<ServerEvent> events;
    private final Consumer<String> status;
    private volatile WebSocket socket;
    private volatile boolean active;
    private volatile String token;
    private String name;
    private URI endpoint;
    private volatile long generation;
    private final Preferences preferences = Preferences.userNodeForPackage(NetworkClient.class);
    private String sessionKey;
    private CompletableFuture<WebSocket> sending = CompletableFuture.completedFuture(null);

    public NetworkClient(Consumer<ServerEvent> events, Consumer<String> status) {
        this.events = events; this.status = status;
        executor.scheduleAtFixedRate(() -> {
            if (socket != null) send(new ClientCommand("PING", null, null, null, null, null, null, null, null));
        }, 20, 20, TimeUnit.SECONDS);
    }

    /** Account cookies stay in memory; passwords are never placed in user preferences. */
    public CompletableFuture<Void> connectAccount(String address, String name, char[] password, boolean register) {
        URI base;
        try { base = accountBase(address); }
        catch (Exception invalid) { Arrays.fill(password, '\0'); throw invalid; }
        status.accept("Connecting…");
        return CompletableFuture.runAsync(() -> {
            try {
                var csrf = http.send(HttpRequest.newBuilder(base.resolve("/api/auth/csrf")).timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString());
                if (csrf.statusCode() != 200) throw new IllegalArgumentException("The account gate is unavailable.");
                String csrfToken = mapper.readTree(csrf.body()).path("token").asText();
                String body = mapper.writeValueAsString(Map.of("name", name, "password", new String(password)));
                var response = http.send(HttpRequest.newBuilder(base.resolve(register ? "/api/auth/register" : "/api/auth/login"))
                        .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json").header("X-XSRF-TOKEN", csrfToken)
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
                var result = mapper.readTree(response.body());
                if (response.statusCode() != 200) throw new IllegalArgumentException(result.path("message").asText("Could not log in."));
                connectSession(base.toString(), result.path("account").path("name").asText());
            } catch (Exception failure) {
                status.accept("Offline"); throw new CompletionException(failure);
            } finally { Arrays.fill(password, '\0'); }
        }, executor);
    }
    private static URI accountBase(String address) {
        URI base = URI.create(address.strip());
        if (!List.of("http", "https").contains(base.getScheme() == null ? "" : base.getScheme()) || base.getHost() == null
                || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("Use an http:// or https:// server address without credentials, query or fragment.");
        }
        return URI.create(base.getScheme() + "://" + base.getRawAuthority());
    }

    public void connect(String address, String name) {
        cookies.getCookieStore().removeAll();
        connectSession(address, name);
    }

    private void connectSession(String address, String name) {
        if (name == null || name.isBlank() || name.length() > 24) throw new IllegalArgumentException("Use a display name of 1 to 24 characters");
        URI base = URI.create(address.strip());
        String scheme = switch (base.getScheme() == null ? "" : base.getScheme()) {
            case "http", "ws" -> "ws";
            case "https", "wss" -> "wss";
            default -> throw new IllegalArgumentException("Use an http:// or https:// server address");
        };
        if (base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("Enter a server address without credentials, query or fragment");
        }
        endpoint = URI.create(scheme + "://" + base.getRawAuthority() + "/ws");
        this.name = name;
        sessionKey = UUID.nameUUIDFromBytes((endpoint + ":" + name).getBytes(StandardCharsets.UTF_8)).toString();
        token = preferences.get(sessionKey, null);
        active = true;
        open();
    }

    private synchronized void open() {
        if (!active) return;
        long attempt = ++generation;
        status.accept(token == null ? "Connecting…" : "Reconnecting…");
        http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10)).buildAsync(endpoint, new WebSocket.Listener() {
            private final StringBuilder text = new StringBuilder();
            public void onOpen(WebSocket ws) {
                synchronized (NetworkClient.this) {
                    if (!active || generation != attempt) { ws.abort(); return; }
                    socket = ws; sending = CompletableFuture.completedFuture(ws);
                    send(new ClientCommand("HELLO", null, token, name, null, null, null, null, null));
                }
                ws.request(1);
            }
            public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                text.append(data);
                if (text.length() > 100_000) { ws.abort(); return null; }
                if (last) {
                    try {
                        ServerEvent event = mapper.readValue(text.toString(), ServerEvent.class);
                        if (generation != attempt) return null;
                        if (event.type().equals("WELCOME")) { token = event.token(); preferences.put(sessionKey, token); status.accept("Connected"); }
                        if (event.type().equals("BYE")) {
                            active = false; token = null; preferences.remove(sessionKey); status.accept("Offline"); ws.abort();
                        }
                        if ("SESSION_EXPIRED".equals(event.code()) || (event.type().equals("ERROR") && token == null)) {
                            token = null; preferences.remove(sessionKey); active = false; ws.abort(); status.accept("Offline");
                        }
                        events.accept(event);
                    } catch (Exception invalid) { status.accept("Could not read a server update"); }
                    text.setLength(0);
                }
                ws.request(1); return null;
            }
            public CompletionStage<?> onClose(WebSocket ws, int code, String reason) { reconnect(attempt); return null; }
            public void onError(WebSocket ws, Throwable error) { reconnect(attempt); }
        }).exceptionally(error -> { reconnect(attempt); return null; });
    }

    private synchronized void reconnect(long attempt) {
        if (!active || generation != attempt) return;
        generation++; socket = null; status.accept("Reconnecting…");
        executor.schedule(this::open, 2, TimeUnit.SECONDS);
    }

    public synchronized void send(ClientCommand command) {
        WebSocket current = socket;
        if (current == null) return;
        try {
            String json = mapper.writeValueAsString(command);
            sending = sending.handle((ignored, error) -> current).thenCompose(ws -> ws.sendText(json, true));
        } catch (Exception invalid) { status.accept("Could not send this action"); }
    }

    public void command(String type, String roomId, String rules, String god, RoomState room, GameAction action) {
        send(new ClientCommand(type, UUID.randomUUID().toString(), null, null, roomId, rules, god,
                room == null ? null : room.version(), action));
    }

    public CompletableFuture<Void> disconnect() {
        WebSocket current;
        URI base;
        synchronized (this) {
            active = false; generation++; current = socket; socket = null;
            base = endpoint == null ? null : URI.create((endpoint.getScheme().equals("wss") ? "https" : "http") + "://" + endpoint.getRawAuthority());
            token = null; if (sessionKey != null) preferences.remove(sessionKey);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                if (current != null) {
                    current.sendText("{\"type\":\"GOODBYE\"}", true).join();
                    current.sendClose(WebSocket.NORMAL_CLOSURE, "Visit ended").join();
                }
                if (base != null && cookies.getCookieStore().getCookies().stream().anyMatch(cookie -> cookie.getName().equals("santorini-account"))) {
                    var csrf = http.send(HttpRequest.newBuilder(base.resolve("/api/auth/csrf")).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
                    var logout = http.send(HttpRequest.newBuilder(base.resolve("/api/auth/logout")).timeout(Duration.ofSeconds(10))
                            .header("X-XSRF-TOKEN", mapper.readTree(csrf.body()).path("token").asText())
                            .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
                    if (logout.statusCode() != 200) throw new IllegalArgumentException("Could not finish logging out. Please try again.");
                }
            } catch (Exception failure) { throw new CompletionException(failure); }
            finally { cookies.getCookieStore().removeAll(); status.accept("Offline"); }
        }, executor);
    }

    public synchronized void close() {
        active = false; generation++; executor.shutdownNow();
        if (socket != null) socket.sendClose(WebSocket.NORMAL_CLOSURE, "Closed");
        socket = null;
    }
}
