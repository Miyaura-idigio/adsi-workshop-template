# 勤怠アプリ — 要求仕様 Q&A

## スコープ・ユーザー

[Question] 対象ユーザーは？（社員のみ？管理者もいる？）
[Answer]どちらもいる

[Question] 想定ユーザー数の規模は？（数十人？数百人？）
[Answer]数十人

## 打刻

[Question] 打刻方法は？（Web画面からボタン押下？位置情報付き？）
[Answer]ボタン押下で位置情報機能はいらない

[Question] 打刻の修正（打刻忘れ等）は可能ですか？誰が修正できますか？
[Answer]修正可能、本人と管理者

## 勤務形態

[Question] 勤務形態は？（固定時間制？フレックス？シフト制？）
[Answer]フレックス

[Question] 休憩時間の扱いは？（自動控除？手動打刻？）
[Answer]自動控除

## 休暇・申請

[Question] 有給休暇・休日申請の機能は必要ですか？
[Answer]必要だけど、あとで実装

## 社員管理

[Question] 社員情報（社員マスタ）の管理機能は必要ですか？それとも既存システム連携？
[Answer]必要

[Question] 社員IDは自動採番ですか？それとも手動入力？
[Answer]自動採番

## 認証・権限

[Question] 認証方式は？（ID/パスワード？SSO？）
[Answer]ID/パスワード

[Question] 権限の種類は？（一般社員 / 管理者 の2種？それ以上？）
[Answer]一般社員と管理者の2種類

## 集計・出力

[Question] 月次の集計・CSV出力は必要ですか？
[Answer]月次の集計のみ

[Question] 残業時間の計算は必要ですか？基準となる所定労働時間は？
[Answer]残業時間の計算は必要、所定労働時間は8時間

## 技術・環境

[Question] 技術スタックの希望は？（Backend: Java/Spring Boot、Frontend: Next.js を想定してよいですか？）
[Answer]FrontendはtypescriptでBackendはJava/Spring Boot

[Question] DBは？（PostgreSQL？H2で開発？）
[Answer]DBはH2で開発
