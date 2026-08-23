package com.example.householdbudget.recurringtransaction;

import java.util.List;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.category.CategoryRepository;
import com.example.householdbudget.category.CategoryType;
import com.example.householdbudget.common.ResourceNotFoundException;
import com.example.householdbudget.common.ValidationException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final CategoryRepository categoryRepository;

    public RecurringTransactionService(RecurringTransactionRepository recurringTransactionRepository,
                                        CategoryRepository categoryRepository) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> list(CategoryType type) {
        List<RecurringTransaction> items = type != null
                ? recurringTransactionRepository.findByType(type)
                : recurringTransactionRepository.findAll();
        return items.stream().map(RecurringTransactionResponse::from).toList();
    }

    @Transactional
    public RecurringTransactionResponse create(RecurringTransactionRequest request) {
        Category category = findCategory(request.categoryId());
        validateCategoryType(category, request.type());

        RecurringTransaction recurringTransaction = new RecurringTransaction(
                request.name(), request.amount(), request.type(), category, request.executionDay(), request.memo());
        return RecurringTransactionResponse.from(recurringTransactionRepository.save(recurringTransaction));
    }

    @Transactional
    public RecurringTransactionResponse update(Long id, RecurringTransactionRequest request) {
        RecurringTransaction recurringTransaction = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("定期支出テンプレートが見つかりません: id=" + id));
        Category category = findCategory(request.categoryId());
        validateCategoryType(category, request.type());

        recurringTransaction.update(
                request.name(), request.amount(), request.type(), category, request.executionDay(), request.memo());
        return RecurringTransactionResponse.from(recurringTransaction);
    }

    @Transactional
    public void delete(Long id) {
        if (!recurringTransactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("定期支出テンプレートが見つかりません: id=" + id);
        }
        recurringTransactionRepository.deleteById(id);
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("カテゴリが見つかりません: id=" + categoryId));
    }

    private void validateCategoryType(Category category, CategoryType requestedType) {
        if (category.getType() != requestedType) {
            throw new ValidationException("categoryId", "カテゴリの種別が一致しません");
        }
    }
}
