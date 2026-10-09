package santorini.server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import java.util.Map;
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final GameSocketHandler handler;
    private final String[] origins;
    private final AccountService accounts;
    public WebSocketConfig(GameSocketHandler handler, AccountService accounts, @Value("${game.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String[] origins) {
        this.handler = handler; this.accounts = accounts; this.origins = origins;
    }
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws").addInterceptors(new HandshakeInterceptor() {
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Map<String,Object> attributes) {
                if (request instanceof ServletServerHttpRequest servlet) {
                    var session = accounts.session(AuthController.cookie(servlet.getServletRequest()));
                    if (session != null) attributes.put("account", session);
                }
                return true;
            }
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception error) {}
        }).setAllowedOrigins(origins);
    }
}
