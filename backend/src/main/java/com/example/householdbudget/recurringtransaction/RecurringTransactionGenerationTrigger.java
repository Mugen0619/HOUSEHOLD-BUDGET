package com.example.householdbudget.recurringtransaction;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * {@link RecurringTransactionGenerationService#generate} の実行タイミングを担当する
 * （アプリ起動時のキャッチアップ・毎日の定時実行、data-design.md 5.1・5.2）。
 * 生成処理を別Beanに切り出しているのは、同一クラス内から self-invocation で呼び出すと
 * {@code @Transactional} がSpringのプロキシを経由せず効かなくなるのを避けるため。
 */
@Component
public class RecurringTransactionGenerationTrigger {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionGenerationTrigger.class);

    private final RecurringTransactionGenerationService generationService;

    public RecurringTransactionGenerationTrigger(RecurringTransactionGenerationService generationService) {
        this.generationService = generationService;
    }

    // 起動時のキャッチアップ生成が失敗しても、アプリの起動自体は継続させる（Issue #21）。
    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartup() {
        try {
            generationService.generate(LocalDate.now());
        } catch (RuntimeException e) {
            log.error("起動時の定期支出キャッチアップ生成に失敗しました", e);
        }
    }

    @Scheduled(cron = "0 10 0 * * *")
    public void generateDaily() {
        generationService.generate(LocalDate.now());
    }
}
