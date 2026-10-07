package com.starterkit.commons.web;

/**
 * Contract for per-service API code enums.
 *
 * Each service defines its own enum implementing this interface
 * (e.g. AuthApiCode, TicketApiCode). The shared
 * {@link ApiResponseFactory} uses this interface, not a concrete enum.
 */
public interface ApiCode {

    /** Machine-readable code (e.g. "AUTH-USER-001"). */
    String getCode();

    /** Default human-readable message. */
    String getDefaultMessage();
}
