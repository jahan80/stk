package com.starterkit.notif.notif.infrastructure.provider;

import com.starterkit.notif.notif.domain.provider.ProviderResponse;
import com.starterkit.notif.notif.domain.provider.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "notif.provider.sms",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockSmsProvider implements SmsProvider {

    @Override
    public ProviderResponse send(String to, String message) {
        log.info("[MOCK-SMS] to={}, message={}", to, message);

        // Simulate success
        return ProviderResponse.success("mock-sms-" + UUID.randomUUID());
    }

    @Override
    public String providerName() {
        return "MOCK";
    }
}
