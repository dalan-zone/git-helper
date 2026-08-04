# GIT-FLOW.md — 无 alias 原生命令流程

本文件是 [`BRANCHING.md`](./BRANCHING.md) 中分支 / 版本 / 发布规范的**原生命令版本**（不使用全局 git alias），适合不想依赖 alias、或在全新环境 / CI 中复现时使用。

> **核心约定（贯穿全文）**：所有"同步 main"统一用 `git pull --ff-only`（= `git fetch` + `git merge --ff-only`），**不要**写裸的 `git merge --ff-only origin/main`——后者不会联网，依赖本地可能过期的 `origin/main` 快照，无法保证拉到远端最新。

---

## 一、创建分支

```bash
# 功能 / 修复 / 发版分支：先回到干净且最新的 main，再切出
git switch main
git pull --ff-only                 # 联网拉最新 main（关键）
git switch -c feature/dark-mode    # 功能
git switch -c fix/null-crash       # 修复
git switch -c release/1.3.0        # 发版稳定分支

# 热修分支：从出问题的 tag 切出（基线不是 main）
git fetch --tags
git switch -c hotfix/1.2.1 v1.2.0
```

## 二、日常开发 + 推 PR + 同步

```bash
# 提交（必须遵循 Conventional Commits：<type>(<scope>): <subject>）
git add .
git commit -m "feat(dark-mode): 支持夜间主题切换"

# 推到远端并开 PR（等价于 git publish）
git push -u origin HEAD

# 随时同步 main（等价于 git sync，已修正为联网拉取）
git switch main
git pull --ff-only                 # fetch + ff-merge，确保是远端最新
git switch feature/dark-mode
git rebase main                    # 可选：把 main 新提交 rebase 进来，保持线性
```

PR 在 GitHub 创建后，`ci.yml` 自动跑 lint + 单测 + 冒烟。

## 三、合并回 main（PR 变绿后）—— 两种路线，别混

### 路线 A：PR 在 GitHub 上点 Merge（最常见）
GitHub 已把分支合进远端 main，**不要在本地再 merge、也不要再 push main**：

```bash
git switch main
git pull --ff-only                 # 把 GitHub 上已合并的 main 拉回来
# 此时 feature/dark-mode 已在 main 里，到此即可
git branch -D feature/dark-mode                    # 删本地（见下方说明）
git push origin --delete feature/dark-mode         # 删远端
```

> 若 GitHub 勾了"合并后自动删分支"，最后一行可省。

### 路线 B：本地合并，不走 GitHub 的 Merge 按钮

```bash
git switch main
git pull --ff-only                 # 先同步远端最新 main
git merge feature/dark-mode        # 本地合并
git push origin main               # 推回远端
git branch -D feature/dark-mode
git push origin --delete feature/dark-mode
```

只有这条路才需要 `git merge feature/dark-mode`。

### ⚠️ `git branch -d` 可能拒删（squash 合并坑）
GitHub 默认 **"Squash and merge"** 会把分支压成一个新 commit，导致 `feature/dark-mode` 不是 main 的祖先，`git branch -d` 报 "not fully merged"。处理：
- 确认 PR 已合并 → 直接 `git branch -D`（强删）；
- 或先 `git branch --merged main` 查看是否已列为合并，再决定。
（用 "Rebase and merge" / "Create a merge commit" 则 `git branch -d` 正常。）

## 四、发布（打 tag 触发，等价于 `git release`）

发布不是"在分支上点按钮"，而是基于**干净且最新**的 main 打 annotated tag：

```bash
# 0) 有未提交改动先暂存（保证基于干净 main 发版）
git stash push -u -m "release-wip-1.3.0"

# 1) 回到最新 main
git switch main
git pull --ff-only

# 2) 打 tag 并推送 → 触发 release.yml → android-release.yml 自动构建签名打包
git tag -a v1.3.0 -m "v1.3.0: 深色模式上线，修复启动崩溃"
git push origin v1.3.0

# 3) 回到原分支并恢复暂存
git switch feature/dark-mode
git stash pop
```

> 仅当本地改了 `gradle.properties` 的 `VERSION_NAME` 时，记得先 commit 再打 tag。`versionCode` 永远交给 CI 自动升，别手改。

## 五、热修（等价于 `git hotfix`）

```bash
git fetch --tags
git switch -c hotfix/1.2.1 v1.2.0     # 从问题 tag 切出
# 修复代码
git add .
git commit -m "fix: 修复启动崩溃 (#42)"

# 立即发布
git tag -a v1.2.1 -m "v1.2.1: 修复启动崩溃"
git push origin v1.2.1                # 触发发布

# 合回主干（本地合并路线）
git switch main
git pull --ff-only
git merge hotfix/1.2.1
git push origin main
git branch -D hotfix/1.2.1
git push origin --delete hotfix/1.2.1
```

## 六、查看与管理

```bash
git branch -a                                   # 所有本地+远端分支
git tag -l                                      # 所有 tag
git log --oneline --graph --all                 # 分支拓扑
git branch --merged main                        # 看哪些已合进 main
git branch -D <名>                              # 强删（squash 合并后常用）
git push origin --delete <名>                   # 删远端分支
```

---

## 对照表（alias ↔ 原生，已修正）

| 动作 | alias（一行） | 原生命令核心 |
|------|--------------|-------------|
| 开功能分支 | `git feat xxx` | `git switch main && git pull --ff-only && git switch -c feature/xxx` |
| 推送开 PR | `git publish` | `git push -u origin HEAD` |
| 同步 main | `git sync` | `git switch main && git pull --ff-only`（= fetch + ff-merge）|
| 合回 main | （GitHub 合并） | `git switch main && git pull --ff-only` → 删分支 |
| 合回 main | （本地合并） | `git switch main && git pull --ff-only && git merge feature/xxx && git push origin main` |
| 发版 | `git release x.y.z "msg"` | `git switch main && git pull --ff-only && git tag -a vx.y.z -m "..." && git push origin vx.y.z` |
| 热修 | `git hotfix x.y.z base` | `git fetch --tags && git switch -c hotfix/x.y.z <base-tag>` |
