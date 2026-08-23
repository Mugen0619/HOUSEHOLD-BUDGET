package com.example.householdbudget.recurringtransaction;

import java.util.List;

import com.example.householdbudget.category.CategoryType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByType(CategoryType type);

    boolean existsByCategoryId(Long categoryId);
}
