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

    public Optional<UserContact> getContact(Long userId) {
        if (userId == null) return Optional.empty();

        try {
            JsonNode response = authServiceRestClient.get()
                    .uri("/internal/users/{id}/contact", userId)
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

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }
}
