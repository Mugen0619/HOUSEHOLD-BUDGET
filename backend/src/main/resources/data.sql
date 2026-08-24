INSERT INTO categories (name, type) VALUES
    ('給与', 'INCOME'),
    ('ボーナス', 'INCOME'),
    ('食費', 'EXPENSE'),
    ('交通費', 'EXPENSE'),
    ('日用品', 'EXPENSE'),
    ('住居費', 'EXPENSE'),
    ('娯楽費', 'EXPENSE')
ON CONFLICT (name, type) DO NOTHING;

-- 定期支出テンプレートのサンプル（アプリ起動時のキャッチアップ生成により、当月分の収支記録が自動生成される）
WITH sample_recurring (name, amount, type, category_name, execution_day, memo) AS (
    VALUES
        ('家賃', 80000, 'EXPENSE', '住居費', 1, '家賃'),
        ('Netflix', 1980, 'EXPENSE', '娯楽費', 1, 'サブスクリプション')
)
INSERT INTO recurring_transactions (name, amount, type, category_id, execution_day, memo)
SELECT sr.name, sr.amount, sr.type, c.id, sr.execution_day, sr.memo
FROM sample_recurring sr
JOIN categories c ON c.name = sr.category_name AND c.type = sr.type
WHERE NOT EXISTS (SELECT 1 FROM recurring_transactions);

-- 収支記録のサンプル（当月・先月分）。日付はCURRENT_DATE基準の相対指定なので、いつ起動しても「当月データ」として表示される
WITH sample_transactions (month_offset, day_offset, amount, type, category_name, memo) AS (
    VALUES
        -- 当月
        (0, 0, 300000, 'INCOME', '給与', '今月の給与'),
        (0, 20, 100000, 'INCOME', 'ボーナス', '夏季賞与'),
        (0, 1, 850, 'EXPENSE', '食費', 'コンビニでランチ'),
        (0, 3, 3200, 'EXPENSE', '食費', 'スーパーで食料品購入'),
        (0, 6, 1100, 'EXPENSE', '食費', 'カフェ'),
        (0, 9, 4500, 'EXPENSE', '食費', '外食（家族で）'),
        (0, 13, 980, 'EXPENSE', '食費', 'スーパーで食料品購入'),
        (0, 17, 620, 'EXPENSE', '食費', 'コンビニ'),
        (0, 21, 2800, 'EXPENSE', '食費', 'スーパーで食料品購入'),
        (0, 2, 220, 'EXPENSE', '交通費', '電車'),
        (0, 10, 500, 'EXPENSE', '交通費', 'バス'),
        (0, 18, 3000, 'EXPENSE', '交通費', '定期券チャージ'),
        (0, 5, 1200, 'EXPENSE', '日用品', '洗剤・ティッシュ'),
        (0, 15, 2400, 'EXPENSE', '日用品', 'ドラッグストア'),
        (0, 8, 1500, 'EXPENSE', '娯楽費', '映画'),
        (0, 22, 3000, 'EXPENSE', '娯楽費', 'ゲーム購入'),
        -- 先月
        (-1, 0, 300000, 'INCOME', '給与', '先月の給与'),
        (-1, 4, 2200, 'EXPENSE', '食費', 'スーパーで食料品購入'),
        (-1, 9, 800, 'EXPENSE', '交通費', '電車'),
        (-1, 14, 1600, 'EXPENSE', '日用品', 'ドラッグストア')
)
INSERT INTO transactions (date, amount, type, category_id, memo, source)
SELECT (date_trunc('month', CURRENT_DATE) + (st.month_offset || ' month')::interval)::date + st.day_offset,
       st.amount,
       st.type,
       c.id,
       st.memo,
       'MANUAL'
FROM sample_transactions st
JOIN categories c ON c.name = st.category_name AND c.type = st.type
WHERE NOT EXISTS (SELECT 1 FROM transactions);
