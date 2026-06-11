package ci.ashamaz.languageflash.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting по требованиям ТЗ (раздел 4.3):
 * auth-эндпоинты — 5/мин на IP, parse — 10/мин на пользователя, остальное — default/мин.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final int authPerIp;
    private final int parsePerUser;
    private final int defaultPerUser;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.enabled}") boolean enabled,
                           @Value("${app.rate-limit.auth-per-ip}") int authPerIp,
                           @Value("${app.rate-limit.parse-per-user}") int parsePerUser,
                           @Value("${app.rate-limit.default-per-user}") int defaultPerUser) {
        this.enabled = enabled;
        this.authPerIp = authPerIp;
        this.parsePerUser = parsePerUser;
        this.defaultPerUser = defaultPerUser;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String bucketKey;
        int limit;

        if (uri.startsWith("/api/v1/auth/") && !uri.endsWith("/refresh")) {
            bucketKey = "auth:" + clientIp(request);
            limit = authPerIp;
        } else if (uri.equals("/api/v1/articles/parse")) {
            bucketKey = "parse:" + principalKey(request);
            limit = parsePerUser;
        } else {
            bucketKey = "any:" + principalKey(request);
            limit = defaultPerUser;
        }

        Bucket bucket = buckets.computeIfAbsent(bucketKey + ":" + limit, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(limit).refillGreedy(limit, Duration.ofMinutes(1)).build())
                .build());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Превышен лимит запросов, попробуйте позже\"}");
        }
    }

    private String principalKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            return "u" + p.id();
        }
        return clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        return fwd != null ? fwd.split(",")[0].trim() : request.getRemoteAddr();
    }
}
