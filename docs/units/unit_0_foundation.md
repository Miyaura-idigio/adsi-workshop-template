# Unit 0: 共通基盤 + 認証

## 概要

プロジェクトのスキャフォールド、DB マイグレーション、認証機能を構築する。
Unit 1・Unit 2 が並行で作業するための共通基盤。

## 前提

- なし（最初に実装する Unit）

## 担当範囲

### Backend

| レイヤー | 成果物 |
|---------|--------|
| プロジェクト構成 | Spring Boot アプリケーション骨格（pom.xml, Application クラス, テスト基盤） |
| DB マイグレーション | V1__create_employees.sql, V2__create_attendance_records.sql, V3__insert_initial_admin.sql |
| Entity | Employee, AttendanceRecord, Role enum |
| Repository | EmployeeRepository, AttendanceRecordRepository |
| Security | SecurityFilterChain, BCryptPasswordEncoder, UserDetailsService 実装 |
| Auth API | AuthController（login / me / logout） |
| Service | AuthService (interface + impl) |
| 例外ハンドリング | GlobalExceptionHandler（@RestControllerAdvice） |
| Value Object | WorkDuration（計算ロジック） |
| テスト基盤 | application-test.yml, テストユーティリティ |

### Frontend

| レイヤー | 成果物 |
|---------|--------|
| プロジェクト構成 | Next.js アプリケーション骨格（package.json, next.config, レイアウト） |
| API クライアント | fetch ラッパー（withBasePath 対応）, 型定義 |
| 認証 | ログイン画面 (S-1), 認証コンテキスト, ログアウト機能 |
| 共通 UI | ナビゲーション, レイアウト, 権限ガード |

### API エンドポイント

| メソッド | パス | 説明 |
|---------|------|------|
| POST | /api/auth/login | ログイン |
| GET | /api/auth/me | ログインユーザー取得 |
| POST | /api/auth/logout | ログアウト |

### 画面

| # | 画面名 | パス |
|---|--------|------|
| S-1 | ログイン | /login |
| — | 共通レイアウト（ナビゲーション） | — |

### DB テーブル

- employees（全カラム）
- attendance_records（全カラム）

## 機能

| # | 機能名 |
|---|--------|
| F-12 | ログイン |
| F-13 | ログアウト |
| F-14 | 権限制御（SecurityFilterChain による URL パターン認可） |

## テスト観点

### Backend

- [ ] Employee Entity の永続化・取得
- [ ] AttendanceRecord Entity の永続化・取得
- [ ] EmployeeRepository: findByEmail
- [ ] AuthService: login 成功 / 失敗（401）
- [ ] AuthController: POST /auth/login → 200 / 401
- [ ] AuthController: GET /auth/me → 200（認証済み）/ 401（未認証）
- [ ] AuthController: POST /auth/logout → セッション破棄
- [ ] SecurityFilterChain: 認可パターンのテスト
- [ ] WorkDuration: 休憩控除計算（6h以下/6h超/8h超）
- [ ] WorkDuration: 残業計算
- [ ] GlobalExceptionHandler: 各例外のレスポンス形式
- [ ] Flyway マイグレーション正常実行

### Frontend

- [ ] ログイン画面: 正常ログイン → ホームへ遷移
- [ ] ログイン画面: 認証失敗 → エラーメッセージ表示
- [ ] ログイン画面: バリデーション（必須、メール形式、8文字以上）
- [ ] 認証ガード: 未認証時 → ログインへリダイレクト
- [ ] ナビゲーション: ロール別メニュー表示切替

## 完了基準

- `mvn test` が全件パス
- `npm run lint` / `npm run test` が全件パス
- ログイン → ホーム遷移がブラウザで動作確認できる
- Unit 1 / Unit 2 が、この基盤に import して独立に TDD を開始できる

## 実装順序

1. Backend プロジェクト骨格（pom.xml, Application, application.yml）
2. Flyway マイグレーション（V1〜V3）
3. Entity + Repository
4. WorkDuration Value Object + テスト
5. AuthService + テスト
6. SecurityFilterChain + AuthController + テスト
7. GlobalExceptionHandler + テスト
8. Frontend プロジェクト骨格
9. API クライアント + 型定義
10. ログイン画面 + 認証コンテキスト
11. 共通レイアウト + ナビゲーション + 権限ガード
