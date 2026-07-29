# P3a 本地媒体闭环验证记录

**日期：** 2026-07-29  
**设备：** `192.168.1.10:5555`，Android 12 / API 31，D0 + D2。

## 交付内容

- 新增 `features:media`，定义 `MediaEngine` 播放边界。
- `PlatformMediaEngine` 以 Android `MediaPlayer` 和 D2 `SurfaceView` 播放用户通过系统文件选择器授予的本地 `content://` 视频。
- 已选择 URI 通过持久化权限恢复；URI 不可读或播放器报错时，应用清除失效引用，不在下次启动循环重试。
- D2 提供“选择本地视频”和“播放/暂停”入口；播放状态继续经过 `WorkspaceCoordinator`，可被语音状态机暂停/恢复。

## 自动化验证结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 模块/单测/APK 编译 | 通过 | `:core:workspace:test :app:assembleDebug` 成功，包含 `features:media`。 |
| 副屏媒体 UI | 通过 | Display 2 系统截图显示 P3a 标题、黑色视频 Surface、选择和播放按钮。 |
| D0 → D2 路由 | 通过 | 新进程日志依次出现 `OfficeActivity ready on display=0`、`Launching MediaActivity on display=2`、`MediaActivity ready on display=2`。 |
| D2 文件选择 Intent | 通过 | 系统日志确认 `com.android.documentsui/.picker.PickActivity` 由 D2 `MediaActivity` 启动。 |
| 失效 URI 清理 | 通过 | 修复后最新 DeskLink 进程未再出现先前的 `setDataSource` 失败循环。 |

## 待输入样片的真机检查

本轮未读取用户的私人媒体文件，也未提供公开样片；因此“选定真实 H.264/H.265 1080p/4K 文件后的画面、音频、拖动和长时间播放”保留为下一次带样片验收。应用入口、权限与播放器回调已在真机运行。

## 2026-07-29：本地 `content://` 引擎路由修正

一次后续真机 VOD 验收发现：LibVLC 可以接受系统文件选择器给出的 `content://` URI 并进入“已准备”状态，但实际开始播放后会报告错误并清理媒体。该 URI 是 Android 文档提供器授予的能力，不应假设 LibVLC 能以 URL 方式稳定消费。

修正后：

- `local` `MediaRef` 在加载前明确选择 `PlatformMediaEngine`，通过 `MediaPlayer.setDataSource(Context, Uri)` 保持 Android 的授权语义；SMB/NFS/UPnP/HTTP(S)/RTSP 等网络 URI 保持 LibVLC 路径。
- 切换引擎时同步重建 `VLCVideoLayout` 或 `SurfaceView`，不复用另一引擎的输出 Surface。
- 通过 `OpenableColumns.DISPLAY_NAME` 读取系统文件名，避免 DocumentsUI 的 `msf:962` 一类内部 document id 出现在用户界面和最近播放列表。
- `MediaPlaybackEnginePolicyTest` 已覆盖本地/网络路由，且完整 Gradle 测试与 APK 构建通过。

**尚未宣称通过：** 修复版在测试机上的人工 VOD 续播复测尚未完成。测试期间 `com.carriez.flutter_hbb` / `com.huanglong.portui` 会抢占 D0/D2 焦点，自动化无法稳定保持 DeskLink 的主屏入口；这属于测试环境干扰，不能替代本机 H.264 真实播放验证。

## P3b 前置条件

官方 VLC Android 资料确认 LibVLC 是用于嵌入的引擎，并可覆盖 SMB、FTP、SFTP、NFS、UPnP/DLNA 等协议；但本机构建缓存没有 LibVLC，Maven Central 查询在当前网络条件下无法完成。待获得可复现 AAR/仓库后，实现 `LibVlcMediaEngine` 和各 NAS Provider，保留当前 `MediaEngine` API。
