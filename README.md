# qBitRemote

一个 Android 端的 [qBittorrent](https://www.qbittorrent.org/) 远程控制客户端,使用 Kotlin + Jetpack Compose 编写。配置好 qBittorrent 的 WebUI 地址与账号后,即可在手机上查看/添加/管理种子。

> 当前版本:**v1.6.1** · 最低 Android 7.0 (API 24) · 目标 Android 14 (API 34)

---

## 功能

### 服务器
- 多服务器管理:新增 / 编辑 / 删除 / 切换当前服务器,启动时恢复上次选中的服务器。
- 登录测试:保存前验证账号是否可用,失败时给出具体原因(401 / 403 / 网络错误等)。
- **自动重登录**:会话 cookie 过期(后台切回、长时间未操作)时,自动重新登录并恢复,列表不会变空。

### 种子列表(首页)
- 3 秒轮询,实时显示总上传 / 下载速度。
- 搜索、状态筛选(全部 / 下载中 / 已完成)、分类筛选。
- 排序:按名称 / 进度 / 下载速度 / 上传速度 / 比率 / 添加时间,正序或倒序。
- 长按底部弹窗:暂停 / 恢复 / 删除(删除前二次确认)。
- 全部暂停 / 全部恢复。

### 添加种子
- 磁力链 / URL、从剪贴板粘贴、`.torrent` 文件。
- 选择分类、多选已有标签(tags)。

### 种子详情
- 基本信息(名称 / 状态 / 大小 / 种子数 / 分类 / 标签)。
- 当前速度、下载/上传量、比率、做种时间、存储路径。
- 每种子下载 / 上传限速。
- 只读文件列表(每文件进度条,封顶 100 项)与 Tracker 列表。
- 操作:暂停 / 恢复 / 重新校验 / 删除(可选「同时删除本地文件」)。

### 通知
- 后台周期任务(WorkManager,约 15 分钟)检测种子「下载完成」与「出错」状态变化并发通知(需 Android 13+ 授权 `POST_NOTIFICATIONS`)。

### 体验
- SplashScreen(无启动白屏)+ 自适应应用图标。
- 动态取色主题(Android 12+ Material You)。
- 国际化:英文(默认)+ 简体中文(`zh-rCN`),随系统语言切换。

---

## 技术栈

| 层 | 技术 |
|----|------|
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM(ViewModel + StateFlow) |
| 依赖注入 | Hilt |
| 网络 | Retrofit + OkHttp(+ 自定义 cookie / 自动重登录拦截器) |
| 持久化 | DataStore Preferences |
| 后台任务 | WorkManager |
| 构建 | Gradle 8.4 · AGP 8.2.2 · Kotlin 1.9.22 · JDK 17 · compileSdk 34 |

---

## 前置:开启 qBittorrent WebUI

在 qBittorrent 中:`工具 → 选项 → Web UI`,勾选「Web 用户界面」,设置用户名/密码,并记录端口号(默认 8080)。qBitRemote 里填入的「地址」是 qBittorrent 所在机器的 IP(局域网需同一网络或自行打通)。

> 默认情况下 app 允许明文 HTTP。若 qBittorrent 启用了 HTTPS,在服务器配置里勾选「启用 SSL」。

---

## 构建

### 环境要求
- JDK 17
- Android SDK(compileSdk 34)
- Gradle 8.4(仓库未提交 wrapper,本地首次构建需先执行 `gradle wrapper --gradle-version 8.4` 生成 `gradlew`;CI 会自动生成)

### Debug 构建
```sh
gradle wrapper --gradle-version 8.4   # 仅首次需要
./gradlew :app:assembleDebug
```
产物:`app/build/outputs/apk/debug/app-debug.apk`

### Release 签名构建
本地签名:
```sh
./gradlew :app:assembleRelease \
  -Pkey.store=/path/to/release.keystore \
  -Pkey.alias=your_alias \
  -Pkey.store.password=****** \
  -Pkey.key.password=******
```

未提供签名参数时,release 构建不会创建签名配置(可改为用 debug key 调试 release 变体)。

---

## CI 与发布

GitHub Actions(`.github/workflows/android.yml`):
- **push 到 main / PR**:构建 debug APK,上传为 artifact。
- **打 `v*` tag**:用 keystore secret 签名构建 release APK,并自动创建 GitHub Release。

### 配置 release 签名 secret
在仓库 **Settings → Secrets and variables → Actions** 添加:

| Secret | 说明 |
|--------|------|
| `KEYSTORE_BASE64` | keystore 文件的 base64(`base64 -w0 release.keystore`) |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | key 别名 |
| `KEY_PASSWORD` | key 密码 |

### 发版
```sh
git tag -a v1.6.1 -m "v1.6.1: ..."
git push origin v1.6.1
```
CI 跑完后,GitHub Releases 会出现对应版本的签名 APK。

---

## 项目结构

```
app/src/main/java/com/jbcbros/qbitremote/
├── MainActivity.kt              # 入口,SplashScreen + Edge-to-Edge + 权限申请
├── QbApplication.kt             # Hilt + WorkManager 周期任务调度
├── data/
│   ├── api/                     # QbApiService(Retrofit)、QbCookieJar、QbAuthInterceptor(自动重登录)
│   ├── model/                   # ServerConfig / Torrent / TransferInfo / Tracker / TorrentFile ...
│   └── repository/             # QbRepository(DataStore 持久化 + 网络封装)
├── di/AppModule.kt              # Hilt 模块
├── notification/               # NotificationHelper + TorrentCheckWorker
└── ui/
    ├── home/                    # 首页(列表 / 筛选 / 排序 / 暂停恢复全部)
    ├── detail/                  # 种子详情
    ├── upload/                  # 添加种子
    ├── settings/                # 设置(多服务器管理)
    ├── navigation/AppNavHost.kt
    └── theme/                   # Color / Theme / Type
```

---

## 开发计划

详细路线见 [ROADMAP.md](ROADMAP.md):
- **Phase 1(P0)发布门槛** ✅ — 图标、SplashScreen、统一错误处理、删除确认、i18n。
- **Phase 2(P1)高频功能** ✅ — 全部暂停/恢复、排序、多服务器、标签、通知、详情扩展。
- **Phase 3(P2)体验与工程化** — 空错状态细化、Edge-to-Edge、Timber 日志、Pull-to-Refresh(需先升 Compose BOM)。
- **Phase 4(P3)发布加固** — R8/ProGuard 规则、版本号自动化、单元测试。

---

## 已知限制 / 待办

- 未生成商店上架用的 512×512 `ic_launcher_web.png` 及各密度 PNG 图标(目前用矢量自适应图标)。
- 未提交 Gradle wrapper(`gradlew` / `gradle-wrapper.jar`),CI 会自动生成;建议本地生成后提交。
- release 未开启 `isMinifyEnabled` / 资源压缩(Phase 4 计划)。
- `usesCleartextTraffic` 全局开启(为方便连接未启用 HTTPS 的 qBittorrent)。

## License

本项目仅供学习与个人使用。qBittorrent 是其各自所有者的商标。
