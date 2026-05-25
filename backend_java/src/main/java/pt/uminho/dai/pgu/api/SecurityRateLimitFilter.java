package pt.uminho.dai.pgu.api;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;
import pt.uminho.dai.pgu.p7_data.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.api.acessos_configuracao.*;
import pt.uminho.dai.pgu.api.operacao_tempo_real.*;
import pt.uminho.dai.pgu.api.analitica_historico.*;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de Segurança Anti-Brute Force e Anti-Spam.
 * Implementa Zero-Trust a nível da rede interna limitando pedidos por IP.
 * Este é um mecanismo em memória. Em produção, recomenda-se Redis + Bucket4j ou API Gateway.
 */
@Component
public class SecurityRateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 60; // Rate Limit Geral
    private static final int MAX_LOGIN_ATTEMPTS = 5;       // Anti-Brute Force

    private final Map<String, TokenBucket> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> loginAttempts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String ip = request.getRemoteAddr();
        String path = httpRequest.getRequestURI();

        // 1. Prevenção de Brute Force nos endpoints de Login
        if (path.contains("/auth/login") || path.contains("/auth/admin/login")) {
            TokenBucket loginBucket = loginAttempts.computeIfAbsent(ip, k -> new TokenBucket(System.currentTimeMillis()));
            if (!loginBucket.tryConsume(MAX_LOGIN_ATTEMPTS, 15 * 60 * 1000)) { // 15 minutos de lockout
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"status\":\"erro\",\"mensagem\":\"Múltiplas tentativas falhadas. Conta bloqueada temporariamente. Política de Zero-Trust aplicada.\"}");
                return;
            }
        }

        // 2. Rate Limiting Geral para Prevenção de DoS
        TokenBucket generalBucket = requestCounts.computeIfAbsent(ip, k -> new TokenBucket(System.currentTimeMillis()));
        if (!generalBucket.tryConsume(MAX_REQUESTS_PER_MINUTE, 60 * 1000)) { // 1 minuto de janela
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"status\":\"erro\",\"mensagem\":\"Muitos pedidos simultâneos (Rate Limit Exceeded). Tente mais tarde.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private static class TokenBucket {
        private final AtomicInteger tokens = new AtomicInteger(0);
        private long windowStart;

        public TokenBucket(long windowStart) {
            this.windowStart = windowStart;
        }

        public synchronized boolean tryConsume(int limit, long windowSizeMillis) {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowSizeMillis) {
                // Reset da janela de tempo
                tokens.set(0);
                windowStart = now;
            }

            if (tokens.incrementAndGet() > limit) {
                return false;
            }
            return true;
        }
    }
}