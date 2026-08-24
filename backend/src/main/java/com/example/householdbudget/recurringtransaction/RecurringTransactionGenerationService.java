package com.example.householdbudget.recurringtransaction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.transaction.Transaction;
import com.example.householdbudget.transaction.TransactionRepository;
import com.example.householdbudget.transaction.TransactionSource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 定期支出テンプレートから未生成分の収支記録を自動生成するバッチ処理（data-design.md 5章）。
 * トリガー（起動時キャッチアップ・毎日の定時実行）は {@link RecurringTransactionGenerationTrigger}
 * が担当する。呼び出しはそちら経由の外部呼び出しのみを想定しており、同一クラス内からの
 * self-invocationで呼ぶと {@code @Transactional} がSpringのプロキシを経由せず効かなくなる点に注意。
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

    @Transactional
    public void generate(LocalDate referenceDate) {
        YearMonth currentMonth = YearMonth.from(referenceDate);
        List<RecurringTransaction> templates = recurringTransactionRepository.findAll();
        for (RecurringTransaction template : templates) {
            generateForTemplate(template, referenceDate, currentMonth);
        }
    }

    // テンプレートの前回生成月（未生成なら作成月）から当月まで、未生成分をすべて遡って生成する
    // （アプリを複数月起動しなかった場合でも取りこぼさない。data-design.md 5.2、Issue #21）。
    // 「生成済みか」の判定は last_generated_month の目印のみで行い、生成済み収支記録の date が
    // 編集で変更されていても影響を受けない（Issue #21の二重生成バグ対策）。
    private void generateForTemplate(RecurringTransaction template, LocalDate referenceDate, YearMonth currentMonth) {
        YearMonth lastGenerated = template.getLastGeneratedMonth();
        YearMonth targetMonth = lastGenerated != null ? lastGenerated.plusMonths(1) : YearMonth.from(template.getCreatedAt());

        while (!targetMonth.isAfter(currentMonth)) {
            LocalDate executionDate = resolveExecutionDate(targetMonth, template.getExecutionDay());
            if (referenceDate.isBefore(executionDate)) {
                break; // この月の実行日がまだ来ていない＝それより後の月も同様のため打ち切る
            }

            Category category = template.getCategory();
            Transaction transaction = new Transaction(
                    executionDate, template.getAmount(), template.getType(), category, template.getMemo(),
                    TransactionSource.RECURRING, template);
            transactionRepository.save(transaction);
            template.markGenerated(targetMonth);

            targetMonth = targetMonth.plusMonths(1);
        }
    }

    // 実行日が対象月に存在しない場合（例：31日指定で2月）はその月の最終日に丸める（data-design.md 5.3）。
    private LocalDate resolveExecutionDate(YearMonth targetMonth, int executionDay) {
        int day = Math.min(executionDay, targetMonth.lengthOfMonth());
        return targetMonth.atDay(day);
    }
}
