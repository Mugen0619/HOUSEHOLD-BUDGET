package com.example.householdbudget.summary;

import java.time.LocalDate;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.category.CategoryRepository;
import com.example.householdbudget.category.CategoryType;
import com.example.householdbudget.recurringtransaction.RecurringTransactionRepository;
import com.example.householdbudget.transaction.Transaction;
import com.example.householdbudget.transaction.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SummaryControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    private Long salaryCategoryId;
    private Long foodCategoryId;

    // The test Spring context (and its in-memory H2 database) is cached and
    // shared across all IT test classes for the whole test run, so each test
    // must clear shared tables before seeding its own data to avoid colliding
    // with the categories.(name, type) unique constraint.
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        recurringTransactionRepository.deleteAll();
        categoryRepository.deleteAll();
        Category salary = categoryRepository.save(new Category("給与", CategoryType.INCOME));
        Category food = categoryRepository.save(new Category("食費", CategoryType.EXPENSE));
        salaryCategoryId = salary.getId();
        foodCategoryId = food.getId();

        transactionRepository.save(new Transaction(
                LocalDate.of(2026, 8, 10), 300000, CategoryType.INCOME, salary, "給与振込"));
        transactionRepository.save(new Transaction(
                LocalDate.of(2026, 8, 15), 25000, CategoryType.EXPENSE, food, "スーパー"));
        transactionRepository.save(new Transaction(
                LocalDate.of(2026, 8, 20), 15000, CategoryType.EXPENSE, food, "外食"));
        // Outside the requested month: must not affect the summary.
        transactionRepository.save(new Transaction(
                LocalDate.of(2026, 7, 20), 99999, CategoryType.EXPENSE, food, "先月分"));
    }

    @Test
    void getSummary_returnsAggregatedTotalsForMonth() {
        ResponseEntity<SummaryResponse> response =
                restTemplate.getForEntity("/api/summary?month=2026-08", SummaryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SummaryResponse body = response.getBody();
        assertThat(body.month()).isEqualTo("2026-08");
        assertThat(body.incomeTotal()).isEqualTo(300000);
        assertThat(body.expenseTotal()).isEqualTo(40000);
        assertThat(body.balance()).isEqualTo(260000);

        assertThat(body.incomeByCategory())
                .containsExactly(new CategoryAmount(salaryCategoryId, "給与", 300000));
        assertThat(body.expenseByCategory())
                .containsExactly(new CategoryAmount(foodCategoryId, "食費", 40000));
    }
}
