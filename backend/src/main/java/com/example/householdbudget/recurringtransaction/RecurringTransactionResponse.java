package com.example.householdbudget.recurringtransaction;

import java.time.LocalDateTime;

import com.example.householdbudget.category.CategoryType;

public record RecurringTransactionResponse(
        Long id,
        String name,
        Integer amount,
        CategoryType type,
        CategoryRef category,
        Integer executionDay,
        String memo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record CategoryRef(Long id, String name) {
    }

    public static RecurringTransactionResponse from(RecurringTransaction recurringTransaction) {
        return new RecurringTransactionResponse(
                recurringTransaction.getId(),
                recurringTransaction.getName(),
                recurringTransaction.getAmount(),
                recurringTransaction.getType(),
                new CategoryRef(recurringTransaction.getCategory().getId(), recurringTransaction.getCategory().getName()),
                recurringTransaction.getExecutionDay(),
                recurringTransaction.getMemo(),
                recurringTransaction.getCreatedAt(),
                recurringTransaction.getUpdatedAt()
        );
    }
}
