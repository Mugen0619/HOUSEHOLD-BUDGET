package com.example.householdbudget.transaction;

import java.time.LocalDate;
import java.util.Optional;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.category.CategoryRepository;
import com.example.householdbudget.category.CategoryType;
import com.example.householdbudget.common.ValidationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void create_throwsValidationException_whenCategoryTypeDoesNotMatchTransactionType() {
        Category incomeCategory = new Category("給与", CategoryType.INCOME);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(incomeCategory));

        TransactionRequest request = new TransactionRequest(
                LocalDate.of(2026, 8, 1), 1000, CategoryType.EXPENSE, 1L, null);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("カテゴリの種別が一致しません");

        verifyNoInteractions(transactionRepository);
    }
}
