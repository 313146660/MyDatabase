# MyDatabase

一个面向眼镜店/视光工作室的**客户档案 + 镜片库存**管理工具。先做网页原型确认交互，再 1:1 移植到 Android（Jetpack Compose），数据完全保存在本机，不依赖任何后端。

> 仓库里只有代码和虚构的演示数据，不包含任何真实客户信息。

## 功能

**客户档案**

- 16 列固定 CSV 可直接导入/导出：`姓名,电话,用途,OD,OD轴向,OS,OS轴向,瞳距,镜框,镜片,合计,时间,NO.,ADD,备注,客户`
- 列表自动换行，可切换显示字段、按数字调字号
- 排序：最近验光 / 最近修改 / 最早验光 / 名称 A→Z / 名称 Z→A（按库记住）
- 搜索支持区间：`-3.00~-1.00`、`1.56～1.61`、`2025/01~2025/12`；带搜索历史
- 收藏、家庭组（同电话不同名）、字段管理、公式字段
- 详情页一键复制电话；顶部摘要卡折叠全部字段
- 验光历史：与上次对比的度数增减、首尾度数标注、等效球镜趋势图（近视更负/远视更正都向上）

**镜片库存**

- 型号 × 球镜 × 柱镜的矩阵表格，点格子直接改库存
- 选镜片扣库存、改选自动回补；库存留空 = 未入库不提醒，`0` = 缺货提醒
- 按型号/球镜范围/有无散光批量改库存，支持撤销和自定义预设
- 型号 chip 长按批量改名；低库存红色标记
- 补货清单按型号生成纯文本（单光 4 片、带散光 2 片，数量可改），一键复制或保存 `.txt`

**数据与外观**

- 本地 JSON 备份 + WebDAV 云备份（手动/自动），恢复前自动留快照可撤销
- 深色模式、全局列表字号
- 所有数据只存在设备本地（SharedPreferences / `localStorage`），无统计、无遥测

## 仓库结构

```
.
├─ memento-prototype/   零依赖网页原型（HTML + CSS + 原生 JS）
│  ├─ index.html        入口，双击即可运行
│  ├─ app.js            全部逻辑
│  └─ styles.css        设计系统与布局
└─ android/             Android App（Jetpack Compose，Kotlin）
   ├─ app/              界面、数据层、备份、CSV
   └─ smoketest/        数据层 JVM 自测入口
```

## 网页原型

无需 npm、无需构建：

```bash
# 直接用浏览器打开
memento-prototype/index.html
```

首次打开会写入 5 个演示库到 `localStorage`。数据只存在当前浏览器，清站点数据会丢失；重要数据请用页面里的「备份与恢复」导出。

## Android 构建

环境：JDK 17、Android SDK 34、Gradle 8.7（仓库带 `gradlew`，也可用本机 Gradle 8.7）。

```bash
cd android
./gradlew :app:assembleDebug       # Windows: gradlew.bat :app:assembleDebug
```

产物：`android/app/build/outputs/apk/debug/app-debug.apk`。

首次构建前在 `android/local.properties` 写 SDK 路径（该文件已被 `.gitignore` 忽略）：

```properties
sdk.dir=/path/to/Android/Sdk
```

`minSdk 26`、`targetSdk 34`，使用 debug 签名；正式发布请自行配置 release keystore。

## 数据与隐私

- **不要提交**：真实备份 JSON、`客户.csv`、`local.properties`、签名文件。`.gitignore` 已覆盖常见情况。
- 备份格式为 JSON，`version: 2 / schemaVersion: 2`，同时兼容旧版 v1 与裸 state。
- 网页原型里的 8 位手机号、姓名均为虚构演示数据（`138000000xx` / 陈晓明等），仅用于展示家庭组和历史记录。

## 已知限制

- `applicationId` 仍是 `com.example.mementoclone`，属于早期占位；改名会导致已装应用的数据隔离，需要先导出备份再迁移。
- 没有崩溃上报、没有多设备同步；WebDAV 是唯一的内置云备份方式。
- UI 以中文为主，代码注释为中文。

## License

[MIT](LICENSE)

## 声明

本项目是独立实现，与 Memento Database 及其开发商无关。早期内部名称中出现过 Memento，目录名、主题名、类名、UI 显示文字均已替换；仅包名 `com.example.mementoclone` 与备份文件名 `memento-backup.json` 为升级兼容保留的字符串，不涉及商标授权，且 `applicationId` 改名会导致已装应用数据隔离，迁移前请先导出备份。
