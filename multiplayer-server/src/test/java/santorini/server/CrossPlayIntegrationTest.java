package santorini.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import online.desktop.NetworkClient;
import online.protocol.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CrossPlayIntegrationTest {
    @LocalServerPort int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private ServerEvent await(BlockingQueue<ServerEvent> queue, String type) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            ServerEvent event = queue.poll(1,TimeUnit.SECONDS);
            if (event != null && event.type().equals("ERROR")) fail(event.code()+": "+event.message());
            if (event != null && event.type().equals(type)) return event;
        }
        throw new AssertionError("No "+type+" received");
    }
    @Test void actualDesktopClientAndBrowserProtocolShareOneMatch() throws Exception {
        BlockingQueue<ServerEvent> desktopEvents = new LinkedBlockingQueue<>(), browserEvents = new LinkedBlockingQueue<>();
        try (NetworkClient desktop = new NetworkClient(desktopEvents::add, status -> {})) {
            desktop.connect("http://localhost:"+port,"Desktop-"+UUID.randomUUID().toString().substring(0,8));
            await(desktopEvents,"WELCOME");
            WebSocket browser = HttpClient.newHttpClient().newWebSocketBuilder().header("Origin","http://localhost:5173")
                    .buildAsync(URI.create("ws://localhost:"+port+"/ws"), new WebSocket.Listener() {
                        final StringBuilder data = new StringBuilder();
                        public void onOpen(WebSocket socket) { socket.request(1); }
                        public CompletionStage<?> onText(WebSocket socket, CharSequence part, boolean last) {
                            data.append(part);
                            if (last) {
                                try { browserEvents.add(mapper.readValue(data.toString(), ServerEvent.class)); }
                                catch (Exception invalid) { throw new RuntimeException(invalid); }
                                data.setLength(0);
                            }
                            socket.request(1); return null;
                        }
                    }).get(10,TimeUnit.SECONDS);
            try {
                browser.sendText("{\"type\":\"HELLO\",\"name\":\"Browser\"}",true).join(); await(browserEvents,"WELCOME");
                desktop.command("CREATE",null,"CLASSIC",null,null,null);
                RoomState host = await(desktopEvents,"STATE").room();
                browser.sendText(mapper.writeValueAsString(new ClientCommand("JOIN",null,null,null,host.id(),null,null,null,null)),true).join();
                RoomState guest = await(browserEvents,"STATE").room(); host = await(desktopEvents,"STATE").room();
                desktop.command("READY",null,null,null,host,null);
                guest = await(browserEvents,"STATE").room(); await(desktopEvents,"STATE");
                browser.sendText(mapper.writeValueAsString(new ClientCommand("READY",null,null,null,null,null,null,guest.version(),null)),true).join();
                guest = await(browserEvents,"STATE").room(); host = await(desktopEvents,"STATE").room();
                assertEquals("PLAYING",guest.status());
                desktop.command("ACTION",null,null,null,host,GameAction.at("PLACE",0,0));
                RoomState browserAfter = await(browserEvents,"STATE").room();
                RoomState desktopAfter = await(desktopEvents,"STATE").room();
                assertEquals(browserAfter.game(), desktopAfter.game());
                assertEquals(0,browserAfter.game().cells().get(0).owner());
                desktop.command("RESIGN",null,null,null,desktopAfter,null);
                assertEquals(1,await(browserEvents,"STATE").room().game().winner());
            } finally { browser.sendClose(WebSocket.NORMAL_CLOSURE,"Test complete").join(); }
        }
    }
    @Test void untrustedBrowserOriginsCannotOpenGameSocket() {
        CompletionException rejected = assertThrows(CompletionException.class, () -> HttpClient.newHttpClient()
                .newWebSocketBuilder().header("Origin","https://untrusted.example")
                .buildAsync(URI.create("ws://localhost:"+port+"/ws"),new WebSocket.Listener() {}).join());
        assertInstanceOf(WebSocketHandshakeException.class,rejected.getCause());
    }
}
