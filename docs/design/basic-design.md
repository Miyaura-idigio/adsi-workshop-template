# 基本設計書 — 勤怠管理アプリケーション

## 1. システム概要

社員の出退勤を Web 画面で打刻・管理する勤怠管理アプリケーション。

| 項目 | 内容 |
|------|------|
| 対象ユーザー | 社員（一般）+ 管理者 |
| 想定規模 | 数十人 |
| 勤務形態 | フレックスタイム制（所定労働時間 8時間） |
| 認証方式 | ID（メールアドレス）/ パスワード |

## 2. システムアーキテクチャ

### 2.1 技術スタック

| レイヤー | 技術 | 備考 |
|---------|------|------|
| Frontend | TypeScript / Next.js | サーバーコンポーネント + Client コンポーネント |
| Backend | Java / Spring Boot | REST API |
| DB | H2 | 開発環境。Flyway でマイグレーション管理 |
| 認証 | Spring Security | セッションベース |

### 2.2 システム構成図

```
┌──────────────────┐       HTTP        ┌──────────────────┐       JDBC       ┌────────┐
│   Browser        │──────────────────▶│  Next.js (3000)  │                  │        │
│                  │◀──────────────────│  (SSR + SPA)     │                  │        │
└──────────────────┘                   └────────┬─────────┘                  │   H2   │
                                                │ /api/* proxy               │  (DB)  │
                                                ▼                            │        │
                                       ┌──────────────────┐                  │        │
                                       │ Spring Boot(8080)│─────────────────▶│        │
                                       │  REST API        │◀─────────────────│        │
                                       └──────────────────┘                  └────────┘
```

### 2.3 レイヤー構成（Backend）

```
Controller → Service (interface) → Repository (interface) → Entity
```

- Controller: HTTP リクエスト/レスポンスの変換
- Service: ビジネスロジック（トランザクション境界）
- Repository: データアクセス（Spring Data JPA）
- Entity: ドメインオブジェクト

## 3. ドメインモデル

### 3.1 Entity

#### Employee（社員）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long (自動採番) | 社員ID（PK） |
| employeeCode | String | 社員コード（表示用、UNIQUE） |
| name | String | 氏名 |
| email | String | メールアドレス（ログインID、UNIQUE） |
| password | String | パスワード（BCrypt ハッシュ） |
| role | Role (enum) | EMPLOYEE / ADMIN |
| active | boolean | 有効フラグ（論理削除用） |
| version | Long | 楽観ロック用 |

#### AttendanceRecord（勤怠記録）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | Long (自動採番) | 勤怠記録ID（PK） |
| employeeId | Long (FK) | 社員ID |
| date | LocalDate | 勤務日 |
| clockIn | LocalDateTime | 出勤時刻 |
| clockOut | LocalDateTime | 退勤時刻（null = 未退勤） |
| version | Long | 楽観ロック用 |

**一意制約**: (employeeId, date) — 1社員1日1レコード

### 3.2 Value Object

#### WorkDuration（勤務時間）

| メソッド | 説明 |
|---------|------|
| totalMinutes() | 出勤〜退勤の総分数 |
| breakMinutes() | 休憩控除分数（6h超→45分、8h超→60分） |
| actualMinutes() | 実勤務分数（total − break） |
| overtimeMinutes() | 残業分数（actual − 480分、下限0） |

#### MonthlySummary（月次集計）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| yearMonth | YearMonth | 対象年月 |
| totalWorkingDays | int | 出勤日数 |
| totalActualMinutes | int | 月合計実勤務分数 |
| totalOvertimeMinutes | int | 月合計残業分数 |

### 3.3 ドメイン関連図

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

## 4. DB 設計

### 4.1 テーブル定義

#### employees

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

#### attendance_records

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

### 4.2 ER 図

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

### 4.3 インデックス

| テーブル | カラム | 用途 |
|---------|--------|------|
| employees | email | ログイン時検索（UNIQUE 制約が兼ねる） |
| attendance_records | (employee_id, date) | 社員×日付検索（UNIQUE 制約が兼ねる） |

### 4.4 マイグレーション構成

```
src/main/resources/db/migration/
  V1__create_employees.sql
  V2__create_attendance_records.sql
  V3__insert_initial_admin.sql
```

## 5. API 設計

### 5.1 エンドポイント一覧

| メソッド | パス | 説明 | 認証 | ロール |
|---------|------|------|:----:|--------|
| POST | /api/auth/login | ログイン | 不要 | — |
| GET | /api/auth/me | ログインユーザー取得 | 要 | 全員 |
| POST | /api/auth/logout | ログアウト | 要 | 全員 |
| POST | /api/attendance/clock-in | 出勤打刻 | 要 | 全員 |
| POST | /api/attendance/clock-out | 退勤打刻 | 要 | 全員 |
| GET | /api/attendance/today | 本日の打刻状態取得 | 要 | 全員 |
| GET | /api/attendance/records?yearMonth= | 月次勤怠一覧（自分） | 要 | 全員 |
| GET | /api/attendance/summary?yearMonth= | 月次集計（自分） | 要 | 全員 |
| PUT | /api/attendance/records/{id} | 打刻修正 | 要 | 本人 or ADMIN |
| GET | /api/admin/attendance/records | 社員勤怠一覧 | 要 | ADMIN |
| GET | /api/admin/attendance/summary | 社員月次集計 | 要 | ADMIN |
| GET | /api/admin/employees | 社員一覧 | 要 | ADMIN |
| POST | /api/admin/employees | 社員登録 | 要 | ADMIN |
| GET | /api/admin/employees/{id} | 社員詳細 | 要 | ADMIN |
| PUT | /api/admin/employees/{id} | 社員更新 | 要 | ADMIN |
| DELETE | /api/admin/employees/{id} | 社員論理削除 | 要 | ADMIN |

### 5.2 主要リクエスト/レスポンス

#### ログイン

```json
// POST /api/auth/login
// Request
{ "email": "tanaka@example.com", "password": "password123" }

// Response 200
{ "employee": { "id": 1, "employeeCode": "EMP001", "name": "田中太郎", "email": "tanaka@example.com", "role": "EMPLOYEE", "active": true } }
```

#### 出勤打刻

```json
// POST /api/attendance/clock-in
// Response 200
{ "id": 1, "employeeId": 1, "date": "2026-07-14", "clockIn": "2026-07-14T09:00:00", "clockOut": null, "workingMinutes": null, "overtimeMinutes": null }
```

#### 月次集計

```json
// GET /api/attendance/summary?yearMonth=2026-07
// Response 200
{ "yearMonth": "2026-07", "totalWorkingDays": 15, "totalWorkingMinutes": 7200, "totalOvertimeMinutes": 600 }
```

### 5.3 エラーレスポンス

| HTTP ステータス | 用途 |
|---------------|------|
| 400 | バリデーションエラー |
| 401 | 未認証 |
| 403 | 権限不足 |
| 404 | リソース未存在 |
| 409 | 競合（重複打刻、メール重複等） |

## 6. 画面設計

### 6.1 画面一覧

| # | 画面名 | パス | 対象ロール | 関連機能 |
|---|--------|------|-----------|----------|
| S-1 | ログイン | /login | 全員 | F-12 |
| S-2 | 打刻（ホーム） | / | 全員 | F-1, F-2 |
| S-3 | 勤怠履歴（自分） | /attendance | 全員 | F-5, F-6, F-7, F-8 |
| S-4 | 打刻修正 | /attendance/edit/:id | 全員 | F-3 |
| S-5 | 社員勤怠一覧 | /admin/attendance | ADMIN | F-4, F-5, F-6 |
| S-6 | 社員管理一覧 | /admin/employees | ADMIN | F-9, F-10, F-11 |
| S-7 | 社員登録 | /admin/employees/new | ADMIN | F-9 |
| S-8 | 社員編集 | /admin/employees/:id/edit | ADMIN | F-10 |

### 6.2 画面遷移図

```
                         ┌─────────┐
                         │  ログイン │
                         │  (S-1)  │
                         └────┬────┘
                              │ ログイン成功
                              ▼
                    ┌──────────────────┐
                    │  打刻（ホーム）    │
                    │      (S-2)       │
                    └───┬──────────┬───┘
                        │          │
          ┌─────────────┘          └──────────────┐
          ▼                                       ▼
┌──────────────────┐                    ┌──────────────────┐
│ 勤怠履歴（自分）   │                    │  管理者メニュー    │
│     (S-3)        │                    │ (ADMIN のみ表示)  │
└───────┬──────────┘                    └───┬──────────┬───┘
        │ 修正リンク                        │          │
        ▼                                   ▼          ▼
┌──────────────────┐              ┌────────────┐ ┌────────────┐
│   打刻修正        │              │社員勤怠一覧 │ │社員管理一覧 │
│    (S-4)         │              │   (S-5)    │ │   (S-6)    │
└──────────────────┘              └────────────┘ └──┬─────┬───┘
                                                    │     │
                                              ┌─────┘     └─────┐
                                              ▼                 ▼
                                        ┌──────────┐     ┌──────────┐
                                        │ 社員登録  │     │ 社員編集  │
                                        │  (S-7)   │     │  (S-8)   │
                                        └──────────┘     └──────────┘
```

### 6.3 画面詳細

#### S-1: ログイン

| 要素 | 種類 | バリデーション |
|------|------|-------------|
| メールアドレス | テキスト入力 | 必須、メール形式 |
| パスワード | パスワード入力 | 必須、8文字以上 |
| ログインボタン | ボタン | — |
| エラーメッセージ | 表示エリア | 認証失敗時に表示 |

#### S-2: 打刻（ホーム）

| 要素 | 種類 | 条件 |
|------|------|------|
| 現在時刻 | リアルタイム表示 | 毎秒更新 |
| 打刻状態 | テキスト | 未出勤 / 出勤済み / 退勤済み |
| 出勤ボタン | ボタン | 未出勤時のみ活性 |
| 退勤ボタン | ボタン | 出勤済み・未退勤時のみ活性 |
| 出勤時刻 | テキスト | 打刻済みの場合表示 |
| 退勤時刻 | テキスト | 打刻済みの場合表示 |

#### S-3: 勤怠履歴（自分）

| 要素 | 種類 | 説明 |
|------|------|------|
| 年月セレクタ | ドロップダウン | デフォルト: 当月 |
| 日別一覧テーブル | テーブル | 日付/出勤/退勤/勤務時間/残業時間 |
| 修正リンク | リンク | 各行に配置、S-4 へ遷移 |
| 月次集計 | 集計エリア | 出勤日数/合計勤務時間/合計残業時間 |

#### S-4: 打刻修正

| 要素 | 種類 | バリデーション |
|------|------|-------------|
| 対象日 | テキスト表示 | 編集不可 |
| 出勤時刻 | datetime 入力 | 必須 |
| 退勤時刻 | datetime 入力 | 任意（出勤時刻より後） |
| 保存ボタン | ボタン | — |
| キャンセルボタン | ボタン | S-3 に戻る |

#### S-5: 社員勤怠一覧（管理者）

| 要素 | 種類 | 説明 |
|------|------|------|
| 社員選択 | ドロップダウン | 全社員表示 |
| 年月セレクタ | ドロップダウン | デフォルト: 当月 |
| 日別一覧テーブル | テーブル | S-3 と同構成 |
| 修正リンク | リンク | 管理者のみ表示 |
| 月次集計 | 集計エリア | 出勤日数/合計勤務時間/合計残業時間 |

#### S-6: 社員管理一覧

| 要素 | 種類 | 説明 |
|------|------|------|
| 社員テーブル | テーブル | コード/氏名/メール/ロール/状態 |
| 編集ボタン | ボタン | 各行、S-8 へ遷移 |
| 削除ボタン | ボタン | 各行、確認ダイアログ後に論理削除 |
| 新規登録ボタン | ボタン | S-7 へ遷移 |

#### S-7: 社員登録

| 要素 | 種類 | バリデーション |
|------|------|-------------|
| 氏名 | テキスト入力 | 必須、100文字以内 |
| メールアドレス | テキスト入力 | 必須、メール形式、重複不可 |
| パスワード | パスワード入力 | 必須、8文字以上 |
| ロール | セレクト | 一般社員 / 管理者 |
| 登録ボタン | ボタン | — |
| キャンセルボタン | ボタン | S-6 に戻る |

#### S-8: 社員編集

| 要素 | 種類 | バリデーション |
|------|------|-------------|
| 氏名 | テキスト入力 | 必須、100文字以内（既存値プリセット） |
| メールアドレス | テキスト入力 | 必須、メール形式 |
| ロール | セレクト | 一般社員 / 管理者 |
| 保存ボタン | ボタン | — |
| キャンセルボタン | ボタン | S-6 に戻る |

## 7. 機能設計

### 7.1 機能一覧

| # | 機能名 | 区分 | 説明 |
|---|--------|------|------|
| F-1 | 出勤打刻 | 打刻 | 当日未打刻時に現在時刻を出勤として記録 |
| F-2 | 退勤打刻 | 打刻 | 出勤済み・未退勤時に現在時刻を退勤として記録 |
| F-3 | 打刻修正（本人） | 打刻 | 自分の過去の打刻を編集 |
| F-4 | 打刻修正（管理者） | 打刻 | 任意の社員の打刻を編集 |
| F-5 | 月次勤怠一覧表示 | 履歴 | 指定月の日別勤怠を一覧表示 |
| F-6 | 月次集計表示 | 履歴 | 月の合計勤務時間・残業時間を算出表示 |
| F-7 | 休憩時間自動控除 | 計算 | 6h超→45分、8h超→60分を自動控除 |
| F-8 | 残業時間計算 | 計算 | 実勤務時間 − 8h（下限0） |
| F-9 | 社員登録 | 社員管理 | 氏名・メール・パスワード・ロールで社員作成 |
| F-10 | 社員編集 | 社員管理 | 社員情報の更新 |
| F-11 | 社員削除（論理） | 社員管理 | active フラグを false にする |
| F-12 | ログイン | 認証 | メール + パスワードで認証、セッション開始 |
| F-13 | ログアウト | 認証 | セッション破棄 |
| F-14 | 権限制御 | 認証 | ロール別アクセス制限 |

### 7.2 ビジネスルール詳細

#### 休憩時間自動控除（F-7）

```
if 総勤務時間 > 8時間:
    休憩控除 = 60分
elif 総勤務時間 > 6時間:
    休憩控除 = 45分
else:
    休憩控除 = 0分
```

#### 残業時間計算（F-8）

```
実勤務時間 = 総勤務時間 − 休憩控除
残業時間 = max(0, 実勤務時間 − 480分)
```

#### 打刻制約

| 操作 | 事前条件 | 違反時 |
|------|---------|--------|
| 出勤打刻 | 当日レコードなし | 409 Conflict |
| 退勤打刻 | 当日出勤済み & 未退勤 | 409 Conflict |
| 打刻修正 | 本人 or ADMIN | 403 Forbidden |

## 8. セキュリティ設計

### 8.1 認証

| 項目 | 方式 |
|------|------|
| 認証方式 | セッションベース（Spring Security） |
| パスワード保存 | BCrypt ハッシュ |
| セッション管理 | サーバーサイドセッション |

### 8.2 認可（権限制御）

| パスパターン | 許可ロール |
|-------------|-----------|
| /api/auth/** | 全員（認証不要） |
| /api/attendance/** | 認証済みユーザー |
| /api/admin/** | ADMIN のみ |

### 8.3 セキュリティ対策

| 脅威 | 対策 |
|------|------|
| SQL インジェクション | Spring Data JPA（パラメータバインディング） |
| XSS | React デフォルトエスケープ、dangerouslySetInnerHTML 不使用 |
| CSRF | Spring Security CSRF トークン |
| パスワード漏洩 | BCrypt ハッシュ化、ログ出力禁止 |
| 情報漏洩 | エラー詳細をクライアントに返さない |

## 9. Service 設計

### 9.1 AttendanceService

| メソッド | 入力 | 出力 | 説明 |
|---------|------|------|------|
| clockIn(employeeId) | 社員ID | AttendanceRecord | 出勤打刻。当日レコード存在時は 409 |
| clockOut(employeeId) | 社員ID | AttendanceRecord | 退勤打刻。未出勤/退勤済み時は 409 |
| updateRecord(recordId, clockIn, clockOut) | レコードID, 時刻 | AttendanceRecord | 打刻修正。権限チェックあり |
| getMonthlyRecords(employeeId, yearMonth) | 社員ID, 年月 | List\<AttendanceRecord\> | 月次勤怠一覧 |
| getMonthlySummary(employeeId, yearMonth) | 社員ID, 年月 | MonthlySummary | 月次集計 |

### 9.2 EmployeeService

| メソッド | 入力 | 出力 | 説明 |
|---------|------|------|------|
| create(name, email, password, role) | 社員情報 | Employee | 社員登録。メール重複時は 409 |
| update(id, name, email, role) | 更新情報 | Employee | 社員情報更新 |
| deactivate(id) | 社員ID | void | 論理削除（active=false） |
| findById(id) | 社員ID | Employee | 社員取得。未存在時は 404 |
| findAll() | — | List\<Employee\> | 全社員取得（active のみ） |

### 9.3 AuthService

| メソッド | 入力 | 出力 | 説明 |
|---------|------|------|------|
| login(email, password) | 認証情報 | Employee | 認証。失敗時は 401 |
| getCurrentUser() | — | Employee | セッションからユーザー取得 |

## 10. 非機能要件

| 項目 | 内容 |
|------|------|
| 可用性 | 開発環境のため特記なし |
| パフォーマンス | 数十人規模。ページネーション不要 |
| データ整合性 | 楽観ロック（@Version）で並行更新を検出 |
| ログ | SLF4J。ERROR/WARN/INFO/DEBUG の使い分け |
| テスト | カバレッジ 80% 目標。TDD で実装 |

## 11. 参照ドキュメント

| ドキュメント | パス |
|-------------|------|
| 要求仕様書 | docs/requirements/attendance-app.md |
| ドメインモデル設計 | docs/design/domain-model.md |
| DB 設計 | docs/design/database.md |
| API 定義（OpenAPI） | docs/design/api.yaml |
| 画面・機能一覧 | docs/design/screens-and-features.md |
