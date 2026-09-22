package com.starterkit.auth.api.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ValidationErrorResponse {

    private final Map<String, String> fields;
}