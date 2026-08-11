package com.verse.store.shared.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ValidationErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, List<String>> fieldErrors) {
}
