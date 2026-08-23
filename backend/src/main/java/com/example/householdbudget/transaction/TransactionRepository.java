package com.example.householdbudget.transaction;

import java.time.LocalDate;
import java.util.List;

import com.example.householdbudget.recurringtransaction.RecurringTransaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    boolean existsByCategoryId(Long categoryId);

    List<Transaction> findByDateBetween(LocalDate from, LocalDate to);

    boolean existsByRecurringTransactionAndDateBetween(RecurringTransaction recurringTransaction, LocalDate from, LocalDate to);
}
