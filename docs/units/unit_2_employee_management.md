# Unit 2: 社員管理ドメイン（CRUD）

## 概要

管理者による社員の登録・編集・論理削除を実装する。
ADMIN ロール専用の管理機能。

## 依存

- **Unit 0（共通基盤）** — Entity, Repository, 認証を使用

## 独立性

- Unit 1（勤怠ドメイン）とは互いに依存しない → **並行実装可能**

## 担当範囲

### Backend

| レイヤー | 成果物 |
|---------|--------|
| Service | EmployeeService (interface + impl) |
| Controller | AdminEmployeeController |
| DTO | CreateEmployeeRequest, UpdateEmployeeRequest, EmployeeResponse |

### Frontend

| レイヤー | 成果物 |
|---------|--------|
| 画面 | S-6 社員管理一覧, S-7 社員登録, S-8 社員編集 |
| hooks | useEmployees |
| API | employee API クライアント関数 |

### API エンドポイント

| メソッド | パス | 説明 | ロール |
|---------|------|------|--------|
| GET | /api/admin/employees | 社員一覧 | ADMIN |
| POST | /api/admin/employees | 社員登録 | ADMIN |
| GET | /api/admin/employees/{id} | 社員詳細 | ADMIN |
| PUT | /api/admin/employees/{id} | 社員更新 | ADMIN |
| DELETE | /api/admin/employees/{id} | 社員論理削除 | ADMIN |

### 画面

| # | 画面名 | パス | 対象ロール |
|---|--------|------|-----------|
| S-6 | 社員管理一覧 | /admin/employees | ADMIN |
| S-7 | 社員登録 | /admin/employees/new | ADMIN |
| S-8 | 社員編集 | /admin/employees/:id/edit | ADMIN |

## 機能

| # | 機能名 | 説明 |
|---|--------|------|
| F-9 | 社員登録 | 氏名・メール・パスワード・ロールで社員作成（employeeCode 自動採番） |
| F-10 | 社員編集 | 社員情報の更新 |
| F-11 | 社員削除（論理） | active フラグを false にする |

## ビジネスルール

| ルール | 詳細 |
|--------|------|
| メール一意制約 | 登録・更新時にメールアドレスの重複チェック → 409 |
| 社員コード自動採番 | EMP001, EMP002, ... 形式。最大値 + 1 |
| パスワードハッシュ | BCrypt で保存。登録時のみパスワード指定 |
| 論理削除 | DELETE は active=false にする（物理削除しない） |
| 楽観ロック | @Version による並行更新検出 |

## テスト観点

### Backend

- [ ] EmployeeService.create: 正常登録 → employeeCode 自動採番
- [ ] EmployeeService.create: メール重複 → 409
- [ ] EmployeeService.create: パスワードが BCrypt ハッシュ化される
- [ ] EmployeeService.update: 正常更新
- [ ] EmployeeService.update: 存在しない ID → 404
- [ ] EmployeeService.update: メール重複（他社員と） → 409
- [ ] EmployeeService.deactivate: active=false に更新
- [ ] EmployeeService.deactivate: 存在しない ID → 404
- [ ] EmployeeService.findAll: active=true のみ返す
- [ ] EmployeeService.findById: 存在する → Employee 返却
- [ ] EmployeeService.findById: 存在しない → 404
- [ ] AdminEmployeeController: ADMIN → 200
- [ ] AdminEmployeeController: EMPLOYEE → 403
- [ ] AdminEmployeeController: 未認証 → 401
- [ ] 楽観ロック: version 不一致 → 409

### Frontend

- [ ] S-6: 社員一覧テーブル表示（コード/氏名/メール/ロール/状態）
- [ ] S-6: 削除ボタン → 確認ダイアログ → 論理削除
- [ ] S-7: 入力バリデーション（氏名必須、メール形式、パスワード8文字以上）
- [ ] S-7: 登録成功 → 一覧へ遷移
- [ ] S-7: メール重複 → エラーメッセージ
- [ ] S-8: 既存値がプリセットされる
- [ ] S-8: 保存成功 → 一覧へ遷移
- [ ] S-8: パスワードフィールドがない（編集時はパスワード変更なし）

## 完了基準

- Backend: EmployeeService + Controller のユニットテスト全パス
- Backend: 統合テスト（API エンドポイント通し）全パス
- Frontend: コンポーネントテスト全パス
- ブラウザで社員登録 → 一覧確認 → 編集 → 削除の一連フローが動作

## 実装順序（TDD）

1. EmployeeService テスト → interface 定義
2. EmployeeService 実装（create — 自動採番 + BCrypt）
3. EmployeeService 実装（update / deactivate / find）
4. AdminEmployeeController テスト → 実装
5. 統合テスト
6. Frontend: S-6 社員管理一覧
7. Frontend: S-7 社員登録フォーム
8. Frontend: S-8 社員編集フォーム
