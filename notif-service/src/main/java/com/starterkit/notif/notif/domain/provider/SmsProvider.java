package com.starterkit.notif.notif.domain.provider;

public interface SmsProvider {

    /**
     * Send SMS to a phone number.
     *
     * @param to      phone number (e.g. 09123456789)
     * @param message the message body
     * @return provider response
     */
    ProviderResponse send(String to, String message);

    /**
     * @return provider name (e.g. MOCK, KAVENEGAR, TWILIO)
     */
    String providerName();
}
