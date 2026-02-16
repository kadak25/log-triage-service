package com.logtriage.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final int analyzePerMinute;
    private final int analyzeFilePerMinute;

    // key: ip + ":" + routeKey
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${rate-limit.enabled:true}") boolean enabled,
            @Value("${rate-limit.analyze-per-minute:30}") int analyzePerMinute,
            @Value("${rate-limit.analyze-file-per-minute:10}") int analyzeFilePerMinute
    ) {
        this.enabled = enabled;
        this.analyzePerMinute = analyzePerMinute;
        this.analyzeFilePerMinute = analyzeFilePerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) return true;


        String path = request.getRequestURI();
        if (path == null) return true;


        return !(path.startsWith("/api/analyze"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain
    ) throws ServletException, IOException {

        String path = req.getRequestURI();
        String ip = clientIp(req);

        String routeKey = routeKey(path);
        int limit = routeLimit(routeKey);

        Bucket bucket = buckets.computeIfAbsent(ip + ":" + routeKey, k -> newBucket(limit));

        var probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {

            res.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(req, res);
            return;
        }

        long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);

        res.setStatus(429);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.setHeader("Retry-After", String.valueOf(waitSeconds));


        res.getWriter().write("""
            {
              "error": "RATE_LIMITED",
              "message": "Too many requests. Please retry later.",
              "retryAfterSeconds": %d
            }
            """.formatted(waitSeconds));
    }

    private Bucket newBucket(int perMinute) {
        Bandwidth limit = Bandwidth.classic(
                perMinute,
                Refill.greedy(perMinute, Duration.ofMinutes(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }


    private String routeKey(String path) {


        if (path != null && path.contains("/file")) return "analyze-file";
        return "analyze";
    }

    private int routeLimit(String routeKey) {
        return "analyze-file".equals(routeKey) ? analyzeFilePerMinute : analyzePerMinute;
    }

    private String clientIp(HttpServletRequest req) {

        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "unknown";
    }
}
