# データ設計：家計簿（収支管理）アプリ

要件定義書（[docs/requirements.md](./requirements.md)）4章・5章をもとに、DBスキーマとAPI設計を整理する。技術構成は[tech-stack.md](./tech-stack.md)を参照。

## 1. DBスキーマ

### 1.1 categories（カテゴリ）

| カラム | 型 | 制約 | 内容 |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | 自動採番 |
| name | VARCHAR(20) | NOT NULL | カテゴリ名（1〜20文字） |
| type | VARCHAR(10) | NOT NULL, CHECK (type IN ('INCOME', 'EXPENSE')) | 収入用 / 支出用 |

- 制約：`UNIQUE (name, type)` — 同一種別内でのカテゴリ名重複を禁止（要件定義書4.2）
- 削除：紐づく収支記録が存在する場合はアプリ側（Service層）でエラーとし、削除させない（要件定義書5章 No.6）。DB側にも `ON DELETE RESTRICT`（後述の外部キー）を設定し、二重の安全策とする
- カテゴリ作成後の `type` 変更は不可とする（既存の収支記録との整合性が崩れるため）。種別を変えたい場合は削除して新規作成する運用とする

### 1.2 transactions（収支記録）

| カラム | 型 | 制約 | 内容 |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | 自動採番 |
| date | DATE | NOT NULL | 取引が発生した日 |
| amount | INTEGER | NOT NULL, CHECK (amount > 0) | 金額（円単位の正の整数） |
| type | VARCHAR(10) | NOT NULL, CHECK (type IN ('INCOME', 'EXPENSE')) | 収入 / 支出 |
| category_id | BIGINT | NOT NULL, REFERENCES categories(id) ON DELETE RESTRICT | 紐づくカテゴリ |
| memo | VARCHAR(500) | NULL可 | 任意入力 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | 作成日時（自動記録） |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() | 更新日時（自動記録、更新時にアプリ側で上書き） |

- `type` と `category_id` が指す `categories.type` は一致していなければならない（例：支出の記録に収入用カテゴリを紐づけることはできない）。DBのCHECK制約では表現しづらいため、Service層でのバリデーションとして実装する
- 物理削除とする（要件定義書11章）

### 1.3 ER図（概略）

```
categories (1) ──< (多) transactions
  id                     category_id (FK)
  name
  type                    type（categoriesと一致させる）
```

## 2. マイグレーション方針

- Spring Data JPAの `ddl-auto` に頼らず、Flyway等のマイグレーションツールは今回は導入しない（学習目的・個人利用のため、`schema.sql` を用意しアプリ起動時に反映する簡易運用とする）
- 初期データ（カテゴリのプリセット）は `data.sql` で投入するか検討する（要件定義書11章の保留事項）→ 実装時に「食費・交通費・給与」等の代表的なカテゴリを数件だけ初期投入し、以降はユーザーが自由に追加・編集できるようにする方針とする

## 3. API設計

すべて `Content-Type: application/json`。ベースパスは `/api`。

### 3.1 収支記録（Transactions）

| メソッド | パス | 内容 |
|---|---|---|
| GET | `/api/transactions` | 一覧取得（クエリパラメータで絞り込み・並び替え） |
| POST | `/api/transactions` | 新規作成 |
| PUT | `/api/transactions/{id}` | 更新 |
| DELETE | `/api/transactions/{id}` | 削除 |

#### GET /api/transactions のクエリパラメータ

| パラメータ | 例 | 内容 |
|---|---|---|
| from | `2026-08-01` | 期間の開始日（省略時は当月初日） |
| to | `2026-08-31` | 期間の終了日（省略時は当月末日） |
| type | `EXPENSE` | 種別での絞り込み（省略可） |
| categoryId | `3` | カテゴリでの絞り込み（省略可） |
| sort | `date` \| `amount` | 並び替え基準（省略時は `date`） |
| order | `asc` \| `desc` | 昇順/降順（省略時は `desc`） |

#### リクエスト例（POST /api/transactions）

```json
{
  "date": "2026-08-15",
  "amount": 1200,
  "type": "EXPENSE",
  "categoryId": 3,
  "memo": "スーパーで食料品購入"
}
```

#### レスポンス例（GET /api/transactions）

```json
{
  "items": [
    {
      "id": 101,
      "date": "2026-08-15",
      "amount": 1200,
      "type": "EXPENSE",
      "category": { "id": 3, "name": "食費" },
      "memo": "スーパーで食料品購入",
      "createdAt": "2026-08-15T20:00:00",
      "updatedAt": "2026-08-15T20:00:00"
    }
  ]
}
```

### 3.2 カテゴリ（Categories）

| メソッド | パス | 内容 |
|---|---|---|
| GET | `/api/categories` | 一覧取得（`?type=INCOME` で絞り込み可） |
| POST | `/api/categories` | 新規作成 |
| PUT | `/api/categories/{id}` | 名称の変更（`type` は変更不可） |
| DELETE | `/api/categories/{id}` | 削除（紐づく収支記録がある場合は409エラー） |

#### リクエスト例（POST /api/categories）

```json
{ "name": "食費", "type": "EXPENSE" }
```

### 3.3 月次集計（Summary）

| メソッド | パス | 内容 |
|---|---|---|
| GET | `/api/summary?month=2026-08` | 指定月のカテゴリ別合計・月次サマリーを取得 |

#### レスポンス例

```json
{
  "month": "2026-08",
  "incomeTotal": 300000,
  "expenseTotal": 85000,
  "balance": 215000,
  "incomeByCategory": [
    { "categoryId": 1, "name": "給与", "amount": 300000 }
  ],
  "expenseByCategory": [
    { "categoryId": 3, "name": "食費", "amount": 40000 },
    { "categoryId": 4, "name": "交通費", "amount": 15000 }
  ]
}
```

## 4. バリデーション・エラーレスポンス

要件定義書5章 No.8に対応。

| 項目 | ルール |
|---|---|
| date | 必須 |
| amount | 必須、1以上の整数 |
| type | 必須、`INCOME` \| `EXPENSE` のいずれか |
| categoryId | 必須、存在するカテゴリID、かつそのカテゴリの `type` が `type` と一致していること |
| memo | 任意、500文字以内 |
| category.name | 必須、1〜20文字、同一種別内で重複不可 |

### エラーレスポンス形式（共通）

```json
{
  "status": 400,
  "message": "入力値が不正です",
  "errors": [
    { "field": "amount", "message": "金額は1以上の整数で入力してください" }
  ]
}
```

| ステータス | ケース |
|---|---|
| 400 | バリデーションエラー |
| 404 | 指定したID（収支記録・カテゴリ）が存在しない |
| 409 | 使用中のカテゴリを削除しようとした場合 |

## 5. 保留事項の解消

要件定義書11章のうち、本書で決着した項目：

- **カテゴリの初期プリセット** → 代表的なカテゴリを数件のみ初期投入し、以降はユーザーが自由に追加・編集する方針とする（2章参照）
