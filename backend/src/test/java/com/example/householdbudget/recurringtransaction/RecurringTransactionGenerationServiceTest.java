package com.example.householdbudget.recurringtransaction;

import java.time.LocalDate;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

    @Test
    void generate_createsTransaction_whenExecutionDayHasPassedAndNotYetGenerated() {
        Category category = new Category("住居費", CategoryType.EXPENSE);
        RecurringTransaction template = new RecurringTransaction(
                "家賃", 80000, CategoryType.EXPENSE, category, 25, "家賃");
        when(recurringTransactionRepository.findAll()).thenReturn(List.of(template));
        when(transactionRepository.existsByRecurringTransactionAndDateBetween(any(), any(), any()))
                .thenReturn(false);

        generationService.generate(LocalDate.of(2026, 8, 25));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 8, 25));
        assertThat(saved.getAmount()).isEqualTo(80000);
        assertThat(saved.getSource()).isEqualTo(TransactionSource.RECURRING);
        assertThat(saved.getMemo()).isEqualTo("家賃");
    }

    @Test
    void generate_skips_whenAlreadyGeneratedForCurrentMonth() {
        Category category = new Category("住居費", CategoryType.EXPENSE);
        RecurringTransaction template = new RecurringTransaction(
                "家賃", 80000, CategoryType.EXPENSE, category, 25, "家賃");
        when(recurringTransactionRepository.findAll()).thenReturn(List.of(template));
        when(transactionRepository.existsByRecurringTransactionAndDateBetween(any(), any(), any()))
                .thenReturn(true);

        generationService.generate(LocalDate.of(2026, 8, 26));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void generate_skips_whenExecutionDayHasNotArrivedYet() {
        Category category = new Category("住居費", CategoryType.EXPENSE);
        RecurringTransaction template = new RecurringTransaction(
                "家賃", 80000, CategoryType.EXPENSE, category, 25, "家賃");
        when(recurringTransactionRepository.findAll()).thenReturn(List.of(template));

        generationService.generate(LocalDate.of(2026, 8, 24));

        verify(transactionRepository, never())
                .existsByRecurringTransactionAndDateBetween(any(), any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void generate_roundsExecutionDateToLastDayOfMonth_whenExecutionDayDoesNotExistInTargetMonth() {
        Category category = new Category("娯楽費", CategoryType.EXPENSE);
        RecurringTransaction template = new RecurringTransaction(
                "サブスク", 1000, CategoryType.EXPENSE, category, 31, null);
        when(recurringTransactionRepository.findAll()).thenReturn(List.of(template));
        when(transactionRepository.existsByRecurringTransactionAndDateBetween(any(), any(), any()))
                .thenReturn(false);

        // 2026年2月は28日までしかない
        generationService.generate(LocalDate.of(2026, 2, 28));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        verify(transactionRepository).existsByRecurringTransactionAndDateBetween(
                eq(template), eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28)));
    }
}
