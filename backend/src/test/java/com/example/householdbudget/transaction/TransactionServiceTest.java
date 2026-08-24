package com.example.householdbudget.transaction;

import java.time.LocalDate;

import com.example.householdbudget.category.CategoryService;
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
    private CategoryService categoryService;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void create_propagatesValidationException_whenCategoryTypeDoesNotMatchTransactionType() {
        when(categoryService.getForType(1L, CategoryType.EXPENSE))
                .thenThrow(new ValidationException("categoryId", "カテゴリの種別が一致しません"));

        TransactionRequest request = new TransactionRequest(
                LocalDate.of(2026, 8, 1), 1000, CategoryType.EXPENSE, 1L, null);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("カテゴリの種別が一致しません");

        verifyNoInteractions(transactionRepository);
    }
}
