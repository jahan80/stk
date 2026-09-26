package com.starterkit.gateway.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Decodes JWT claims WITHOUT verification.
 *
 * Purpose: enrich audit logs with user context.
 * Security: do NOT use for auth decisions.
 *
 * Verification is done by downstream services (auth-service).
 */
@Slf4j
@Component
public class JwtClaimsExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtClaims extract(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return JwtClaims.empty();
            }

            // JWT uses Base64URL encoding
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);

            JsonNode node = objectMapper.readTree(payloadJson);

            Long userId = null;
            if (node.has("sub")) {
                try {
                    userId = Long.parseLong(node.get("sub").asText());
                } catch (NumberFormatException ignored) {}
            }

            String username = node.has("username")
                    ? node.get("username").asText()
                    : null;

            List<String> roles = List.of();
            if (node.has("roles") && node.get("roles").isArray()) {
                roles = StreamSupport.stream(
                                node.get("roles").spliterator(), false)
                        .map(JsonNode::asText)
                        .collect(Collectors.toList());
            }

            return new JwtClaims(userId, username, roles);

        } catch (Exception ex) {
            log.debug("Failed to extract JWT claims: {}", ex.getMessage());
            return JwtClaims.empty();
        }
    }

    public record JwtClaims(
            Long userId,
            String username,
            List<String> roles
    ) {
        public static JwtClaims empty() {
            return new JwtClaims(null, null, List.of());
        }
    }
}
