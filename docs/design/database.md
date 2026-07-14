# DB 設計

DB: H2（開発環境）。マイグレーション: Flyway。

## テーブル定義

### employees

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 社員ID |
| employee_code | VARCHAR(20) | UNIQUE, NOT NULL | 社員コード |
| name | VARCHAR(100) | NOT NULL | 氏名 |
| email | VARCHAR(255) | UNIQUE, NOT NULL | メールアドレス |
| password | VARCHAR(255) | NOT NULL | パスワード（BCrypt） |
| role | VARCHAR(20) | NOT NULL | EMPLOYEE / ADMIN |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE | 有効フラグ |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |
| created_at | TIMESTAMP | NOT NULL | 作成日時 |
| updated_at | TIMESTAMP | NOT NULL | 更新日時 |

### attendance_records

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 勤怠記録ID |
| employee_id | BIGINT | FK(employees.id), NOT NULL | 社員ID |
| date | DATE | NOT NULL | 勤務日 |
| clock_in | TIMESTAMP | NOT NULL | 出勤時刻 |
| clock_out | TIMESTAMP | NULL | 退勤時刻 |
| version | BIGINT | NOT NULL, DEFAULT 0 | 楽観ロック |
| created_at | TIMESTAMP | NOT NULL | 作成日時 |
| updated_at | TIMESTAMP | NOT NULL | 更新日時 |

**一意制約**: UNIQUE(employee_id, date)

## ER 図

```
┌─────────────────────┐         ┌─────────────────────────┐
│      employees      │         │   attendance_records    │
├─────────────────────┤         ├─────────────────────────┤
│ PK id               │◀───┐    │ PK id                   │
│    employee_code    │    │    │ FK employee_id          │──┘
│    name             │    │    │    date                 │
│    email            │    └────│    clock_in             │
│    password         │         │    clock_out            │
│    role             │         │    version              │
│    active           │         │    created_at           │
│    version          │         │    updated_at           │
│    created_at       │         └─────────────────────────┘
│    updated_at       │
└─────────────────────┘
```

## インデックス

- `attendance_records(employee_id, date)` — 社員×日付での検索用（一意制約が兼ねる）
- `employees(email)` — ログイン時の検索用（一意制約が兼ねる）

## Flyway マイグレーション構成

```
src/main/resources/db/migration/
  V1__create_employees.sql
  V2__create_attendance_records.sql
  V3__insert_initial_admin.sql
```
