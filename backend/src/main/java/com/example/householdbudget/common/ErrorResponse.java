package com.example.householdbudget.common;

import java.util.List;

/**
 * Common error response shape (data-design.md 4章).
 */
public record ErrorResponse(
        int status,
        String message,
        List<FieldErrorDetail> errors
) {
    public record FieldErrorDetail(String field, String message) {
    }
}
