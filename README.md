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
| Office/PDF | [Collabora Office](https://github.com/CollaboraOnline/online) | 当前 P2 候选；需先在 GitHub Linux Runner 完成原生 engine 构建与真机验证，尚未接入应用。 |
| 本地/NAS 视频 | [LibVLC / VLC Android](https://github.com/videolan/vlc-android) | 成熟的 Android 媒体播放栈，适合本地文件与 SMB、NFS、UPnP/DLNA 等来源。 |
| 可选媒体库 | [Jellyfin Android](https://github.com/jellyfin/jellyfin-android) | 仅在客户已有 Jellyfin 服务端时启用，绝不成为 NAS 直连的前置条件。 |
| 平台层 | Kotlin + AndroidX + KEMI 平台适配 | 两 Activity、双显示器、输入法状态与音频焦点控制。 |

详细边界见 [架构文档](docs/architecture.md)、[开发计划](docs/development-plan.md) 和 [许可证与依赖](docs/licensing.md)。

## 当前状态

`P3c 已完成媒体库基础闭环`：D0/D2 路由、工作区进程恢复、主屏标准输入、IME/音频协调状态机，以及 D2 本地/网络 URI 播放路由均已实现。本地 DocumentsUI `content://` 文件使用 Android 平台播放器；网络 SMB、NFS、UPnP、HTTP(S)、RTSP URI 使用 `LibVLC 3.6.5`，后者已在目标设备真实解码公开 HLS。D2 已具备最近播放、收藏和断点状态持久化。P3 的修复版本地可 seek VOD 真机续播、NAS 浏览、Keystore 凭据、实际 SMB/NFS 样片和长测仍在后续范围。P2 已从失效的 ONLYOFFICE 地址切换到 [Collabora Office 可行性验证](docs/validation/p2-collabora-feasibility.md)：源码已固定并在 GitHub Linux Runner 验证原生配置，尚未进入 DeskLink 集成。详见 [P0](docs/validation/p0-device-validation.md)、[P1](docs/validation/p1-workspace-voice.md)、[P2](docs/validation/p2-office-upstream.md)、[P3a](docs/validation/p3a-local-media.md)、[P3b](docs/validation/p3b-libvlc-network.md) 与 [P3c](docs/validation/p3c-media-library.md) 记录。

## 目录规划

```text
kemi-desklink/
├── app/                    # Android 应用入口（后续）
├── core/
│   ├── platform/           # 双屏、生命周期、IME、音频焦点
│   ├── workspace/          # 跨屏会话状态与恢复
│   └── reference/          # 文档 ↔ 视频时间点引用
├── features/
│   ├── office/             # Collabora Office 适配，不耦合媒体
│   └── media/              # LibVLC、文件/NAS/媒体库适配
└── docs/
```

## 非目标

- 不重写或分叉 KEMI 输入法；应用只遵循 Android `InputConnection`。
- 不在首期实现云端协作、账号体系或自建 NAS 服务。
- 不承诺设备本地 8K 编码、8K 录制或多路 4K 转码；这些应在后续压力测试后单独定义。
