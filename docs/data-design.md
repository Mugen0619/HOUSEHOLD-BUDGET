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
| source | VARCHAR(10) | NOT NULL, DEFAULT 'MANUAL', CHECK (source IN ('MANUAL', 'RECURRING')) | 手入力 / 定期支出からの自動生成（要件定義書5章 No.9） |
| recurring_transaction_id | BIGINT | NULL可, REFERENCES recurring_transactions(id) ON DELETE SET NULL | 生成元の定期支出テンプレート（1.3参照）。手入力の場合はNULL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | 作成日時（自動記録） |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() | 更新日時（自動記録、更新時にアプリ側で上書き） |

- `type` と `category_id` が指す `categories.type` は一致していなければならない（例：支出の記録に収入用カテゴリを紐づけることはできない）。DBのCHECK制約では表現しづらいため、Service層でのバリデーションとして実装する
- 物理削除とする（要件定義書11章）
- `source` は生成後に変わらない固定フラグ。`recurring_transaction_id` はテンプレート削除時に `SET NULL` されるが（要件定義書5.1）、`source = 'RECURRING'` はそのまま残るため、テンプレートが削除された後も一覧上の「定期」バッジ表示は維持される
- 自動生成後の収支記録は、通常の収支記録と同様に編集・削除できる（要件定義書5.1）。編集してもテンプレートとの紐付け（`recurring_transaction_id`）自体は変更しない

### 1.3 recurring_transactions（定期支出テンプレート）

| カラム | 型 | 制約 | 内容 |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | 自動採番 |
| name | VARCHAR(20) | NOT NULL | テンプレート名（1〜20文字、例：家賃） |
| amount | INTEGER | NOT NULL, CHECK (amount > 0) | 金額（円単位の正の整数） |
| type | VARCHAR(10) | NOT NULL, CHECK (type IN ('INCOME', 'EXPENSE')) | 収入 / 支出 |
| category_id | BIGINT | NOT NULL, REFERENCES categories(id) ON DELETE RESTRICT | 紐づくカテゴリ（`type`と一致すること） |
| execution_day | INTEGER | NOT NULL, CHECK (execution_day BETWEEN 1 AND 31) | 毎月の自動生成日 |
| memo | VARCHAR(500) | NULL可 | 任意入力（生成される収支記録にそのまま引き継ぐ） |
| created_at | TIMESTAMP | NOT NULL, DEFAULT now() | 作成日時 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT now() | 更新日時 |

- カテゴリ同様、`categories.type` に対して `ON DELETE RESTRICT` — 定期支出テンプレートで使用中のカテゴリは削除できない
- テンプレートの削除は物理削除。削除しても既に生成済みの `transactions` レコードは残る（`recurring_transaction_id` が `NULL` になるのみ）

### 1.4 ER図（概略）

```
categories (1) ──< (多) transactions
  id                     category_id (FK)
  name                    type（categoriesと一致させる）
  type                    recurring_transaction_id (FK, NULL可)

categories (1) ──< (多) recurring_transactions
  id                     category_id (FK)
                          type（categoriesと一致させる）

recurring_transactions (1) ──< (多) transactions
  id                     recurring_transaction_id (FK, ON DELETE SET NULL)
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
      "source": "MANUAL",
      "recurringTransactionId": null,
      "createdAt": "2026-08-15T20:00:00",
      "updatedAt": "2026-08-15T20:00:00"
    },
    {
      "id": 102,
      "date": "2026-08-25",
      "amount": 80000,
      "type": "EXPENSE",
      "category": { "id": 5, "name": "住居費" },
      "memo": "家賃",
      "source": "RECURRING",
      "recurringTransactionId": 1,
      "createdAt": "2026-08-25T00:10:00",
      "updatedAt": "2026-08-25T00:10:00"
    }
  ]
}
```

`source` が `RECURRING` の場合、フロントエンドは一覧上に「定期」バッジを表示する（[screen-design.md](./screen-design.md)参照）。POST/PUTのリクエストボディに `source`／`recurringTransactionId` は含めない（常にサーバー側で決定する。手入力APIからの作成・更新は常に `source = MANUAL` のまま）。

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

### 3.3 定期支出テンプレート（Recurring Transactions）

| メソッド | パス | 内容 |
|---|---|---|
| GET | `/api/recurring-transactions` | 一覧取得（`?type=EXPENSE` で絞り込み可） |
| POST | `/api/recurring-transactions` | 新規作成 |
| PUT | `/api/recurring-transactions/{id}` | 更新 |
| DELETE | `/api/recurring-transactions/{id}` | 削除（既に生成済みの収支記録には影響しない。1.2参照） |

#### リクエスト例（POST /api/recurring-transactions）

```json
{
  "name": "家賃",
  "amount": 80000,
  "type": "EXPENSE",
  "categoryId": 5,
  "executionDay": 25,
  "memo": "家賃"
}
```

#### レスポンス例（GET /api/recurring-transactions）

```json
{
  "items": [
    {
      "id": 1,
      "name": "家賃",
      "amount": 80000,
      "type": "EXPENSE",
      "category": { "id": 5, "name": "住居費" },
      "executionDay": 25,
      "memo": "家賃",
      "createdAt": "2026-08-01T10:00:00",
      "updatedAt": "2026-08-01T10:00:00"
    }
  ]
}
```

### 3.4 月次集計（Summary）

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
| recurringTransaction.name | 必須、1〜20文字 |
| recurringTransaction.executionDay | 必須、1以上31以下の整数 |

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
| 404 | 指定したID（収支記録・カテゴリ・定期支出テンプレート）が存在しない |
| 409 | 使用中のカテゴリを削除しようとした場合（収支記録・定期支出テンプレートいずれかで使用中の場合を含む） |

## 5. 定期支出の自動生成バッチ

要件定義書5章 No.9に対応。

### 5.1 実行方式

- Spring Bootの `@Scheduled` により、**アプリ起動中は毎日決まった時刻（例：0:10）に1回**、有効な全ての `recurring_transactions` をチェックするジョブを実行する
- 各テンプレートについて、「当月分の収支記録が既に生成済みか」を `transactions` テーブルから確認する（`recurring_transaction_id = テンプレートID` かつ `date` が当月内のレコードの有無で判定）
- 未生成であり、かつ「当月の実行日（5.2参照）」を過ぎている場合、その場で当月分の収支記録を生成する（`date` = 当月の実行日、`source = 'RECURRING'`）

### 5.2 常時起動していない環境への対応（キャッチアップ方式）

このアプリはDocker/バックエンド/フロントエンドを手動で起動するローカル運用（要件定義書7章：公開環境への常時デプロイは対象外）のため、「毎日決まった時刻に必ず実行される」ことは前提にできない（例：実行日を過ぎてからアプリを起動するケースがある）。

そのため、5.1のジョブは「その時刻ちょうどに動く」ことを目的とせず、**「アプリが起動している間に、まだ生成されていない当月分があれば生成する」というキャッチアップ方式**とする。これにより、アプリ起動時にも同じジョブ（または起動時に1回限りの同等チェック）を実行しておけば、多少実行日から日にちが経っていても、次にアプリを起動したタイミングで確実に生成される。

### 5.3 月末エッジケース

実行日（`execution_day`）が対象月に存在しない日（例：31日を指定していて2月は28日までしかない）の場合、**その月の最終日に生成する**（例：2月なら2月28日、うるう年は2月29日）。生成される `date` はテンプレートの `execution_day` そのままではなく、実際にその月に存在する日に丸めた値とする。

## 6. 保留事項の解消

要件定義書11章のうち、本書で決着した項目：

- **カテゴリの初期プリセット** → 代表的なカテゴリを数件のみ初期投入し、以降はユーザーが自由に追加・編集する方針とする（2章参照）
- **定期支出テンプレートの実行日が月末を超える場合の挙動** → その月の最終日に丸めて生成する（5.3参照）
- **バッチ処理の実行タイミング** → Spring Schedulerによる定時実行を基本としつつ、ローカル運用で常時起動していない前提を踏まえ、アプリ起動中にキャッチアップ生成する方式とする（5.1・5.2参照）
- **定期支出テンプレート削除時の挙動** → `transactions.recurring_transaction_id` を `ON DELETE SET NULL` とし、生成済みレコードはそのまま残す。`source = 'RECURRING'` は変更されないため「定期」バッジ表示も維持される（1.2参照）
