package com.example.householdbudget.summary;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.category.CategoryType;
import com.example.householdbudget.transaction.Transaction;
import com.example.householdbudget.transaction.TransactionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SummaryService summaryService;

    @Test
    void getSummary_computesTotalsAndGroupsByCategory() {
        Category salary = category(1L, "給与", CategoryType.INCOME);
        Category food = category(2L, "食費", CategoryType.EXPENSE);
        Category transport = category(3L, "交通費", CategoryType.EXPENSE);

        YearMonth month = YearMonth.of(2026, 8);
        List<Transaction> transactions = List.of(
                new Transaction(LocalDate.of(2026, 8, 1), 300000, CategoryType.INCOME, salary, null),
                new Transaction(LocalDate.of(2026, 8, 5), 25000, CategoryType.EXPENSE, food, "スーパー"),
                new Transaction(LocalDate.of(2026, 8, 15), 15000, CategoryType.EXPENSE, food, "外食"),
                new Transaction(LocalDate.of(2026, 8, 20), 15000, CategoryType.EXPENSE, transport, null));

        when(transactionRepository.findByDateBetween(month.atDay(1), month.atEndOfMonth()))
                .thenReturn(transactions);

        SummaryResponse response = summaryService.getSummary(month);

        assertThat(response.month()).isEqualTo("2026-08");
        assertThat(response.incomeTotal()).isEqualTo(300000);
        assertThat(response.expenseTotal()).isEqualTo(55000);
        assertThat(response.balance()).isEqualTo(245000);

        assertThat(response.incomeByCategory())
                .containsExactly(new CategoryAmount(1L, "給与", 300000));

        assertThat(response.expenseByCategory())
                .containsExactlyInAnyOrder(
                        new CategoryAmount(2L, "食費", 40000),
                        new CategoryAmount(3L, "交通費", 15000));
    }

    @Test
    void getSummary_returnsZeros_whenNoTransactionsInMonth() {
        YearMonth month = YearMonth.of(2026, 9);
        when(transactionRepository.findByDateBetween(month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of());

        SummaryResponse response = summaryService.getSummary(month);

        assertThat(response.month()).isEqualTo("2026-09");
        assertThat(response.incomeTotal()).isZero();
        assertThat(response.expenseTotal()).isZero();
        assertThat(response.balance()).isZero();
        assertThat(response.incomeByCategory()).isEmpty();
        assertThat(response.expenseByCategory()).isEmpty();
    }

    private Category category(Long id, String name, CategoryType type) {
        Category category = new Category(name, type);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
