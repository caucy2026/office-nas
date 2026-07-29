# P2 GitHub Actions Linux 构建

**状态：** 已配置，尚未推送到 GitHub 或执行。  
**目的：** 把 Collabora 的 Linux 原生 engine 构建放到临时 GitHub Linux Runner；Mac 只负责 DeskLink 开发和 KEMI 真机安装/验收。

## 工作流分工

| 工作流 | 触发 | 用途 | 资源策略 |
| --- | --- | --- | --- |
| `Android verify` | PR、`main` 推送、手动 | 编译和测试当前 DeskLink 模块 | 最多 20 分钟；Gradle 缓存；会取消同一分支过期任务。 |
| `Collabora Android smoke` | 手动；受控 `collabora-preflight-*` 标签 | 从 Collabora Gerrit 构建其独立 Android 候选 APK | 最多 6 小时；只构建 `arm64-v8a`；手动完整构建串行且不会被取消。 |

这两个任务故意不互相阻塞：DeskLink 每次提交持续得到快速回归，而耗时的 Office 构建只在上游验证节点触发。

后续的 `collabora-preflight-*` 标签会取消仍在运行的旧预检，避免重复占用 Runner；从页面手动启动的 `assemble` 永远不会被预检标签取消。

## 第一次执行：低成本预检

在 GitHub 仓库的 **Actions → Collabora Android smoke → Run workflow** 中选择：

```text
source_ref = refs/heads/main
mode       = preflight
```

该任务会在 Gerrit 单仓库中检出源码，确认 `engine/` 和 `android/` 都存在，安装官方要求的 Android NDK `23.0.7599858`，运行 engine 配置，并在日志、Actions 摘要和产物 `collabora-build-metadata.txt` 中记录实际 `source_sha`。预检没有调用 `make`，避免在网络/依赖/路径不通时浪费数小时。

`sdkmanager --licenses` 的输入管道会单独保存 `sdkmanager` 的退出状态；`yes` 在对端正常关闭后产生的 SIGPIPE 不会被误判成 SDK 安装失败。

Gerrit 源码只浅抓取请求的一个 ref，并设置 20 分钟连接/传输上限；预检不下载 monorepo 的完整历史。失败时保留配置与元数据，先修复连接或 ref，再开始完整构建。

若 native 配置失败，工作流会将 `engine/config.log` 中与错误相关的脱敏片段及末尾写入 Job Summary，同时保留完整日志 Artifact；无需把 GitHub Token 发到聊天中即可定位依赖问题。

同一错误的首要单行会作为 GitHub Check Annotation 发布，便于只具 Git SSH 权限的构建机通过公开 Checks API 读取；不需给上游构建任务 `contents: write` 权限。

如果构建机只有 Git SSH 权限、没有 GitHub API Token，也可显式推送名为 `collabora-preflight-*` 的标签；它只会以 `refs/heads/main` 执行同样的预检。普通 `main` 提交不会触发它。这个标签入口只适用于预检，完整构建仍必须通过手动工作流输入固定 SHA。

如果 Gerrit 的 `main` 路径变化，请在下一次预检中填写其实际 ref；不要退回只含 Android 壳的 GitHub 镜像，也不要为了方便固定一个未在 Gerrit 验证过的 GitHub commit。

## 第二次执行：固定版本构建

预检成功后，复制其 `source_sha`，再次手动执行：

```text
source_ref = <预检产物中的 40 位 source_sha>
mode       = assemble
```

此时才会依次执行 engine `make`、顶层 Android 配置/构建和 `android/gradlew build`。生成的 APK、SHA-256、源码/NDK 元数据及关键配置日志会保留 14 天。只取 `out/` 中完整 APK，并在 Mac 上校验 SHA-256 后用 ADB 安装；绝不安装中断下载或未核验的 APK。

## Runner 的边界

- 工作流会在隔离的 GitHub Runner 内回收预装的 Android/.NET/Haskell/Boost 工具链磁盘，再重新安装固定 NDK；它不会触碰 Mac 或 KEMI。
- 完整 LibreOffice engine 构建可能超过标准 Runner 的磁盘、内存或 6 小时上限。若失败，先下载日志判定瓶颈；只有确认是 Runner 资源不足，才考虑 GitHub larger runner。不要在未通过预检前购买或启用更大 Runner。
- 引擎构建和 APK 产出只是 P2 的第一个门。成功后仍须由 Mac 在 `192.168.1.10:5555` 上完成 DOCX/XLSX/PPTX/PDF、保存回读、KEMI 语音输入和 D2 视频协调的真机验收。

## 发布前约束

- `Collabora Android smoke` 只验证上游独立候选，不会自动把它塞进 DeskLink APK。
- 每次重跑 `assemble` 必须使用 40 位 commit SHA，而不是移动分支。
- APK 发布前仍需做许可证/SBOM 扫描，遵循 [许可证记录](../licensing.md)；CI 的构建成功不等于完成开源合规验收。
