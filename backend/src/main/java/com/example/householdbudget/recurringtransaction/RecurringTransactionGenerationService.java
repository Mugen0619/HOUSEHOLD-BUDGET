package com.example.householdbudget.recurringtransaction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.transaction.Transaction;
import com.example.householdbudget.transaction.TransactionRepository;
import com.example.householdbudget.transaction.TransactionSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定期支出テンプレートから当月分の収支記録を自動生成するバッチ処理（data-design.md 5章）。
 * このアプリは常時起動を前提としない（アプリ起動時に手動で立ち上げるローカル運用）ため、
 * 毎日の定時実行に加えてアプリ起動時にもキャッチアップとして同じ処理を実行する（5.2参照）。
 */
@Service
public class RecurringTransactionGenerationService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionRepository transactionRepository;

    public RecurringTransactionGenerationService(RecurringTransactionRepository recurringTransactionRepository,
                                                   TransactionRepository transactionRepository) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.transactionRepository = transactionRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartup() {
        generate(LocalDate.now());
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void generateDaily() {
        generate(LocalDate.now());
    }

    @Transactional
    public void generate(LocalDate referenceDate) {
        YearMonth targetMonth = YearMonth.from(referenceDate);
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        List<RecurringTransaction> templates = recurringTransactionRepository.findAll();
        for (RecurringTransaction template : templates) {
            LocalDate executionDate = resolveExecutionDate(targetMonth, template.getExecutionDay());
            if (referenceDate.isBefore(executionDate)) {
                continue;
            }
            boolean alreadyGenerated = transactionRepository.existsByRecurringTransactionAndDateBetween(
                    template, monthStart, monthEnd);
            if (alreadyGenerated) {
                continue;
            }

            Category category = template.getCategory();
            Transaction transaction = new Transaction(
                    executionDate, template.getAmount(), template.getType(), category, template.getMemo(),
                    TransactionSource.RECURRING, template);
            transactionRepository.save(transaction);
        }
    }

    // 実行日が対象月に存在しない場合（例：31日指定で2月）はその月の最終日に丸める（data-design.md 5.3）。
    private LocalDate resolveExecutionDate(YearMonth targetMonth, int executionDay) {
        int day = Math.min(executionDay, targetMonth.lengthOfMonth());
        return targetMonth.atDay(day);
    }
}
