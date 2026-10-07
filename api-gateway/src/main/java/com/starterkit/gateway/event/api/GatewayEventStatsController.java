package com.starterkit.gateway.event.api;

import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
import com.starterkit.gateway.event.publisher.GatewayEventPublisherStats;
import com.starterkit.gateway.shared.api.response.ApiCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Operator-facing stats for gateway event publishing.
 *
 * Protected by the gateway's global JwtAuthFilter (path starts with
 * /gateway/, requires ADMIN role).
 */
@RestController
@RequestMapping("/gateway/events")
@RequiredArgsConstructor
public class GatewayEventStatsController {

    private final GatewayEventPublisherStats stats;
    private final ApiResponseFactory responseFactory;

    @GetMapping("/stats")
    public Mono<ApiResponse<GatewayEventPublisherStats.Snapshot>> stats() {
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                stats.snapshot()
        ));
    }
}
