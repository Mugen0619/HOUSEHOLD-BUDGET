package com.example.householdbudget.summary;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.householdbudget.category.CategoryType;
import com.example.householdbudget.transaction.Transaction;
import com.example.householdbudget.transaction.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SummaryService {

    private final TransactionRepository transactionRepository;

    public SummaryService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public SummaryResponse getSummary(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<Transaction> transactions = transactionRepository.findByDateBetween(from, to);

        Map<Long, CategoryAmount> incomeByCategory = new LinkedHashMap<>();
        Map<Long, CategoryAmount> expenseByCategory = new LinkedHashMap<>();
        int incomeTotal = 0;
        int expenseTotal = 0;

        for (Transaction transaction : transactions) {
            Long categoryId = transaction.getCategory().getId();
            String categoryName = transaction.getCategory().getName();
            CategoryAmount entry = new CategoryAmount(categoryId, categoryName, transaction.getAmount());

            if (transaction.getType() == CategoryType.INCOME) {
                incomeTotal += transaction.getAmount();
                incomeByCategory.merge(categoryId, entry, CategoryAmount::add);
            } else {
                expenseTotal += transaction.getAmount();
                expenseByCategory.merge(categoryId, entry, CategoryAmount::add);
            }
        }

        return new SummaryResponse(
                month.toString(),
                incomeTotal,
                expenseTotal,
                incomeTotal - expenseTotal,
                new ArrayList<>(incomeByCategory.values()),
                new ArrayList<>(expenseByCategory.values())
        );
    }
}
