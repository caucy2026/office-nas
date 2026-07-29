# 开发计划

## 总体策略

先验证风险最高的“独立构建 + 双屏 + 系统输入法”链路，再引入 Office 和媒体引擎。每一阶段可单独演示、可回归，不以堆积依赖代替真机验证。

## P0：工程骨架与技术验证

**目标：** 新建纯 Kotlin Android 工程，在目标 KEMI 设备上稳定打开两个 Activity。

- 建立 Gradle 多模块目录与基础 CI（编译、静态检查、单元测试）。
- `OfficeActivity` 放 D0，使用普通 `EditText` 作为临时编辑器。
- `MediaActivity` 放 D2，使用占位播放界面；确保 `singleInstance` 与重启恢复。
- 实现 D2 错误启动主界面的回流保护。
- 用 KEMI 语音输入法向 D0 的 `EditText` 提交文本，记录无敏感内容的状态日志。

**验收：** 冷启动、Home 返回、旋转/休眠、D2 拔插后恢复均不出现重复副屏窗口；连续 20 次语音提交不丢字、不插入副屏控件。

## P1：工作区状态与跨屏协调

**目标：** 两屏不依靠临时 Intent 参数维持业务状态。

- 定义 `WorkspaceSession`、`MediaRef`、`VoiceSession` 和事件序列。
- 实现 `WorkspaceRepository` 持久化与进程重建恢复。
- 实现语音状态机：播放暂停、释放音频焦点、语音结束后的条件恢复。
- 建立 Display/IME/AudioFocus 的调试日志开关，默认关闭。

**验收：** 杀掉进程后恢复同一文档与同一媒体位置；语音前暂停和语音后恢复只发生一次。

## P2：Office 技术接入验证

> **状态（2026-07-29）：替代上游已选定，等待 Linux 冒烟。** 原定 ONLYOFFICE Android 官方 Git 地址不可访问；已验证 Collabora Office 官方 Android 壳和发布物存在。它要求先构建 LibreOffice 原生 engine，因此 P2 的下一门是 Linux / arm64 构建与 KEMI 真机冒烟，不在当前 Android 工程中盲目接入。详见 [ONLYOFFICE 记录](validation/p2-office-upstream.md) 和 [Collabora 可行性记录](validation/p2-collabora-feasibility.md)。

**目标：** 独立构建并验证 Collabora Office Android 的可集成方式；不把“能拉到源码”误判为“可交付”。

- 固定上游 commit/tag，建立可重复的构建说明。
- 优先验证本地 DOCX、XLSX、PPTX 打开与编辑，PDF 打开、检索与批注。
- 编写 `OfficeAdapter` 接口，禁止上游 UI/API 泄漏到 `core`。
- 验证编辑区与系统 `InputConnection` 的交互，特别是跨屏 KEMI 语音提交。
- 若无法作为库嵌入，评估“受控分叉 + 明确上游同步策略”；不将 APK 间 Intent 当作编辑集成方案。

**验收：** 至少各 3 个 DOCX/XLSX/PPTX/PDF 样例可打开；DOCX/XLSX/PPTX 可保存并由桌面 Office 再次打开；语音结果进入实际编辑区。

## P3：本地与 NAS 媒体接入

> **P3a/P3b 已完成基础闭环：** 已交付独立 `MediaEngine`、平台本地播放兜底、`LibVLC 3.6.5`、D0 无凭据网络 URI 入口，以及 D2 公开 HLS 真机出画。SMB/NFS/UPnP 浏览、Keystore 凭据、实际 NAS 样片和长测仍未完成，详见 [P3a](validation/p3a-local-media.md) 和 [P3b](validation/p3b-libvlc-network.md) 验证记录。

**目标：** 用 LibVLC 验证播放能力与内容源抽象。

- 实现 `MediaProvider`：`local`、`smb` 为首批；NFS/UPnP/DLNA 在接口稳定后增加。
- 集成本地文件浏览、收藏、最近播放与断点续播。
- 接入 LibVLC 的硬解优先策略、字幕/音轨切换和失败降级。
- 建立 NAS 凭据的 Keystore 保存、网络断开重连和脱敏日志。
- 可选实现 Jellyfin Provider，前提是 P3 核心功能已稳定。

**验收：** 本地 1080p、SMB 1080p、SMB 4K 样片均可播放；播放/暂停/拖动后状态能跨 Activity 恢复；网络断开时有可理解的错误提示。

## P4：视频时间点引用

**目标：** 形成真正的“1 + 1”体验，而不仅是并排使用。

- D2 增加“引用当前时刻”操作。
- 实现 `ReferenceService` 和 `kemi-desklink://` URI 解析。
- 优先通过 `OfficeAdapter` 插入可读文字及超链接；能力不足时采用应用内引用面板作为降级。
- 在文档内或引用面板点击后激活 D2、打开媒体并精确 seek。

**验收：** 连续插入 10 条引用均跳转到正确媒体与误差不超过 1 秒的位置；文档在外部 Office 打开时，引用文本仍可读。

## P5：可靠性、性能与发布准备

**目标：** 明确产品可承诺边界并让贡献者能复现构建。

- 真机 30 分钟 4K NAS 播放 + D0 文档编辑 + 多次语音输入压力测试。
- 测试 Home/任务切换、锁屏、Wi-Fi 切换、NAS 断连、D2 热插拔和低内存。
- 建立崩溃上报的本地导出机制（默认不上传）。
- 编写贡献指南、构建前置条件、样例媒体说明与已知限制。
- 对发布包做依赖许可证扫描与对应源码提供流程检查。

**验收：** 已定义性能测试报告；关键状态机有自动化测试；开源发布材料完整且许可证清晰。

## 里程碑交付物

| 里程碑 | 对外演示能力 |
| --- | --- |
| M0（P0） | 两个显示器独立应用 + KEMI 语音写入 D0 |
| M1（P1） | 可恢复的双屏工作区 + 正确音频协调 |
| M2（P2） | Office/PDF 实际编辑与保存 |
| M3（P3） | 本地/SMB 视频播放 |
| M4（P4） | 文档点击跳转副屏视频时间点 |
| M5（P5） | 真机稳定性报告与开源首版 |

## 当前立即行动项

1. 将 `Collabora Android smoke` 推送到 GitHub 后，先用 `preflight` 解析 Gerrit 的实际提交，再以其 40 位 SHA 触发 `assemble`，产出 arm64 APK 并在 KEMI 冒烟。详见 [GitHub Actions 构建记录](validation/p2-github-actions-build.md)。
2. 提供一套不含生产凭据的 SMB/NFS 测试样片，完成 P3 网络协议、断连与重连验收。
3. 将 NAS 凭据接入 Android Keystore；继续禁止其进入 `WorkspaceSession`、日志和 URI。
4. 使用真实 KEMI 语音 IME 做一次“播放 → 语音 → 暂停 → 结束 → 恢复”的人工验收。
