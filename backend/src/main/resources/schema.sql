CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT uq_categories_name_type UNIQUE (name, type)
);

CREATE TABLE IF NOT EXISTS recurring_transactions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    amount INTEGER NOT NULL CHECK (amount > 0),
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    execution_day INTEGER NOT NULL CHECK (execution_day BETWEEN 1 AND 31),
    memo VARCHAR(500),
    -- 自動生成バッチが「どの年月分まで生成済みか」を記録する内部用の目印（月初日で保持）。
    -- 生成済み収支記録の date は編集で自由に変更できるため、date の現在値では二重生成/欠落を
    -- 正しく判定できない（Issue #21）。NULLは未生成を表す。
    last_generated_month DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    amount INTEGER NOT NULL CHECK (amount > 0),
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    memo VARCHAR(500),
    source VARCHAR(10) NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'RECURRING')),
    recurring_transaction_id BIGINT REFERENCES recurring_transactions(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
