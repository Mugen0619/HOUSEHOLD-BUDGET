package com.example.householdbudget.category;

import java.util.List;

import com.example.householdbudget.common.CategoryInUseException;
import com.example.householdbudget.common.ResourceNotFoundException;
import com.example.householdbudget.common.ValidationException;
import com.example.householdbudget.transaction.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(CategoryType type) {
        List<Category> categories = type != null ? categoryRepository.findByType(type) : categoryRepository.findAll();
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameAndType(request.name(), request.type())) {
            throw new ValidationException("name", "このカテゴリ名は既に使用されています");
        }
        Category saved = categoryRepository.save(new Category(request.name(), request.type()));
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("カテゴリが見つかりません: id=" + id));
        if (categoryRepository.existsByNameAndTypeAndIdNot(request.name(), category.getType(), id)) {
            throw new ValidationException("name", "このカテゴリ名は既に使用されています");
        }
        category.setName(request.name());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("カテゴリが見つかりません: id=" + id));
        if (transactionRepository.existsByCategoryId(id)) {
            throw new CategoryInUseException("このカテゴリは使用中のため削除できません");
        }
        categoryRepository.delete(category);
    }
}
