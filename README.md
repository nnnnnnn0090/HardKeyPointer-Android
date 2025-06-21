# HardKeyPointer-Android

[![Android](https://img.shields.io/badge/Android-6.0+-brightgreen.svg)](https://android.com)
[![License](https://img.shields.io/badge/License-Custom-blue.svg)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Stable-success.svg)]()

**HardKeyPointer**は、画面上にポインタを表示し、ハードキーを使用してポインタを操作するAndroidアプリです。タッチスクリーンなしでも、ハードキーのみで快適にデバイスを操作できます。（ガラホなど）

HardKeyPointer is an app that displays a pointer on the screen and allows users to control it using hardware keys. It enables smooth device operation with just hardware keys, even without a touchscreen, making it ideal for devices like feature phones.

## 特徴

### ポインタ操作
- 画面上のポインタをハードキーで移動
- タップ機能（短押し/長押し対応）  
- 画面回転対応

### キー設定
- 移動、タップ、スクロール機能のキー割り当て変更可能
- Mode 1 Retro II向けデフォルト設定済み
- デフォルトキー設定：
  - 方向キー: ポインタ移動
  - Enter: タップ
  - 数字キー2,4,5,6: スクロール（上,左,下,右）
  - 音量ダウン: ポインタ表示/非表示

### その他機能
- 4方向スクロール
- 移動速度・加速度調整
- バックグラウンド動作
- 設定自動保存

## 対象ユーザー

- ガラケー風スマートフォン（ガラホ）ユーザー
- 身体的制約でタッチスクリーンの使用が困難な方
- タッチスクリーンが故障したデバイスの利用者
- 物理キーでの操作を好むユーザー

## 技術仕様

- **対応OS**: Android 6.0以上
- **必要権限**: アクセシビリティサービス、システムアラートウィンドウ、フォアグラウンドサービス
- **動作方式**: フォアグラウンドサービス + アクセシビリティサービス
- **開発言語**: Kotlin
- **ビルドシステム**: Gradle (Kotlin DSL)

## 動作確認済みデバイス

- **Mode 1 Retro II** (テスト済み・デフォルト設定最適化済み)
- その他のガラホ・フィーチャーフォン型Androidデバイス

> **Note**: デフォルトのキー割り当ては Mode 1 Retro II の物理キー配置に合わせて最適化されており、インストール後すぐに使用できます。他のデバイスでも設定画面からキー割り当てをカスタマイズ可能です。

## インストールと設定

1. APKファイルをインストール
2. 設定 → ユーザー補助 → HardKeyPointer を有効化
3. アプリを起動してキー割り当てを調整
4. 音量ダウンキーでポインタ表示開始

## 使用方法

1. **ポインタ表示**: 音量ダウンキーでON/OFF切り替え
2. **移動**: 設定したキーでポインタを移動
3. **タップ**: Enterキー（またはカスタムキー）でタップ
4. **スクロール**: 数字キー2,4,5,6でスクロール操作
5. **設定変更**: アプリ画面でキー割り当てや速度を調整

## 開発状況

### 完了済み機能
- ~~ハードキーでのスクロールサポート~~
- ~~画面回転対応~~
- ~~ポインタ加速度機能~~
- ~~カスタムキー割り当て~~
- ~~移動速度調整~~

### 今後の開発予定
- [ ] スクロール速度の詳細調整機能
- [ ] ピンチイン/アウト（ズーム）操作のサポート
- [ ] より多くのデバイスでの動作確認
- [ ] ジェスチャー操作の追加

## ライセンス

このアプリは組み込み（プリインストール）での使用も許可されています。  
詳細は[LICENSE](LICENSE)ファイルをご確認ください。

## 貢献

バグレポートや機能要求は、GitHubのIssuesでお知らせください。  
プルリクエストも歓迎いたします。

## サポート

- **バグレポート**: GitHubのIssuesページ
- **機能要求**: GitHubのIssuesページ
- **その他のお問い合わせ**: プロジェクトページ経由

---

## デモ動画

https://github.com/user-attachments/assets/4d60c6fc-2446-4d48-9427-d80d26312bea

---

#Hardware Keyboard #Hardware Button #Pointer #Cursor #Accessibility #Garakei #Garaho #Android #FeaturePhone
