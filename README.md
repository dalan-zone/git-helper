# git-helper — Android Gradle 模板（Version Catalog + CI 自动升号）

一套可直接复用的 Android 工程骨架，落地以下最佳实践：
**versionName 给人看、versionCode 只增不减且交给 CI 自动计算**，依赖版本全部收口到 Version Catalog，多环境用 productFlavors 驱动。

本仓库同时是**分支 / 版本 / 发布规范的唯一真相源**——跨项目复用的规则见根目录 [`BRANCHING.md`](./BRANCHING.md)，发布逻辑集中在可复用工作流 `android-release.yml`，新项目直接套用即可。

## 目录结构

```
git-helper/
├── settings.gradle.kts          # 仓库 / 插件仓库 + 模块声明
├── build.gradle.kts             # 仅声明插件（alias 指向 catalog）
├── gradle.properties            # JVM / Android 开关 + VERSION_NAME
├── version.properties           # CI 维护的单调 versionCode（受 git 跟踪）
├── BRANCHING.md                # ★ 分支 / 版本 / 发布 统一规范（全项目通用）
├── gradle/
│   ├── libs.versions.toml       # ★ 单一版本真相源（依赖 + 插件）
│   └── wrapper/gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts         # ★ 自动 versionCode + 环境 flavor + 签名
│   ├── proguard-rules.pro
│   └── src/main/...             # Manifest / Activity / 资源
└── .github/workflows/
    ├── ci.yml                  # ★ PR / 主干校验（lint + 单测 + 冒烟，不碰签名）
    ├── release.yml             # ★ 薄入口：tag 触发发布
    └── android-release.yml     # ★ 可复用工作流：构建 / 签名 / 打包 / 回写 versionCode
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

## CI 与自动发版

### 合入前校验（`ci.yml`）

- 触发：`pull_request` 到 `main`，以及直接 `push` 到 `main`。
- 只跑 `lintDebug` + `testDebugUnitTest` + `assembleDebug` 冒烟，**不碰签名/打包**（debug 不需要 keystore）。
- 这是 PR 上绿/红状态的来源，也是 main 分支保护里"要求状态检查通过"所卡的检查项（job 名 `verify`）。
- 发布工作流回写 `main` 的提交带 `[skip ci]`，会被本工作流跳过，不会死循环。

### 自动发版（`release.yml` → `android-release.yml`）

触发方式二选一：
- **打 tag**：`git tag v1.2.0 && git push origin v1.2.0` → `versionName` 取 `1.2.0`，自动 `versionCode +1`。
- **手动**：在 Actions 页面 `workflow_dispatch` 填入 `versionName`。

流程：`release.yml`（薄入口）只负责"什么时候发"，真正的构建 / 签名 / 打包 / 回写逻辑在可复用的
`android-release.yml` 里——检出 → 升 `versionCode` → 用 `gradle.properties` 的 `VERSION_NAME`（或 tag）构建
`prodRelease` AAB → 上传产物 → 把升过的 `versionCode` 提交回仓库（保证号码持续递增，该提交带 `[skip ci]`）。
`android-release.yml` 还附带了上传 Google Play 内部轨道的注释步骤，按需取消注释并配置
`PLAY_SERVICE_ACCOUNT_JSON` 即可；其他项目通过跨仓库 `uses: dalan-zone/git-helper/.github/workflows/android-release.yml@main` 直接复用，差异仅一行。

## 分支与一行命令

本仓库遵循 [`BRANCHING.md`](./BRANCHING.md) 的统一规范（主干 `main` + 短命 `feature/*` + 按需 `release/*`、`hotfix/*`，标签即发布）。
已写入全局 git alias，日常只用一行：

```bash
git feat dark-mode     # 从 main 切出 feature/dark-mode
git publish            # 推分支开 PR（等 ci.yml 变绿）
git sync               # 快进同步 main
git release 1.3.0 "深色模式上线"   # 基于干净 main 打 v1.3.0 并触发发布
git hotfix 1.2.1 v1.2.0           # 从 v1.2.0 切出 hotfix/1.2.1
```

> 说明文字、分支保护勾选清单、alias 完整定义见 [`BRANCHING.md`](./BRANCHING.md)。

## 自定义清单

- 改包名：改 `app/build.gradle.kts` 的 `namespace` / `applicationId`，以及 `MainActivity` 的 `package`。
- 加依赖：只在 `gradle/libs.versions.toml` 加 `[versions]` 与 `[libraries]`，build 脚本里用 `libs.xxx` 引用。
- 多渠道：再加一个 `flavorDimension`（如 `channel`）并用 `flavorDimensions += listOf("environment","channel")`。
