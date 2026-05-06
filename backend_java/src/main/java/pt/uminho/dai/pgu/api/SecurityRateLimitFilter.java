package pt.uminho.dai.pgu.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Filter — protege contra brute-force e abuso de API.
 *
 * Limites configurados:
 *   - Login (POST /api/auth/login*):  5 tentativas por 15 minutos (anti brute-force)
 *   - Auth geral (/api/auth/*):       12 pedidos por 1 minuto
 *   - API geral (/api/*):            120 pedidos por 1 minuto
 *
 * Inclui limpeza periódica para evitar memory leak sob DDoS.
 */
@Component
public class SecurityRateLimitFilter extends OncePerRequestFilter {
    // Janelas temporais
    private static final long GENERAL_WINDOW_MS = 60_000L;        // 1 minuto
    private static final long LOGIN_WINDOW_MS = 900_000L;         // 15 minutos

    // Limites por janela
    private static final int LOGIN_LIMIT = 5;                     // 5 tentativas / 15 min
    private static final int AUTH_LIMIT_PER_MIN = 12;             // 12 req / min
    private static final int API_LIMIT_PER_MIN = 120;             // 120 req / min

    // Limpeza periódica
    private static final long CLEANUP_INTERVAL_MS = 300_000L;     // 5 min
    private volatile long lastCleanup = System.currentTimeMillis();

    private final Map<String, RateWindow> counters = new ConcurrentHashMap<>();

    private static final class RateWindow {
        volatile long windowStartMs;
        volatile long windowDurationMs;
        final AtomicInteger count;

        private RateWindow(long nowMs, long windowDurationMs) {
            this.windowStartMs = nowMs;
            this.windowDurationMs = windowDurationMs;
            this.count = new AtomicInteger(0);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Limpeza periódica de entradas expiradas — previne memory leak sob DDoS
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            lastCleanup = now;
            counters.entrySet().removeIf(e ->
                now - e.getValue().windowStartMs > e.getValue().windowDurationMs * 2);
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();

        // Determinar categoria do pedido e limites aplicáveis
        boolean isLoginAttempt = path.startsWith("/api/auth/login") && "POST".equalsIgnoreCase(method);
        boolean isAuthEndpoint = path.startsWith("/api/auth/");

        int limit;
        long windowMs;
        String key;

        if (isLoginAttempt) {
            // Brute-force protection: 5 tentativas por 15 minutos
            limit = LOGIN_LIMIT;
            windowMs = LOGIN_WINDOW_MS;
            key = "LOGIN:" + clientIp;
        } else if (isAuthEndpoint) {
            limit = AUTH_LIMIT_PER_MIN;
            windowMs = GENERAL_WINDOW_MS;
            key = "AUTH:" + clientIp;
        } else {
            limit = API_LIMIT_PER_MIN;
            windowMs = GENERAL_WINDOW_MS;
            key = "API:" + clientIp;
        }

        RateWindow rateWindow = counters.computeIfAbsent(key, k -> new RateWindow(now, windowMs));

        synchronized (rateWindow) {
            if (now - rateWindow.windowStartMs >= rateWindow.windowDurationMs) {
                rateWindow.windowStartMs = now;
                rateWindow.count.set(0);
            }
        }

        int current = rateWindow.count.incrementAndGet();
        if (current > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");

            long retryAfterSeconds = (rateWindow.windowDurationMs - (now - rateWindow.windowStartMs)) / 1000;
            response.setHeader("Retry-After", String.valueOf(Math.max(retryAfterSeconds, 1)));

            if (isLoginAttempt) {
                response.getWriter().write(
                    "{\"status\":\"erro\",\"mensagem\":\"Demasiadas tentativas de login. Tente novamente em " +
                    Math.max(retryAfterSeconds / 60, 1) + " minuto(s).\"}");
            } else {
                response.getWriter().write(
                    "{\"status\":\"erro\",\"mensagem\":\"Rate limit excedido. Tente novamente em instantes.\"}");
            }
            return;
        }

        filterChain.doFilter(request, response);
    }
}
