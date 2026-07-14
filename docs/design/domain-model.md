# ドメインモデル設計

## Entity

### Employee（社員）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long (自動採番) | 社員ID（PK） |
| employeeCode | String | 社員コード（表示用） |
| name | String | 氏名 |
| email | String | メールアドレス（ログインID） |
| password | String | パスワード（BCrypt ハッシュ） |
| role | Role (enum) | EMPLOYEE / MANAGER / ADMIN |
| active | boolean | 有効フラグ（論理削除用） |
| version | Long | 楽観ロック用 |

### AttendanceRecord（勤怠記録）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long (自動採番) | 勤怠記録ID（PK） |
| employeeId | Long (FK) | 社員ID |
| date | LocalDate | 勤務日 |
| clockIn | LocalDateTime | 出勤時刻 |
| clockOut | LocalDateTime | 退勤時刻（null = 未退勤） |
| version | Long | 楽観ロック用 |

**一意制約**: (employeeId, date) — 1社員1日1レコード

## Value Object

### WorkDuration（勤務時間）

計算ロジックを持つ Value Object。Entity には永続化せず、集計時に算出する。

| メソッド | 説明 |
|---------|------|
| totalMinutes() | 出勤〜退勤の総分数 |
| breakMinutes() | 休憩控除分数（6h超→45分、8h超→60分） |
| actualMinutes() | 実勤務分数（total − break） |
| overtimeMinutes() | 残業分数（actual − 480分、下限0） |

### MonthlySummary（月次集計）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| yearMonth | YearMonth | 対象年月 |
| totalWorkingDays | int | 出勤日数 |
| totalActualMinutes | int | 月合計実勤務分数 |
| totalOvertimeMinutes | int | 月合計残業分数 |

## Enum

### Role

```
EMPLOYEE  — 一般社員
MANAGER   — 上司（部下の勤怠閲覧可）
ADMIN     — 管理者
```

## ドメイン関連図

```
┌──────────────┐        1    N  ┌────────────────────┐
│   Employee   │───────────────▶│  AttendanceRecord  │
│              │                 │                    │
│ - name       │                 │ - date             │
│ - email      │                 │ - clockIn          │
│ - role       │                 │ - clockOut         │
│ - active     │                 │                    │
└──────────────┘                 └────────────────────┘
                                          │
                                          │ 算出
                                          ▼
                                 ┌────────────────────┐
                                 │   WorkDuration     │
                                 │ (Value Object)     │
                                 └────────────────────┘
```

## Service

### AttendanceService

| メソッド | 説明 |
|---------|------|
| clockIn(employeeId) | 出勤打刻 |
| clockOut(employeeId) | 退勤打刻 |
| updateRecord(recordId, clockIn, clockOut) | 打刻修正 |
| getMonthlyRecords(employeeId, yearMonth) | 月次勤怠一覧取得 |
| getMonthlySummary(employeeId, yearMonth) | 月次集計取得 |

### EmployeeService

| メソッド | 説明 |
|---------|------|
| create(name, email, password, role) | 社員登録 |
| update(id, ...) | 社員情報更新 |
| deactivate(id) | 論理削除 |
| findById(id) | 社員取得 |
| findAll() | 社員一覧取得 |

### AuthService

| メソッド | 説明 |
|---------|------|
| login(email, password) | 認証・セッション発行 |
| getCurrentUser() | ログインユーザー取得 |

## Repository (interface)

- `EmployeeRepository`
- `AttendanceRecordRepository`
