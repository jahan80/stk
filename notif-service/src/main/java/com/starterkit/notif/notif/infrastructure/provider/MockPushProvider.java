package com.starterkit.notif.notif.infrastructure.provider;

import com.starterkit.notif.notif.domain.provider.ProviderResponse;
import com.starterkit.notif.notif.domain.provider.PushProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "notif.provider.push",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockPushProvider implements PushProvider {

    @Override
    public ProviderResponse send(String deviceToken, String title, String body) {
        log.info("[MOCK-PUSH] token={}, title={}, body={}", deviceToken, title, body);

        return ProviderResponse.success("mock-push-" + UUID.randomUUID());
    }

    @Override
    public String providerName() {
        return "MOCK";
    }
}
