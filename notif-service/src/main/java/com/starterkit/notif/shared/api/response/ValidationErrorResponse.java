package com.starterkit.notif.shared.api.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ValidationErrorResponse {

    private final Map<String, String> fields;
}