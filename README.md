# KEMI DeskLink

**KEMI DeskLink** 是面向 KEMI 双屏 Android 设备的开源“文档 + 视频”协同工作台。

主屏（D0）专注文档、表格、演示文稿和 PDF；副屏（D2）专注本地、局域网 NAS 与媒体库视频。两块屏幕不是镜像：文档可引用某段视频的时间点，点击引用会让副屏精确跳转；编辑时，KEMI 跨屏语音输入法把文本提交给主屏编辑器，副屏媒体自动让出音频焦点。

本项目是全新的独立工程：**不依赖、不复制、不要求编译 KOffice**。KEMI 既有项目只作为双屏、生命周期和跨屏输入的设计参考。

## 产品定位

优先面向需要“边看资料、边看视频、边形成记录”的真实生产场景：

- 培训复盘：主屏课程 PPT/PDF，副屏播放 NAS 教学视频。
- 会议纪要：主屏整理结论，副屏回放局域网会议录像，并插入可追溯的时间点引用。
- 售后维修：主屏设备手册和工单，副屏播放故障录像、维修演示或局域网流媒体。

不把它做成两个应用的拼贴，也不以“8K 录制终端”为第一目标。首个 MVP 聚焦可靠的双屏文档编辑、4K 视频解码和语音驱动记录。

## 技术选型

| 能力 | 首选基础 | 选择原因 |
| --- | --- | --- |
| Office/PDF | [ONLYOFFICE Documents Android](https://github.com/ONLYOFFICE/documents-app-android) | Android 原生项目，覆盖离线 Office 文档与 PDF 能力。 |
| 本地/NAS 视频 | [LibVLC / VLC Android](https://github.com/videolan/vlc-android) | 成熟的 Android 媒体播放栈，适合本地文件与 SMB、NFS、UPnP/DLNA 等来源。 |
| 可选媒体库 | [Jellyfin Android](https://github.com/jellyfin/jellyfin-android) | 仅在客户已有 Jellyfin 服务端时启用，绝不成为 NAS 直连的前置条件。 |
| 平台层 | Kotlin + AndroidX + KEMI 平台适配 | 两 Activity、双显示器、输入法状态与音频焦点控制。 |

详细边界见 [架构文档](docs/architecture.md)、[开发计划](docs/development-plan.md) 和 [许可证与依赖](docs/licensing.md)。

## 当前状态

`P1 自动验证已完成`：独立 Kotlin Android 工程、D0/D2 双 Activity 路由、工作区进程恢复、主屏标准输入通道以及 IME/音频协调状态机均已完成。Office 与 LibVLC 尚未接入；实际 KEMI 语音发声仍保留一项人工验收。详见 [P0](docs/validation/p0-device-validation.md) 和 [P1](docs/validation/p1-workspace-voice.md) 真机验证记录。

## 目录规划

```text
kemi-desklink/
├── app/                    # Android 应用入口（后续）
├── core/
│   ├── platform/           # 双屏、生命周期、IME、音频焦点
│   ├── workspace/          # 跨屏会话状态与恢复
│   └── reference/          # 文档 ↔ 视频时间点引用
├── features/
│   ├── office/             # ONLYOFFICE 适配，不耦合媒体
│   └── media/              # LibVLC、文件/NAS/媒体库适配
└── docs/
```

## 非目标

- 不重写或分叉 KEMI 输入法；应用只遵循 Android `InputConnection`。
- 不在首期实现云端协作、账号体系或自建 NAS 服务。
- 不承诺设备本地 8K 编码、8K 录制或多路 4K 转码；这些应在后续压力测试后单独定义。
