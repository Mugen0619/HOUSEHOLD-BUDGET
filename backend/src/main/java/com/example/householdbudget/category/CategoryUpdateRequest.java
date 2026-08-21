package com.example.householdbudget.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * category type is immutable after creation (data-design.md 1.1), so the update
 * request only carries the name.
 */
public record CategoryUpdateRequest(
        @NotBlank(message = "カテゴリ名は必須です")
        @Size(min = 1, max = 20, message = "カテゴリ名は1〜20文字で入力してください")
        String name
) {
}
