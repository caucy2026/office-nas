# P3b LibVLC 网络媒体闭环验证记录

**日期：** 2026-07-29  
**设备：** `192.168.1.10:5555`，Android 12 / API 31，arm64-v8a，D0 + D2（均为 1920×1280）。

## 已交付

- 固定依赖 `org.videolan.android:libvlc-all:3.6.5`；Debug APK 只打包 `arm64-v8a`，避免无关 ABI 使设备端安装包膨胀。
- `LibVlcMediaEngine` 使用硬解优先、网络缓存和 LibVLC 自带 `VLCVideoLayout`。初版手写 `SurfaceView` 在本机可解码但黑屏；已改为 `MediaPlayer.attachViews(...)`，由 LibVLC 管理视频输出面和布局更新。
- D0 增加“从主屏打开 NAS / 局域网媒体 URI”。接受 `smb`、`nfs`、`upnp`、`http`、`https`、`rtsp`；拒绝不支持的协议、缺失主机、任何 `用户名:密码@` URI、敏感 query（如 `token`、`signature`、`credential`）及 fragment。
- 只将无凭据 URI、来源协议、显示名保存到工作区和 P4 引用；NAS 浏览、账户和凭据存储尚未实现，后续必须使用 Android Keystore。

## 自动化验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| URI 解析单测 | 通过 | `:features:media:testDebugUnitTest` 覆盖 SMB 正常路径、HTTP(S) 显示名、非法协议、userinfo、token/signature query、fragment 拒绝和普通非敏感 query 保留。 |
| APK 构建 | 通过 | `./gradlew :features:media:testDebugUnitTest :app:assembleDebug`，共 80 个任务。 |
| D0 → D2 路由 | 通过 | 真机日志：`Launching MediaActivity on display=2`、`MediaActivity ready on display=2`。 |
| HLS 真机出画 | 通过 | D0 输入公开测试地址 `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`，D2 状态显示 `PLAYING`，Display 2 截图确认视频帧已显示。 |
| 解码证据 | 通过 | LibVLC 日志记录 HLS TS 解复用、音频轨和 `1920x1080` 解码输出；设备媒体服务记录视频正在播放。 |
| 播放状态回写 | 通过 | D2 控件日志依次记录 `PAUSED → PLAYING → PAUSED`；测试设备最终停在暂停状态。 |

## 范围与后续验收

- 已验证的是公开 HTTP HLS 的 D0 选源、D2 路由、LibVLC 解码和画面输出闭环；尚未声称 SMB/NFS/UPnP 服务器实播通过。
- 使用真实 KEMI 语音 IME 的“播放中暂停、语音结束恢复”仍为人工验收项；状态机和播放器控制入口已由 P1/P3b 接通。
- P3 完整验收前，需用无生产凭据的 SMB 1080p/4K、NFS/UPnP 样片测试播放、断连、重连、长时间运行、字幕/音轨与休眠/热插拔恢复。
