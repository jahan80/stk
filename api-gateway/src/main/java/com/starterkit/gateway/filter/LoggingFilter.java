package com.starterkit.gateway.filter;

import com.starterkit.gateway.event.RequestReceivedEvent;
import com.starterkit.gateway.event.ResponseSentEvent;
import com.starterkit.gateway.event.publisher.GatewayEventPublisher;
import com.starterkit.gateway.jwt.JwtClaimsExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final GatewayEventPublisher eventPublisher;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();

        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }
        final String finalTraceId = traceId;
        final String requestId = UUID.randomUUID().toString();

        // JWT claims
        String authorization = request.getHeaders().getFirst("Authorization");
        JwtClaimsExtractor.JwtClaims claims = JwtClaimsExtractor.JwtClaims.empty();
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            claims = jwtClaimsExtractor.extract(authorization.substring(7));
        }

        final String clientIp = getClientIp(request);
        final String userAgent = request.getHeaders().getFirst("User-Agent");
        final Long userId = claims.userId();
        final String username = claims.username();
        final String method = request.getMethod().name();
        final String path = request.getPath().value();
        final String query = request.getURI().getQuery();

        // Detect target service from path (reliable)
        final String targetService = resolveTargetService(path);

        // Mutate request
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .header("X-Request-Id", requestId)
                .header(X_FORWARDED_FOR, clientIp)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        // =====================================================
        // Publish REQUEST_RECEIVED
        // =====================================================
        RequestContext ctx = new RequestContext(
                requestId,
                finalTraceId,
                method,
                path,
                query,
                targetService,
                targetService,
                null,
                clientIp,
                userAgent,
                userId,
                username,
                claims.roles()
        );

        eventPublisher.publish(new RequestReceivedEvent(ctx));

        // =====================================================
        // Continue chain, then publish RESPONSE_SENT using doFinally
        // =====================================================
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    try {
                        long duration = System.currentTimeMillis() - startTime;

                        ServerHttpResponse response = mutatedExchange.getResponse();
                        int statusCode = response.getStatusCode() != null
                                ? response.getStatusCode().value()
                                : 0;

                        long responseSize = response.getHeaders().getContentLength();

                        String errorCode = null;
                        String errorMessage = null;
                        if (statusCode >= 400) {
                            errorCode = "HTTP_" + statusCode;
                            errorMessage = "Request failed with status " + statusCode;
                        }

                        log.info("Publishing RESPONSE_SENT: {} {} status={} duration={}ms target={} signal={}",
                                method, path, statusCode, duration, targetService, signalType);

                        eventPublisher.publish(new ResponseSentEvent(
                                requestId,
                                finalTraceId,
                                method,
                                path,
                                targetService,
                                statusCode,
                                duration,
                                responseSize,
                                userId,
                                errorCode,
                                errorMessage
                        ));

                        // Add headers (only if not committed)
                        if (!response.isCommitted()) {
                            response.getHeaders().add(TRACE_ID_HEADER, finalTraceId);
                            response.getHeaders().add("X-Request-Id", requestId);
                        }

                    } catch (Exception ex) {
                        log.error("Failed to publish RESPONSE_SENT", ex);
                    }
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst(X_FORWARDED_FOR);
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private String resolveTargetService(String path) {
        if (path.startsWith("/auth/")) return "auth-service";
        if (path.startsWith("/audit/")) return "audit-service";
        if (path.startsWith("/notify/")) return "notif-service";
        if (path.startsWith("/tickets/")) return "ticket-service";
        if (path.startsWith("/gateway/")) return "api-gateway";
        return "unknown";
    }
}
