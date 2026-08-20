# 技術スタック：家計簿（収支管理）アプリ

要件定義書（[docs/requirements.md](./requirements.md)）8章の詳細版。バージョンは要件定義書に準拠し、本書では構成・主要ライブラリ・ディレクトリ構成を整理する。

## 1. 全体構成

```
[ブラウザ (React)] --HTTP(REST API, JSON)--> [Spring Boot] --JDBC--> [PostgreSQL (Docker)]
```

- フロントエンドとバックエンドは別プロセスで動作し、開発時はVite開発サーバー（フロント）とSpring Boot（バックエンド）をそれぞれ起動する
- データはブラウザのlocalStorageに保存せず、必ずバックエンドAPI経由でPostgreSQLに保存する

## 2. リポジトリ構成

前作（Trello風タスク管理アプリ）に準拠し、モノレポ構成とする。

```
HOUSEHOLD-BUDGET/
├── frontend/        # React + TypeScript + Vite
├── backend/          # Spring Boot + Gradle
├── docker-compose.yml # PostgreSQL
└── docs/
```

## 3. フロントエンド

| 区分 | 技術 | バージョン |
|---|---|---|
| フレームワーク | React | 19.2.8 |
| 言語 | TypeScript | 5.7.3 |
| ビルドツール | Vite | 6.4.3 |

### 3.1 主要ライブラリ

| 用途 | 選定 | 理由 |
|---|---|---|
| HTTP通信 | 標準の `fetch` API | 「素のReact」方針のため、axios等の追加ライブラリは導入しない |
| グラフ表示（月別・カテゴリ別集計） | Chart.js + react-chartjs-2 | 円グラフ・棒グラフに対応し、ドキュメント・実績が豊富。要件定義書11章で候補としていたものを採用する |
| 状態管理 | Reactの標準Hooks（useState / useEffect等） | 画面数・状態が少なく、Redux等の外部状態管理ライブラリは不要と判断 |
| ルーティング | 使用しない（1ページ内でタブ切り替え） | 画面が「一覧」「カテゴリ管理」「月次集計」の3つのみで、URL遷移を分ける必要性が薄いため（詳細は[screen-design.md](./screen-design.md)参照） |
| スタイリング | 素のCSS（CSS Modules） | UIライブラリは導入せず、学習目的でCSSを直接書く |
| テスト | Vitest + React Testing Library | Viteと親和性が高い標準的な組み合わせ |

### 3.2 ディレクトリ構成（案）

```
frontend/
├── src/
│   ├── api/            # バックエンドAPI呼び出し関数
│   ├── components/      # 画面共通・再利用コンポーネント
│   ├── features/
│   │   ├── transactions/ # 収支記録の一覧・作成・編集・削除
│   │   ├── categories/   # カテゴリ管理
│   │   └── summary/       # 月次集計（表・グラフ）
│   ├── types/            # APIレスポンス等の型定義
│   ├── App.tsx
│   └── main.tsx
├── package.json
└── vite.config.ts
```

## 4. バックエンド

| 区分 | 技術 | バージョン |
|---|---|---|
| 言語 | Java | 25（Gradle toolchainで指定） |
| フレームワーク | Spring Boot | 4.1.0 |
| ビルドツール | Gradle（Wrapper同梱） | 9.5.1 |

### 4.1 主要ライブラリ（Spring Boot Starter）

| 用途 | 依存関係 | 理由 |
|---|---|---|
| REST API | spring-boot-starter-web | |
| DBアクセス | spring-boot-starter-data-jpa | ORMとしてHibernateを利用し、SQLを直接書かずにテーブル操作を行う |
| バリデーション | spring-boot-starter-validation | 金額・必須項目等のリクエストバリデーション（Bean Validation） |
| DB接続（本番/開発） | PostgreSQL Driver | Docker上のPostgreSQL 16に接続 |
| DB（テスト） | H2 Database | 結合テストではPostgreSQLの代わりにインメモリDBを使用し、テスト実行を高速化・簡略化する |
| 開発補助 | spring-boot-devtools | ホットリロード |
| テスト | spring-boot-starter-test（JUnit 5, Mockito, AssertJ） | 単体テスト・結合テストの標準構成 |

### 4.2 ディレクトリ構成（案）

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/householdbudget/
│   │   │   ├── transaction/  # 収支記録：Controller / Service / Repository / Entity
│   │   │   ├── category/     # カテゴリ：Controller / Service / Repository / Entity
│   │   │   ├── summary/      # 月次集計：Controller / Service
│   │   │   └── common/       # 共通の例外ハンドラ等
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── build.gradle
└── settings.gradle
```

## 5. データベース

| 区分 | 技術 | バージョン |
|---|---|---|
| DBMS | PostgreSQL | 16（Dockerコンテナ） |

`docker-compose.yml` でPostgreSQLコンテナを起動する。テーブル定義・マイグレーション方針は[data-design.md](./data-design.md)を参照。

## 6. 開発環境の起動方法（案）

| コンポーネント | 起動コマンド（案） | ポート |
|---|---|---|
| PostgreSQL | `docker compose up -d` | 5432 |
| バックエンド | `./gradlew bootRun` | 8080 |
| フロントエンド | `npm run dev` | 5173（Viteデフォルト） |

ポート番号は固定とし、競合時は使用中プロセスを停止してから起動し直す（ユーザー共通ルールに準拠）。具体的な設定値は実装時に `application.yml` / `vite.config.ts` に反映する。

## 7. テスト方針（詳細）

要件定義書9章の詳細版。

| 対象 | 手法 | ツール |
|---|---|---|
| バックエンド単体テスト | Service層のロジックをMockitoでモック化してテスト | JUnit 5 + Mockito |
| バックエンド結合テスト | Controller〜Repositoryを通しでテスト（H2使用） | Spring Boot Test（`@SpringBootTest`） |
| フロントエンド単体テスト | コンポーネントの表示・操作をテスト | Vitest + React Testing Library |
| 手動テスト | グラフ表示・見た目の確認など自動化が非効率な箇所 | ブラウザで目視確認 |

## 8. 保留事項の解消

要件定義書11章のうち、本書で決着した項目：

- **グラフライブラリの選定** → Chart.js + react-chartjs-2 に決定（3.1参照）
