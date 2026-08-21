package com.example.householdbudget.common;

/**
 * Thrown for service-layer validation failures that cannot be expressed with
 * Bean Validation annotations alone (e.g. category/transaction type mismatch,
 * duplicate category name within the same type). Mapped to HTTP 400 by
 * {@link GlobalExceptionHandler}, carrying a single field/message pair in the
 * same shape as a Bean Validation field error.
 */
public class ValidationException extends RuntimeException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
