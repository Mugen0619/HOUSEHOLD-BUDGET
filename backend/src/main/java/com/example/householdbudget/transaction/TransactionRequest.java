package com.example.householdbudget.transaction;

import java.time.LocalDate;

import com.example.householdbudget.category.CategoryType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransactionRequest(
        @NotNull(message = "日付は必須です")
        LocalDate date,

        @NotNull(message = "金額は必須です")
        @Positive(message = "金額は1以上の整数で入力してください")
        Integer amount,

        @NotNull(message = "種別は必須です")
        CategoryType type,

        @NotNull(message = "カテゴリは必須です")
        Long categoryId,

        @Size(max = 500, message = "メモは500文字以内で入力してください")
        String memo
) {
}
