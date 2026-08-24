package com.example.householdbudget.recurringtransaction;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.category.CategoryType;
import com.example.householdbudget.transaction.Transaction;
import com.example.householdbudget.transaction.TransactionRepository;
import com.example.householdbudget.transaction.TransactionSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionGenerationServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RecurringTransactionGenerationService generationService;

    private RecurringTransaction newTemplate(int executionDay, LocalDate createdAt) {
        Category category = new Category("住居費", CategoryType.EXPENSE);
        RecurringTransaction template = new RecurringTransaction(
                "家賃", 80000, CategoryType.EXPENSE, category, executionDay, "家賃");
        setCreatedAt(template, createdAt.atStartOfDay());
        return template;
    }

    // createdAt is normally populated by the @PrePersist hook on save; set it directly here
    // since these are plain (unpersisted) instances.
    private static void setCreatedAt(RecurringTransaction template, LocalDateTime createdAt) {
        try {
            Field field = RecurringTransaction.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(template, createdAt);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void generate_createsTransaction_whenExecutionDayHasPassedAndNotYetGenerated() {
        RecurringTransaction template = newTemplate(25, LocalDate.of(2026, 8, 1));
        when(recurringTransactionRepository.findAll()).thenReturn(List.of(template));

        generationService.generate(LocalDate.of(2026, 8, 25));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 8, 25));
        assertThat(saved.getAmount()).isEqualTo(80000);
        assertThat(saved.getSource()).isEqualTo(TransactionSource.RECURRING);
        assertThat(saved.getMemo()).isEqualTo("家賃");
        assertThat(template.getLastGeneratedMonth()).isEqualTo(YearMonth.of(2026, 8));
    }

    @Test
    void generate_skips_whenExecutionDayHasNotArrivedYetThisMonth() {
        RecurringTransaction template = newTemplate(25, LocalDate.of(2026, 8, 1));
        when(recurringTransactionRepository.findAll()).thenReturn(List.of(template));

        generationService.generate(LocalDate.of(2026, 8, 24));

        verify(transactionRepository, never()).save(any());
        assertThat(template.getLastGeneratedMonth()).isNull();
    }

    @Test
    void generate_doesNotRegenerate_whenAlreadyGeneratedForCurrentMonth() {
        RecurringTransaction template = newTemplate(25, LocalDate.of(2026, 8, 1));
        template.markGenerated(YearMonth.of(2026, 8));
        when(recurringTransactionRepository.findAll()).thenReturn(List.of(template));

        generationService.generate(LocalDate.of(2026, 8, 26));

        verify(transactionRepository, never()).save(any());
    }

    // Issue #21 バグ1: 複数月アプリを起動しなかった場合、その間の未生成分をすべて遡って生成する。
    @Test
    void generate_backfillsAllMissedMonths_whenAppWasNotStartedForSeveralMonths() {
        RecurringTransaction template = newTemplate(1, LocalDate.of(2026, 4, 1));
        template.markGenerated(YearMonth.of(2026, 4));
        when(recurringTransactionRepository.findAll()).thenReturn(List.of(template));

        // 5〜7月の間アプリを起動しておらず、8/10に起動した想定
        generationService.generate(LocalDate.of(2026, 8, 10));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Transaction::getDate)
                .containsExactly(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 8, 1));
        assertThat(template.getLastGeneratedMonth()).isEqualTo(YearMonth.of(2026, 8));
    }

    // 未到来の月で打ち切られ、それ以降のさらに未来の月には手を出さない。
    @Test
    void generate_stopsAtFirstNotYetDueMonth_andDoesNotAdvanceWatermarkPastIt() {
        RecurringTransaction template = newTemplate(15, LocalDate.of(2026, 6, 1));
        template.markGenerated(YearMonth.of(2026, 6));
        when(recurringTransactionRepository.findAll()).thenReturn(List.of(template));

        // 7/15は過ぎているが8/15はまだ（基準日8/10）
        generationService.generate(LocalDate.of(2026, 8, 10));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(template.getLastGeneratedMonth()).isEqualTo(YearMonth.of(2026, 7));
    }

    @Test
    void generate_roundsExecutionDateToLastDayOfMonth_whenExecutionDayDoesNotExistInTargetMonth() {
        RecurringTransaction template = newTemplate(31, LocalDate.of(2026, 1, 1));
        template.markGenerated(YearMonth.of(2026, 1));
        when(recurringTransactionRepository.findAll()).thenReturn(List.of(template));

        // 2026年2月は28日までしかない
        generationService.generate(LocalDate.of(2026, 2, 28));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(template.getLastGeneratedMonth()).isEqualTo(YearMonth.of(2026, 2));
    }
}
