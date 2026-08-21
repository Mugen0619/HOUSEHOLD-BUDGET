package com.example.householdbudget.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "カテゴリ名は必須です")
        @Size(min = 1, max = 20, message = "カテゴリ名は1〜20文字で入力してください")
        String name,

        @NotNull(message = "種別は必須です")
        CategoryType type
) {
}
