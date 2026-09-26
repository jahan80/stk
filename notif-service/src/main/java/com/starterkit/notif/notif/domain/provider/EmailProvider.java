package com.starterkit.notif.notif.domain.provider;

public interface EmailProvider {

    ProviderResponse send(String to, String subject, String body);

    String providerName();
}
