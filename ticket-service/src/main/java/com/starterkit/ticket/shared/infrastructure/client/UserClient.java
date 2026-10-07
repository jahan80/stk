package com.starterkit.ticket.shared.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Service-to-service client for auth-service.
 *
 * Fetches contact info (email, mobile) for external notifications.
 *
 * Failure policy: returns Optional.empty() on ANY error.
 * Notification is best-effort; the business transaction must NOT
 * fail because auth-service is temporarily unavailable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestClient authServiceRestClient;

    @org.springframework.beans.factory.annotation.Value("${internal-api.token:dev-only-internal-token-change-me}")
    private String internalApiToken;

    public Optional<UserContact> getContact(Long userId) {
        if (userId == null) return Optional.empty();

        try {
            JsonNode response = authServiceRestClient.get()
                    .uri("/internal/users/{id}/contact", userId)
                    .header("X-Internal-Token", internalApiToken)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.path("success").asBoolean(false)) {
                log.debug("auth-service returned non-success for userId={}: {}",
                        userId, response);
                return Optional.empty();
            }

            JsonNode data = response.path("data");
            if (data.isMissingNode() || data.isNull()) {
                return Optional.empty();
            }

            UserContact contact = new UserContact();
            contact.setId(data.path("id").asLong());
            contact.setEmail(textOrNull(data, "email"));
            contact.setMobileNumber(textOrNull(data, "mobileNumber"));
            contact.setFirstName(textOrNull(data, "firstName"));
            contact.setLastName(textOrNull(data, "lastName"));

            return Optional.of(contact);

        } catch (Exception ex) {
            log.warn("Failed to fetch contact for userId={}: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Batch lookup: username -> userId.
     * Returns empty map on ANY failure (best-effort).
     */
    public java.util.Map<String, Long> getUsersByUsernames(java.util.Set<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return java.util.Map.of();
        }

        try {
            JsonNode response = authServiceRestClient.post()
                    .uri("/internal/users/by-usernames")
                    .header("X-Internal-Token", internalApiToken)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("usernames", usernames))
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.path("success").asBoolean(false)) {
                log.debug("auth-service by-usernames non-success: {}", response);
                return java.util.Map.of();
            }

            JsonNode data = response.path("data");
            if (!data.isObject()) {
                return java.util.Map.of();
            }

            java.util.Map<String, Long> result = new java.util.HashMap<>();
            data.fields().forEachRemaining(e -> {
                if (e.getValue().isNumber()) {
                    result.put(e.getKey(), e.getValue().asLong());
                }
            });
            return result;

        } catch (Exception ex) {
            log.warn("Failed to resolve usernames {}: {}",
                    usernames, ex.getMessage());
            return java.util.Map.of();
        }
    }

    /**
     * All ADMIN user IDs. Empty list on failure.
     */
    public java.util.List<Long> getAdminIds() {
        try {
            JsonNode response = authServiceRestClient.get()
                    .uri("/internal/users/admins")
                    .header("X-Internal-Token", internalApiToken)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.path("success").asBoolean(false)) {
                return java.util.List.of();
            }

            JsonNode data = response.path("data");
            if (!data.isArray()) {
                return java.util.List.of();
            }

            java.util.List<Long> ids = new java.util.ArrayList<>();
            data.forEach(n -> {
                if (n.isNumber()) ids.add(n.asLong());
            });
            return ids;

        } catch (Exception ex) {
            log.warn("Failed to fetch admin IDs: {}", ex.getMessage());
            return java.util.List.of();
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }
}
