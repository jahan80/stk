package com.starterkit.notif.notif.infrastructure.provider;

import com.starterkit.notif.notif.domain.provider.EmailProvider;
import com.starterkit.notif.notif.domain.provider.ProviderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "notif.provider.email",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockEmailProvider implements EmailProvider {

    @Override
    public ProviderResponse send(String to, String subject, String body) {
        log.info("[MOCK-EMAIL] to={}, subject={}, body={}", to, subject, body);

        return ProviderResponse.success("mock-email-" + UUID.randomUUID());
    }

    @Override
    public String providerName() {
        return "MOCK";
    }
}
