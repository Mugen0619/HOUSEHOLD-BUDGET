package com.example.householdbudget.recurringtransaction;

import java.util.Optional;

import com.example.householdbudget.category.CategoryService;
import com.example.householdbudget.category.CategoryType;
import com.example.householdbudget.common.ResourceNotFoundException;
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
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    @Test
    void create_propagatesValidationException_whenCategoryTypeDoesNotMatchTemplateType() {
        when(categoryService.getForType(1L, CategoryType.EXPENSE))
                .thenThrow(new ValidationException("categoryId", "カテゴリの種別が一致しません"));

        RecurringTransactionRequest request = new RecurringTransactionRequest(
                "家賃", 80000, CategoryType.EXPENSE, 1L, 25, "家賃");

        assertThatThrownBy(() -> recurringTransactionService.create(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("カテゴリの種別が一致しません");

        verifyNoInteractions(recurringTransactionRepository);
    }

    @Test
    void update_throwsResourceNotFoundException_whenTemplateDoesNotExist() {
        when(recurringTransactionRepository.findById(99L)).thenReturn(Optional.empty());

        RecurringTransactionRequest request = new RecurringTransactionRequest(
                "家賃", 80000, CategoryType.EXPENSE, 1L, 25, "家賃");

        assertThatThrownBy(() -> recurringTransactionService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(categoryService);
    }

    @Test
    void delete_throwsResourceNotFoundException_whenTemplateDoesNotExist() {
        when(recurringTransactionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> recurringTransactionService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
