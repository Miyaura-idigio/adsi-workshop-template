# Unit 1: 勤怠ドメイン（打刻・履歴・集計）

## 実装状況: 未着手

Unit 0 で以下の基盤が実装済み:

- [x] `AttendanceRecord` Entity
- [x] `AttendanceRecordRepository`（findByEmployeeIdAndDate, findByEmployeeIdAndDateBetween）
- [x] `WorkDuration` Value Object + テスト（休憩控除・残業計算）
- [x] `V2__create_attendance_records.sql` マイグレーション

本 Unit で実装するもの: Service, Controller, DTO, Frontend 全般

## 概要

出退勤打刻、勤怠履歴の閲覧、月次集計、打刻修正を実装する。
アプリケーションのコア業務機能。

## 依存

- **Unit 0（共通基盤）** — Entity, Repository, 認証, WorkDuration を使用（実装済み）

## 独立性

- Unit 2（社員管理）とは互いに依存しない → **並行実装可能**（Unit 2 は実装済み）

## 担当範囲

### Backend

| レイヤー | 成果物 |
|---------|--------|
| Service | AttendanceService (interface + impl) |
| Controller | AttendanceController（一般社員向け） |
| Controller | AdminAttendanceController（管理者向け） |
| DTO | AttendanceRecordResponse, UpdateRecordRequest, MonthlySummaryResponse |

### Frontend

| レイヤー | 成果物 |
|---------|--------|
| 画面 | S-2 打刻（ホーム）, S-3 勤怠履歴, S-4 打刻修正, S-5 社員勤怠一覧 |
| hooks | useAttendance, useMonthlySummary |
| API | attendance API クライアント関数 |

### API エンドポイント

| メソッド | パス | 説明 | ロール |
|---------|------|------|--------|
| POST | /api/attendance/clock-in | 出勤打刻 | 全員 |
| POST | /api/attendance/clock-out | 退勤打刻 | 全員 |
| GET | /api/attendance/today | 本日の打刻状態 | 全員 |
| GET | /api/attendance/records?yearMonth= | 月次勤怠一覧（自分） | 全員 |
| GET | /api/attendance/summary?yearMonth= | 月次集計（自分） | 全員 |
| PUT | /api/attendance/records/{id} | 打刻修正 | 本人 or MANAGER(部下) or ADMIN |
| GET | /api/admin/attendance/records | 社員勤怠一覧 | MANAGER, ADMIN |
| GET | /api/admin/attendance/summary | 社員月次集計 | MANAGER, ADMIN |

### 画面

| # | 画面名 | パス | 対象ロール |
|---|--------|------|-----------|
| S-2 | 打刻（ホーム） | / | 全員 |
| S-3 | 勤怠履歴（自分） | /attendance | 全員 |
| S-4 | 打刻修正 | /attendance/edit/:id | 全員 |
| S-5 | 社員勤怠一覧 | /admin/attendance | MANAGER, ADMIN |

## 機能

| # | 機能名 | 説明 |
|---|--------|------|
| F-1 | 出勤打刻 | 当日未打刻時に現在時刻を出勤として記録 |
| F-2 | 退勤打刻 | 出勤済み・未退勤時に現在時刻を退勤として記録 |
| F-3 | 打刻修正（本人） | 自分の過去の打刻を編集 |
| F-4 | 打刻修正（管理者） | 任意の社員の打刻を編集 |
| F-5 | 月次勤怠一覧表示 | 指定月の日別勤怠を一覧表示 |
| F-6 | 月次集計表示 | 月の合計勤務時間・残業時間を算出表示 |
| F-7 | 休憩時間自動控除 | 6h超→45分、8h超→60分を自動控除 |
| F-8 | 残業時間計算 | 実勤務時間 − 8h（下限0） |

## ビジネスルール

### 打刻制約

| 操作 | 事前条件 | 違反時 |
|------|---------|--------|
| 出勤打刻 | 当日レコードなし | 409 Conflict |
| 退勤打刻 | 当日出勤済み & 未退勤 | 409 Conflict |
| 打刻修正 | 本人 or MANAGER(部下) or ADMIN | 403 Forbidden |

### 勤務時間計算

```
休憩控除 = 総勤務 > 8h → 60分 / 総勤務 > 6h → 45分 / else → 0分
実勤務時間 = 総勤務時間 − 休憩控除
残業時間 = max(0, 実勤務時間 − 480分)
```

## テスト観点

### Backend

- [ ] AttendanceService.clockIn: 未打刻 → 出勤記録作成
- [ ] AttendanceService.clockIn: 打刻済み → 409
- [ ] AttendanceService.clockOut: 出勤済み・未退勤 → 退勤記録
- [ ] AttendanceService.clockOut: 未出勤 → 409
- [ ] AttendanceService.clockOut: 退勤済み → 409
- [ ] AttendanceService.updateRecord: 本人 → 成功
- [ ] AttendanceService.updateRecord: ADMIN → 成功
- [ ] AttendanceService.updateRecord: 他人（非管理者） → 403
- [ ] AttendanceService.getMonthlyRecords: 指定月の一覧取得
- [ ] AttendanceService.getMonthlySummary: 集計計算の正確性
- [ ] AttendanceController: 各エンドポイントの HTTP ステータス
- [ ] AdminAttendanceController: MANAGER は部下のみ閲覧可
- [ ] AdminAttendanceController: ADMIN は全社員閲覧可
- [ ] AdminAttendanceController: EMPLOYEE はアクセス不可（403）

### Frontend

- [ ] S-2: 未出勤 → 出勤ボタンのみ活性
- [ ] S-2: 出勤済み → 退勤ボタンのみ活性
- [ ] S-2: 退勤済み → 両ボタン非活性、時刻表示
- [ ] S-2: 出勤ボタン押下 → API 呼び出し → 状態更新
- [ ] S-2: 退勤ボタン押下 → API 呼び出し → 状態更新
- [ ] S-3: 年月選択 → 該当月の勤怠一覧表示
- [ ] S-3: 月次集計エリアに合計値が表示
- [ ] S-4: 打刻修正フォームの入力・送信
- [ ] S-4: バリデーション（出勤 < 退勤）
- [ ] S-5: 社員選択 → 該当社員の勤怠表示

## 完了基準

- Backend: AttendanceService + Controller のユニットテスト全パス
- Backend: 統合テスト（API エンドポイント通し）全パス
- Frontend: コンポーネントテスト全パス
- ブラウザで打刻 → 履歴確認 → 集計確認の一連フローが動作

## 実装順序（TDD）

> Entity / Repository / WorkDuration / マイグレーションは Unit 0 で実装済み。

1. AttendanceService テスト → interface 定義
2. AttendanceService 実装（clockIn / clockOut）
3. AttendanceService 実装（getMonthlyRecords / getMonthlySummary）
4. AttendanceService 実装（updateRecord + 権限チェック）
5. AttendanceController テスト → 実装
6. AdminAttendanceController テスト → 実装
7. 統合テスト
8. Frontend: S-2 打刻（ホーム）
9. Frontend: S-3 勤怠履歴 + 月次集計
10. Frontend: S-4 打刻修正
11. Frontend: S-5 社員勤怠一覧（管理者）

## 既存コード参照

| 成果物 | パス |
|--------|------|
| Entity | `backend/src/main/java/.../entity/AttendanceRecord.java` |
| Repository | `backend/src/main/java/.../repository/AttendanceRecordRepository.java` |
| Value Object | `backend/src/main/java/.../domain/WorkDuration.java` |
| VО テスト | `backend/src/test/java/.../domain/WorkDurationTest.java` |
| Migration | `backend/src/main/resources/db/migration/V2__create_attendance_records.sql` |
