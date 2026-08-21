package com.example.householdbudget.common;

/**
 * Thrown when attempting to delete a category that is still referenced by
 * transactions. Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class CategoryInUseException extends RuntimeException {
    public CategoryInUseException(String message) {
        super(message);
    }
}
