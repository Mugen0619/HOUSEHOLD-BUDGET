INSERT INTO categories (name, type) VALUES
    ('給与', 'INCOME'),
    ('ボーナス', 'INCOME'),
    ('食費', 'EXPENSE'),
    ('交通費', 'EXPENSE'),
    ('日用品', 'EXPENSE')
ON CONFLICT (name, type) DO NOTHING;
