# qBitRemote 开发计划

> 计划日期：2026/06/24  
> 当前版本：v1.4.4（已修复 HomeScreen release 编译错误）  
> 技术栈：Kotlin + Jetpack Compose + Material3 + Hilt + Retrofit/OkHttp + DataStore  
> 构建：Gradle 8.4 + JDK 17 + compileSdk 34

---

## 1. 项目现状

已完成核心功能：
- 服务器配置持久化（DataStore）与登录测试
- 首页种子列表实时轮询（3 秒），显示总上传/下载速度
- 搜索、状态筛选（全部/下载中/已完成）、分类筛选
- 长按底部弹窗：暂停 / 恢复 / 删除
- 添加种子：磁力链/URL、剪贴板、`.torrent` 文件、选择分类
- 种子详情页：基本信息、速度、下载/上传量、比率、做种时间、路径，以及暂停/恢复/校验/删除
- 动态主题（Android 12+）+ 中文界面
- GitHub Actions：push/tag 自动构建 debug/release APK，tag 自动创建 Release

---

## 2. 目标与原则

### 总体目标
把当前“可运行”的版本推进到“可稳定发布”的状态：补齐缺失的发布物料、统一错误处理、补充测试，再逐步扩展高频功能。

### 设计原则
1. **先修后扩**：P0 全部完成前不开新大功能。
2. **向后兼容**：服务器配置格式不变；新增字段尽量可空/有默认值。
3. **避免过度工程**：能用 DataStore 不用 Room；能用简单轮询不用 WebSocket。
4. **CI 先行**：每个 PR 必须过 lint + assembleDebug，release tag 必须能签名打包。
5. **Compose 风格统一**：继续使用 Material3，实验性 API 用 `@file:OptIn` 压制，后续随 BOM 升级逐步移除。

---

## 3. 阶段划分

### Phase 1：发布门槛（P0）
**目标：达到可在真机/模拟器稳定运行并打包发布。**

| # | 任务 | 说明 | 改动范围 |
|---|------|------|----------|
| 1.1 | 正式应用图标 + Adaptive Icon | 替换 `mipmap-anydpi-v26` 占位资源，生成 `ic_launcher` / `ic_launcher_round`；Android 12+ 自适应图标 | `app/src/main/res/mipmap-*` |
| 1.2 | SplashScreen API | 使用 `androidx.core:core-splashscreen` 避免启动白屏 | `MainActivity.kt`, `themes.xml`, build.gradle |
| 1.3 | 统一网络错误处理与自动重登录 | Repository 内捕获 401/403/连接错误，自动尝试一次 `login()`，失败通过 StateFlow 暴露错误信息；首页/详情页/上传页统一 Snackbar | `QbRepository.kt`, 各 ViewModel/Screen |
| 1.4 | 删除确认 + 可选删文件 | 首页长按删除前弹 AlertDialog；详情页保留确认并新增“同时删除本地文件”复选框；API `deleteFiles` 动态传参 | `HomeScreen.kt`, `TorrentDetailScreen.kt`, `QbRepository.kt` |
| 1.5 | 真机/模拟器冒烟测试 | 至少覆盖：配置服务器、添加磁链、添加文件、暂停/恢复、删除、筛选/搜索、分类、详情刷新 | 手动测试清单 |
| 1.6 | 补 `gradlew` wrapper | 当前项目根目录缺少 wrapper，CI 每次都要 `gradle wrapper`；应在本地生成并提交 `gradlew` + `gradle/wrapper` | 项目根目录 |

**Phase 1 验收标准：**
- `./gradlew :app:assembleRelease`（无签名或本地 debug key）能成功。
- `./gradlew :app:lintDebug` 无致命错误。
- 真机上主要路径跑通，崩溃率为 0。

---

### Phase 2：高频功能补齐（P1）
**目标：让老用户从 React Native 旧版迁移过来没有明显功能倒退。**

| # | 任务 | 说明 | 改动范围 |
|---|------|------|----------|
| 2.1 | 全部暂停 / 全部恢复 | 首页 TopAppBar 菜单增加“全部暂停/恢复”，调用 `/api/v2/torrents/stop` 和 `/start` 并传入所有 hash | `HomeScreen.kt`, `HomeViewModel.kt`, `QbRepository.kt` |
| 2.2 | 排序功能 | UI 提供按名称、进度、速度、比率、添加时间排序；正/倒序切换；传给 API `sort` + `reverse` | `HomeScreen.kt`, `HomeViewModel.kt` |
| 2.3 | 多服务器管理 | DataStore 保存服务器列表；Settings 页展示列表，支持新增/编辑/切换/删除；启动时恢复上次选中服务器 | `ServerConfig.kt`, `QbRepository.kt`, `SettingsScreen.kt`, `SettingsViewModel.kt` |
| 2.4 | 标签（Tags）支持 | 上传页/详情页显示并选择 qBittorrent tags；同步现有 tag 列表 | `UploadScreen.kt`, `TorrentDetailScreen.kt`, `QbApiService.kt` |
| 2.5 | 下载完成/错误系统通知 | 后台 Service 监听轮询状态，检测到“出错”或“从非完成态变为完成态”时发通知；需申请 `POST_NOTIFICATIONS` | `AndroidManifest.xml`, 新增 NotificationWorker/Service |
| 2.6 | 详情页扩展信息 | 显示 trackers、peers、文件列表（只读）、当前限速 | `TorrentDetailScreen.kt`, `QbApiService.kt`, 新增 Model |

**Phase 2 验收标准：**
- 旧版 README 中列出的功能在新版中全部覆盖。
- 用户可在 Settings 页切换 2+ 服务器。

---

### Phase 3：体验与工程化（P2）
**目标：提升可维护性和国际化。**

| # | 任务 | 说明 | 改动范围 |
|---|------|------|----------|
| 3.1 | 国际化 | 抽出 `values-zh-rCN/strings.xml`（当前默认中文作为 zh-rCN），新增 `values/strings.xml`（英文） | `res/values-*/strings.xml` |
| 3.2 | 空/错状态细化 | 无配置、网络失败、登录失败、服务器不可达、空列表分别给出不同文案与图标 | 各 Screen |
| ~~3.3~~ | ~~Pull-to-Refresh~~ | 已删除：首页已 3 秒自动轮询，下拉刷新边际收益低，不值得为此升级 Compose BOM（风险最高） | — |
| 3.4 | 状态栏/Edge-to-Edge 统一 | 修正 `Theme.kt` 中 `statusBarColor = primary` 与 TopAppBar 的色差；使用 `enableEdgeToEdge` + windowInsets 规范 | `Theme.kt`, `MainActivity.kt` |
| 3.5 | Timber 日志 | 接入 `com.jakewharton.timber:timber`，替换 `e.printStackTrace()`；debug 输出完整，release 关闭 | 全局 |
| 3.6 | 崩溃上报 | 可选接入 Firebase Crashlytics 或开源替代（如 ACRA），先完成基础日志再说 | 后续评估 |

---

### Phase 4：发布加固（P3）
**目标：让 release 包更小、更安全、自动化程度更高。**

| # | 任务 | 说明 | 改动范围 |
|---|------|------|----------|
| 4.1 | ProGuard / R8 规则 | 补齐 `proguard-rules.pro`（Retrofit/Gson/DataStore/Hilt/Compose），开启 `isMinifyEnabled = true` | `app/build.gradle.kts`, `proguard-rules.pro` |
| 4.2 | 版本号自动化 | 使用 `git describe` / tag name 自动生成 `versionName`，commit count 生成 `versionCode` | `app/build.gradle.kts` |
| 4.3 | CI Lint + 测试 | GitHub Actions 增加 `./gradlew :app:lintDebug :app:testDebugUnitTest` | `.github/workflows/android.yml` |
| 4.4 | 单元测试 | 为 `QbRepository` 的 JSON 解析、格式化工具体、ViewModel 状态机写测试 | `app/src/test/java/...` |
| 4.5 | 集成/UI 测试 | 至少覆盖首页空状态、Settings 页保存配置的端到端测试（Espresso/Compose Test） | `app/src/androidTest/java/...` |
| 4.6 | 应用签名与发布流程文档 | 在 README 中说明如何生成 keystore、配置 GitHub Secrets、打 tag | `README.md` |

---

## 4. 风险与依赖

| 风险 | 影响 | 应对 |
|------|------|------|
| Compose BOM 版本较老（2024.02.00） | 部分新 API（如 Pull-to-Refresh）不可用 | Phase 3 再升级，Phase 1/2 避免使用新 API |
| qBittorrent API 版本差异 | 不同版本返回字段可能缺失 | Model 字段全部给默认值，解析失败 gracefully |
| 缺少真机测试环境 | release 前无法验证低端机表现 | 至少使用 Android Emulator 多版本测试 |
| DataStore 多服务器改造复杂 | 可能破坏现有单服务器配置 | 迁移时保留旧 key，新 key 用 `servers` 前缀 |

---

## 5. 推荐下一步

建议立即进入 **Phase 1.1 + 1.3 + 1.6**：
1. 补 Gradle wrapper，让本地能直接 `./gradlew assembleDebug`。
2. 替换正式 launcher icon。
3. 统一网络错误处理与自动重登录。

这三项做完后，release 编译和真机稳定性会有质的提升，之后再进入 Phase 2 功能扩展。

---

## 6. Phase 1（P0）完成情况（2026/06/24）

| 任务 | 状态 | 说明 |
|------|------|------|
| 1.1 应用图标 | ✅ 完成 | 新增品牌矢量前景 `ic_launcher_foreground`（蓝底下载箭头）+ 背景 `ic_launcher_background`；`mipmap-anydpi-v26` 自适应图标 XML 已指向新矢量（含 `monochrome`）；新增 `mipmap-anydpi` 作为 API 24/25 的旧版 fallback。**注意：未生成 PNG 位图**，商店上架需要 512×512 `ic_launcher_web.png`，留待有图像工具链时补充。 |
| 1.2 SplashScreen | ✅ 完成 | 接入 `androidx.core:core-splashscreen:1.0.1`；新增 `Theme.QBitRemote.Starting`（`Theme.SplashScreen` 父主题，`postSplashScreenTheme` 回落到 `Theme.QBitRemote`）；`MainActivity.onCreate` 在 `super.onCreate` 前调用 `installSplashScreen()`；manifest 中 Activity theme 切到 `.Starting`。 |
| 1.3 统一错误处理 + 自动重登录 | ✅ 完成 | 新增 `QbAuthInterceptor`：GET 请求遇到 `403` 时同步执行一次登录并重试（POST 因 body 一次性不可重放，留待下次轮询恢复；登录请求带 `X-Qbit-NoAuth` 头避免递归）。`QbRepository` 暴露 `connectionError: StateFlow<String?>`，`getTorrents` 失败时写入、成功时清空；首页/详情页统一 Snackbar 展示。 |
| 1.4 删除确认 + 删除本地文件 | ✅ 完成 | 首页长按删除前弹 `AlertDialog` 二次确认；详情页删除对话框新增「同时删除本地文件」复选框；`QbRepository.deleteTorrent(hash, deleteFiles)` 动态传参（默认 `true` 保持旧行为）。 |
| 1.5 冒烟测试 | ⏳ 待真机 | 见下方清单，需在真机/模拟器逐项过。 |
| 1.6 Gradle wrapper | ⏳ 受限 | 本机无 Gradle/JDK17，无法生成 `gradle-wrapper.jar`（二进制）。当前 CI 已有「缺失则 `gradle wrapper` 生成」的兜底步骤，构建不受影响；建议本地用 `gradle wrapper --gradle-version 8.4` 生成并提交 `gradlew` + `gradlew.bat` + `gradle/wrapper/gradle-wrapper.jar`。 |

### 6.1 顺带完成的 i18n（对应 Phase 3.1 提前）

- 默认 `values/strings.xml` 改为**英文**；中文迁移到 `values-zh-rCN/strings.xml`。
- ViewModel 中所有硬编码中文改为 `context.getString(R.string.xxx)`（向 4 个 ViewModel 注入 `@ApplicationContext Context`）。
- 修复 `UploadScreen` 用字符串相等判断「添加成功」的脆弱写法：新增 `addSuccess: Boolean` 标志，成功后据此返回上一页。
- 详情页用 `actionSuccess` 标志取代 `actionMessage != "操作失败"` 的脆弱字符串比较。
- 全量 `grep` 确认 `app/src/main/java` 下已无残留 CJK 字面量。

### 6.2 真机冒烟测试清单（Phase 1.5）

1. 启动 → SplashScreen 显示蓝底图标 → 平滑过渡到首页（无白屏）。
2. 首次无配置 → 显示「请先配置服务器」。
3. 设置页填写服务器 → 测试 → 保存 → 首页出现列表。
4. 添加磁链 / 添加 `.torrent` 文件 → 成功返回首页，列表刷新。
5. 长按种子 → 暂停 / 恢复；删除 → 弹确认框 → 确认删除。
6. 详情页：暂停/恢复/校验/删除（勾选/不勾选「删除本地文件」）。
7. 筛选（全部/下载中/已完成）+ 分类下拉 + 搜索。
8. 把 app 切后台几分钟让会话过期 → 切回 → 列表应自动恢复（自动重登录生效，不空列表）。
9. 断网 → 首页 Snackbar 提示「无法连接服务器」；恢复网络 → 自动恢复。
10. 切换系统语言（中/英）→ UI 文案随之变化。

---

## 7. Phase 2（P1）完成情况（2026/06/24）

全部 6 项高频功能已实现并通过 CI（assembleDebug）。

| 任务 | 状态 | 说明 |
|------|------|------|
| 2.1 全部暂停/恢复 | ✅ | 首页溢出菜单「全部暂停/恢复」，调用 `/torrents/stop|start` 传 `hashes=all`。 |
| 2.2 排序 | ✅ | 按名称/进度/下载速度/上传速度/比率/添加时间排序，正/倒序切换；传 `sort`+`reverse`。 |
| 2.3 多服务器 | ✅ | `ServerConfig` 加 `id`；DataStore 以 `servers_json`+`active_server_id` 存储；非破坏性迁移（旧单服务器键折叠为列表，旧键保留备份）。设置页服务器列表：编辑/删除/设为当前/新增。 |
| 2.4 标签 Tags | ✅ | `GET /torrents/tags`；上传页 FlowRow 多选标签（逗号拼接传 `tags`）；详情页展示标签。 |
| 2.5 通知 | ✅ | WorkManager 周期任务（15 分钟）轮询，检测「非完成→完成」与「→出错」状态变化发通知；首跑只播种状态集合避免刷屏。`POST_NOTIFICATIONS` 运行时申请（Android 13+）。 |
| 2.6 详情扩展 | ✅ | 只读文件列表（封顶 100 +「还有 N 个」）含每文件进度条；Tracker 列表（url+节点数）；每种子下载/上传限速行。文件/Tracker 仅进入页时加载一次（不进 3 秒轮询）。 |

### 7.1 待真机验证（CI 只能保证编译）
- 多服务器迁移：升级用户旧的单服务器配置是否被正确迁入列表（旧键保留可回退）。
- 通知：实际下载完成/出错时是否收到通知（Android 13+ 授权后）。
- 性能：大种子（数千文件）详情页渲染、每 3 秒轮询重建 Retrofit 的开销。

### 7.2 下一步建议
进入 **Phase 4（P3）发布加固**：R8/ProGuard 规则、版本号自动化（已完成）、单元测试、CI lint+test。Phase 3 的国际化、空错状态、Edge-to-Edge、Timber 均已完成；3.3 Pull-to-Refresh 已删除（自动轮询已覆盖，不值得为它升级 Compose BOM）。

---

## 8. Phase 4（P3）进度（2026/06/24）

| 任务 | 状态 | 说明 |
|------|------|------|
| 4.1 R8/ProGuard | ⏳ 待定 | 开 `isMinifyEnabled` + keep 规则（Retrofit/Gson/Hilt/Compose/model）。**风险高**：keep 规则写错会导致 release 运行时崩溃（Gson 反射/Hilt 注入），CI 编译验不了，需真机测 minified release。建议先验证 4.4 测试覆盖再开。 |
| 4.2 版本号自动化 | ✅ | `versionCode` = git 提交数，`versionName` = 最新 tag（去 `v` 前缀）。设置页底部展示。 |
| 4.3 CI lint+test | ✅ 部分 | CI 已加 `:app:testDebugUnitTest`；lint 暂未加（避免预存 lint error 噪音，后续按需开）。 |
| 4.4 单元测试 | ✅ | `TorrentParsingTest`（钉死 `tags` 为逗号字符串、缺字段走默认值，守住 v1.6.0 那类回归）、`FormatUtilsTest`。CI 中跑通。 |
| 4.5 集成/UI 测试 | ⏳ | 需 CI 加 emulator（较重），暂缓。 |
| 4.6 签名/发布流程文档 | ✅ | 见 README「CI 与发布」一节（keystore secret、tag 发版）。 |



