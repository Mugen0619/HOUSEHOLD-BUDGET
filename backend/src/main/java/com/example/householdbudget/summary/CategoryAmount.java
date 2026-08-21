package com.example.householdbudget.summary;

public record CategoryAmount(Long categoryId, String name, int amount) {
    public CategoryAmount add(CategoryAmount other) {
        return new CategoryAmount(categoryId, name, this.amount + other.amount);
    }
}
