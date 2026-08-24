package com.example.householdbudget.recurringtransaction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.category.CategoryRepository;
import com.example.householdbudget.category.CategoryType;
import com.example.householdbudget.transaction.Transaction;
import com.example.householdbudget.transaction.TransactionRepository;
import com.example.householdbudget.transaction.TransactionSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RecurringTransactionGenerationServiceIT {

    @Autowired
    private RecurringTransactionGenerationService generationService;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    // The test Spring context (and its in-memory H2 database) is cached and
    // shared across all IT test classes for the whole test run, so each test
    // must clear shared tables before seeding its own data.
    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        recurringTransactionRepository.deleteAll();
        categoryRepository.deleteAll();
        category = categoryRepository.save(new Category("住居費", CategoryType.EXPENSE));
    }

    @Test
    void generate_createsTransactionLinkedToTemplate_andDoesNotDuplicateOnSecondRun() {
        RecurringTransaction template = recurringTransactionRepository.save(
                new RecurringTransaction("家賃", 80000, CategoryType.EXPENSE, category, 25, "家賃"));

        generationService.generate(LocalDate.of(2026, 8, 25));

        List<Transaction> generated = transactionRepository.findByDateBetween(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        assertThat(generated).hasSize(1);
        Transaction transaction = generated.get(0);
        assertThat(transaction.getSource()).isEqualTo(TransactionSource.RECURRING);
        assertThat(transaction.getRecurringTransactionId()).isEqualTo(template.getId());
        assertThat(transaction.getDate()).isEqualTo(LocalDate.of(2026, 8, 25));

        // アプリ再起動やスケジュール実行の重複を想定して同日にもう一度実行しても重複生成しない
        generationService.generate(LocalDate.of(2026, 8, 26));

        List<Transaction> afterSecondRun = transactionRepository.findByDateBetween(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        assertThat(afterSecondRun).hasSize(1);
    }

    @Test
    void generate_keepsGeneratedTransaction_afterTemplateIsDeleted() {
        RecurringTransaction template = recurringTransactionRepository.save(
                new RecurringTransaction("家賃", 80000, CategoryType.EXPENSE, category, 25, "家賃"));
        generationService.generate(LocalDate.of(2026, 8, 25));
        Long transactionId = transactionRepository.findByDateBetween(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)).get(0).getId();

        recurringTransactionRepository.deleteById(template.getId());

        Transaction remaining = transactionRepository.findById(transactionId).orElseThrow();
        assertThat(remaining.getSource()).isEqualTo(TransactionSource.RECURRING);
        assertThat(remaining.getRecurringTransactionId()).isNull();
    }

    // Issue #21 バグ1の再発防止：数ヶ月アプリを起動しなかった場合でも、次に起動したときに
    // 未生成分をすべて遡って生成する（data-design.md 5.2）。
    @Test
    void generate_backfillsAllMissedMonths_forAnExistingTemplate() {
        RecurringTransaction template = recurringTransactionRepository.save(
                new RecurringTransaction("家賃", 80000, CategoryType.EXPENSE, category, 1, "家賃"));
        // 4月分までは生成済みで、5〜7月の間はアプリを起動していなかった状態を再現
        template.markGenerated(YearMonth.of(2026, 4));
        recurringTransactionRepository.save(template);

        generationService.generate(LocalDate.of(2026, 8, 10));

        List<Transaction> generated = transactionRepository.findByDateBetween(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 8, 31));
        assertThat(generated)
                .extracting(Transaction::getDate)
                .containsExactlyInAnyOrder(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 8, 1));
    }

    // Issue #21 バグ2の再発防止：自動生成された収支記録の日付を編集で月またぎに変更しても、
    // 「生成済みか」の判定（last_generated_month）は影響を受けず、二重生成されない。
    @Test
    void generate_doesNotDuplicate_whenGeneratedTransactionDateWasEditedAcrossMonthBoundary() {
        RecurringTransaction template = recurringTransactionRepository.save(
                new RecurringTransaction("家賃", 80000, CategoryType.EXPENSE, category, 25, "家賃"));
        generationService.generate(LocalDate.of(2026, 8, 25));

        Transaction generated = transactionRepository.findByDateBetween(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)).get(0);
        generated.update(LocalDate.of(2026, 9, 1), generated.getAmount(), generated.getType(),
                category, generated.getMemo());
        transactionRepository.save(generated);

        generationService.generate(LocalDate.of(2026, 8, 30));

        List<Transaction> allForTemplate = transactionRepository.findAll().stream()
                .filter(t -> template.getId().equals(t.getRecurringTransactionId()))
                .toList();
        assertThat(allForTemplate).hasSize(1);
    }
}
