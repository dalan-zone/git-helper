# VersionedApp — Android Gradle 模板（Version Catalog + CI 自动升号）

一套可直接复用的 Android 工程骨架，落地上一条回答里的最佳实践：
**versionName 给人看、versionCode 只增不减且交给 CI 自动计算**，依赖版本全部收口到 Version Catalog，多环境用 productFlavors 驱动。

## 目录结构

```
git-helper/
├── settings.gradle.kts          # 仓库 / 插件仓库 + 模块声明
├── build.gradle.kts             # 仅声明插件（alias 指向 catalog）
├── gradle.properties            # JVM / Android 开关 + VERSION_NAME
├── version.properties           # CI 维护的单调 versionCode（受 git 跟踪）
├── gradle/
│   ├── libs.versions.toml       # ★ 单一版本真相源（依赖 + 插件）
│   └── wrapper/gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts         # ★ 自动 versionCode + 环境 flavor + 签名
│   ├── proguard-rules.pro
│   └── src/main/...             # Manifest / Activity / 资源
└── .github/workflows/release.yml# ★ 自动升号并打 AAB
```

## 版本模型

| 字段 | 来源 | 谁改 | 示例 |
|------|------|------|------|
| `versionName` | `gradle.properties` 的 `VERSION_NAME` | 人（发版时） | `1.2.0` |
| `versionCode` | 自动推导（见下） | **CI / 脚本** | `37` |

`versionCode` 推导优先级（见 `app/build.gradle.kts` 的 `computeVersionCode()`）：
1. CI 注入的环境变量 `VERSION_CODE`
2. `version.properties` 里的 `VERSION_CODE`（本地兜底，CI 也会回写）
3. git commit 数 `git rev-list --count HEAD`（保证本地每次构建都拿到唯一号）

> 规则：发版时 **只改 versionName**，versionCode 永远单调递增，永不回退。

## 本地使用

```bash
# 1) 生成 wrapper（本机没装 gradle 时；或直接在 Android Studio 打开）
gradle wrapper

# 2) 构建某个环境/构建类型的包
./gradlew :app:assembleDevDebug        # dev + debug
./gradlew :app:bundleProdRelease        # prod + release AAB（上架用）

# 3) 查看所有变体
./gradlew :app:tasks --group build
```

Flavor 维度 `environment` 提供三个变体：`dev` / `staging` / `prod`，
分别通过 `buildConfigField("String","BASE_URL", ...)` 注入不同 API 地址，
并自动追加 `applicationIdSuffix` 与 `versionNameSuffix`，可同机共存。

## 签名（release）

- 本地：把 `release.keystore` 放到 `app/release.keystore`，在 `~/.gradle/gradle.properties`
  里配置 `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`。
- CI：注入环境变量 `KEYSTORE_FILE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`
  （见 `release.yml` 中已注释的解码步骤）。
- 上架 Google Play 新应用须提交 **AAB**，并建议开启 **Play App Signing**。

## CI 自动发版

触发方式二选一：
- **打 tag**：`git tag v1.2.0 && git push --tags` → `versionName` 取 `1.2.0`，自动 `versionCode +1`。
- **手动**：在 Actions 页面 `workflow_dispatch` 填入 `versionName`。

流程：检出 → 升 `versionCode` → 用 `gradle.properties` 的 `VERSION_NAME`（或 tag）构建
`prodRelease` AAB → 上传产物 → 把升过的 `versionCode` 提交回仓库（保证号码持续递增）。
`release.yml` 还附带了上传 Google Play 内部轨道的注释步骤，按需取消注释并配置
`PLAY_SERVICE_ACCOUNT_JSON` 即可。

## 自定义清单

- 改包名：改 `app/build.gradle.kts` 的 `namespace` / `applicationId`，以及 `MainActivity` 的 `package`。
- 加依赖：只在 `gradle/libs.versions.toml` 加 `[versions]` 与 `[libraries]`，build 脚本里用 `libs.xxx` 引用。
- 多渠道：再加一个 `flavorDimension`（如 `channel`）并用 `flavorDimensions += listOf("environment","channel")`。
