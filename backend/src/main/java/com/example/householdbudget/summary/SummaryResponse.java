package com.example.householdbudget.summary;

import java.util.List;

public record SummaryResponse(
        String month,
        int incomeTotal,
        int expenseTotal,
        int balance,
        List<CategoryAmount> incomeByCategory,
        List<CategoryAmount> expenseByCategory
) {
}
