package com.example.householdbudget.transaction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.category.CategoryService;
import com.example.householdbudget.category.CategoryType;
import com.example.householdbudget.common.ResourceNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository, CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(LocalDate from, LocalDate to, CategoryType type, Long categoryId,
                                           String sort, String order) {
        LocalDate effectiveFrom = from != null ? from : YearMonth.now().atDay(1);
        LocalDate effectiveTo = to != null ? to : YearMonth.now().atEndOfMonth();

        Specification<Transaction> spec = Specification
                .where(TransactionSpecifications.dateFrom(effectiveFrom))
                .and(TransactionSpecifications.dateTo(effectiveTo))
                .and(TransactionSpecifications.hasType(type))
                .and(TransactionSpecifications.hasCategoryId(categoryId));

        String sortField = "amount".equalsIgnoreCase(sort) ? "amount" : "date";
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return transactionRepository.findAll(spec, Sort.by(direction, sortField)).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Category category = categoryService.getForType(request.categoryId(), request.type());

        Transaction transaction = new Transaction(request.date(), request.amount(), request.type(), category, request.memo());
        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("収支記録が見つかりません: id=" + id));
        Category category = categoryService.getForType(request.categoryId(), request.type());

        transaction.update(request.date(), request.amount(), request.type(), category, request.memo());
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("収支記録が見つかりません: id=" + id);
        }
        transactionRepository.deleteById(id);
    }
}
