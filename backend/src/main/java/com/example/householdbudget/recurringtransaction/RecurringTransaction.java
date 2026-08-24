package com.example.householdbudget.recurringtransaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import com.example.householdbudget.category.Category;
import com.example.householdbudget.category.CategoryType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "recurring_transactions")
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "execution_day", nullable = false)
    private Integer executionDay;

    @Column(length = 500)
    private String memo;

    // 自動生成バッチの「どの年月分まで生成済みか」を示す目印（月初日で保持、NULL=未生成）。
    // transactions.date は編集で自由に変更できるため、日付の現在値では二重生成/欠落を正しく
    // 判定できない（Issue #21）。この目印はバッチ処理のみが更新する内部状態。
    @Column(name = "last_generated_month")
    private LocalDate lastGeneratedMonth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RecurringTransaction() {
    }

    public RecurringTransaction(String name, Integer amount, CategoryType type, Category category,
                                 Integer executionDay, String memo) {
        this.name = name;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.executionDay = executionDay;
        this.memo = memo;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String name, Integer amount, CategoryType type, Category category,
                        Integer executionDay, String memo) {
        this.name = name;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.executionDay = executionDay;
        this.memo = memo;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAmount() {
        return amount;
    }

    public CategoryType getType() {
        return type;
    }

    public Category getCategory() {
        return category;
    }

    public Integer getExecutionDay() {
        return executionDay;
    }

    public String getMemo() {
        return memo;
    }

    public YearMonth getLastGeneratedMonth() {
        return lastGeneratedMonth != null ? YearMonth.from(lastGeneratedMonth) : null;
    }

    public void markGenerated(YearMonth month) {
        this.lastGeneratedMonth = month.atDay(1);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
