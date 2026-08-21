package com.example.householdbudget.transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.householdbudget.category.CategoryType;

public record TransactionResponse(
        Long id,
        LocalDate date,
        Integer amount,
        CategoryType type,
        CategoryRef category,
        String memo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record CategoryRef(Long id, String name) {
    }

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDate(),
                transaction.getAmount(),
                transaction.getType(),
                new CategoryRef(transaction.getCategory().getId(), transaction.getCategory().getName()),
                transaction.getMemo(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }
}
