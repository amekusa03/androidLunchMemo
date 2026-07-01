# Lunch Memo 🍱

Lunch Memo は、日々のランチの予定や記録をシンプルかつスタイリッシュに管理できる Android アプリです。
モダンな Material 3 デザインを採用し、スムーズな操作感でランチタイムを楽しく彩ります。

## 主な機能 🌟

- **スマートな記録**: カレンダー形式のカードをスワイプして、ランチの内容を素早くメモ。
- **お昼の通知**: 毎日12:00に、その日のランチメモを通知。今日何食べるんだっけ？を解消します。
- **モダンなデザイン**: 
  - Material 3 フル対応。
  - スムーズなカードアニメーションとグラデーション背景。
  - Android 13 以上の通知権限に完全対応。
- **クリーンなデータ保持**: 過去のメモは自動的に整理され、アプリを常に軽量に保ちます。

## 技術スタック 🛠️

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Database**: Room (SQLite)
- **Background Tasks**: WorkManager
- **Asynchronous**: Coroutines & Flow
- **Architecture**: MVVM
- **Dependency Management**: Gradle Version Catalog (libs.versions.toml)
- **Annotation Processing**: KSP (Kotlin Symbol Processing)

## セットアップ 🚀

1. Android Studio (Ladybug 以降推奨) でプロジェクトを開きます。
2. プロジェクトを Gradle Sync します。
3. Android デバイスまたはエミュレータで実行します。

## 更新履歴 📅

### [Lunch Memo 2.0] - 2026.06.24
- **選択式入力の導入**: 自由入力に加え、あらかじめ登録したアイテムをタップして選択できる機能を追加。
- **3コンポーネント構成**: メモを「選択」「数字」「文字」の3つの要素に分割。用途に合わせて自由にカスタマイズ可能に。
- **設定画面の強化**: コンポーネントごとの入力モード切替や選択肢の編集機能を実装。
- **内部改善**: データベースのマイグレーション(v4)と、設定保存のためのシリアライズ処理(GSON)を導入。

## ライセンス 📝

このプロジェクトは MIT ライセンスの下で公開されています。
