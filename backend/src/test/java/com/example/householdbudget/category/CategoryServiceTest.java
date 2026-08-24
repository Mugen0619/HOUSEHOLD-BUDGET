package com.example.householdbudget.category;

import java.util.Optional;

import com.example.householdbudget.common.CategoryInUseException;
import com.example.householdbudget.common.ResourceNotFoundException;
import com.example.householdbudget.common.ValidationException;
import com.example.householdbudget.recurringtransaction.RecurringTransactionRepository;
import com.example.householdbudget.transaction.TransactionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void delete_throwsCategoryInUseException_whenCategoryHasTransactions() {
        Category category = new Category("食費", CategoryType.EXPENSE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(CategoryInUseException.class);

        verify(categoryRepository, never()).delete(category);
    }

    @Test
    void delete_throwsCategoryInUseException_whenCategoryUsedByRecurringTransaction() {
        Category category = new Category("住居費", CategoryType.EXPENSE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(transactionRepository.existsByCategoryId(1L)).thenReturn(false);
        when(recurringTransactionRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(CategoryInUseException.class);

        verify(categoryRepository, never()).delete(category);
    }

    @Test
    void getForType_returnsCategory_whenTypeMatches() {
        Category category = new Category("食費", CategoryType.EXPENSE);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThat(categoryService.getForType(1L, CategoryType.EXPENSE)).isSameAs(category);
    }

    @Test
    void getForType_throwsValidationException_whenTypeDoesNotMatch() {
        Category incomeCategory = new Category("給与", CategoryType.INCOME);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(incomeCategory));

        assertThatThrownBy(() -> categoryService.getForType(1L, CategoryType.EXPENSE))
                .isInstanceOf(ValidationException.class)
                .hasMessage("カテゴリの種別が一致しません");
    }

    @Test
    void getForType_throwsResourceNotFoundException_whenCategoryDoesNotExist() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getForType(99L, CategoryType.EXPENSE))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
