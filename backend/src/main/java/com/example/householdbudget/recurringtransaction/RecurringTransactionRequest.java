package com.example.householdbudget.recurringtransaction;

import com.example.householdbudget.category.CategoryType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RecurringTransactionRequest(
        @NotBlank(message = "名称は必須です")
        @Size(min = 1, max = 20, message = "名称は1〜20文字で入力してください")
        String name,

        @NotNull(message = "金額は必須です")
        @Positive(message = "金額は1以上の整数で入力してください")
        Integer amount,

        @NotNull(message = "種別は必須です")
        CategoryType type,

        @NotNull(message = "カテゴリは必須です")
        Long categoryId,

        @NotNull(message = "実行日は必須です")
        @Min(value = 1, message = "実行日は1〜31の範囲で入力してください")
        @Max(value = 31, message = "実行日は1〜31の範囲で入力してください")
        Integer executionDay,

        @Size(max = 500, message = "メモは500文字以内で入力してください")
        String memo
) {
}
