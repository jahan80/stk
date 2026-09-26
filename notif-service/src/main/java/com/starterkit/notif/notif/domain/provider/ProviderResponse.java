package com.starterkit.notif.notif.domain.provider;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProviderResponse {

    private final boolean success;
    private final String providerMessageId;
    private final String errorMessage;

    public static ProviderResponse success(String messageId) {
        return ProviderResponse.builder()
                .success(true)
                .providerMessageId(messageId)
                .build();
    }

    public static ProviderResponse failure(String error) {
        return ProviderResponse.builder()
                .success(false)
                .errorMessage(error)
                .build();
    }
}
