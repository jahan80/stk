package com.starterkit.notif.notif.domain.provider;

public interface PushProvider {

    ProviderResponse send(String deviceToken, String title, String body);

    String providerName();
}
