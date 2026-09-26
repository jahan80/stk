package com.starterkit.gateway.filter;

import java.util.List;

/**
 * Holds request-scoped metadata used for logging.
 */
public record RequestContext(
        String requestId,
        String traceId,
        String method,
        String path,
        String query,
        String routeId,
        String targetService,
        String targetUri,
        String clientIp,
        String userAgent,
        Long userId,
        String username,
        List<String> roles
) {}
