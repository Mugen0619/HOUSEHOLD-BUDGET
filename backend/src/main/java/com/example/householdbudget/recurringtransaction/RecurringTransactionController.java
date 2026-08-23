package com.example.householdbudget.recurringtransaction;

import com.example.householdbudget.category.CategoryType;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recurring-transactions")
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionController(RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    @GetMapping
    public RecurringTransactionListResponse list(@RequestParam(required = false) CategoryType type) {
        return new RecurringTransactionListResponse(recurringTransactionService.list(type));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringTransactionResponse create(@Valid @RequestBody RecurringTransactionRequest request) {
        return recurringTransactionService.create(request);
    }

    @PutMapping("/{id}")
    public RecurringTransactionResponse update(@PathVariable Long id,
                                                @Valid @RequestBody RecurringTransactionRequest request) {
        return recurringTransactionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        recurringTransactionService.delete(id);
    }
}
