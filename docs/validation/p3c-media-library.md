# P3c 媒体库、收藏与断点续播验证记录

**日期：** 2026-07-29  
**设备：** `192.168.1.10:5555`，Android 12 / API 31，arm64-v8a，D0 + D2（均为 1920×1280）。

## 已交付

- `MediaEngine` 新增读取当前位置、时长和 best-effort seek 的统一接口；LibVLC 与平台播放器均实现该接口。
- `WorkspaceSession` / `WorkspaceRepository` 保存当前媒体的毫秒位置与时长；每 5 秒播放、暂停/后台切换、播放结束时更新。播放完成会清零续播位置。
- 新增纯 Kotlin `MediaHistoryPolicy`：同源去重、最近播放排序、收藏、近开头/近结尾不续播规则，最多保留 30 条最近记录。
- `MediaLibraryRepository` 将媒体来源、位置、时长、收藏状态落入应用私有 SharedPreferences。只保存既有的无凭据 `MediaRef`，不保存 NAS 用户名、密码或 token。
- D2 增加“收藏当前媒体”和“最近播放”入口；最近列表可再次打开媒体，收藏用 `★` 标识。

## 自动化验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 媒体历史策略单测 | 通过 | `MediaHistoryPolicyTest` 覆盖同源去重、收藏保留、起点/终点续播保护。 |
| APK 构建 | 通过 | `./gradlew :core:workspace:test :features:media:testDebugUnitTest :app:assembleDebug`，83 个任务完成。 |
| D2 媒体页 | 通过 | 真机 Display 2 UI 包含“收藏当前媒体”“最近播放”和播放控件。 |
| 收藏持久化 | 通过 | 公开 HLS 条目点击后文案变为“取消收藏”；应用私有媒体库记录为 `isFavorite=true`。 |
| 最近播放 | 通过 | D2 “最近播放”弹窗显示 `★ x36xhzz.m3u8`，可作为媒体入口。 |

## 未宣称通过的项目

- 公开 HLS 流为零长度时间轴，实测位置为 0，不能作为 VOD seek/断点的验收样片。
- 需要一段可 seek 的本地 MP4 或无凭据 NAS VOD，验证“播放超过 5 秒 → 杀进程/重开 → 从非零位置继续”的真实引擎行为；本地 `content://` 会使用平台播放器，网络 VOD 使用 LibVLC。
- SMB/NFS/UPnP 浏览、Keystore 凭据、字幕/音轨切换、断连重连和长时播放仍未完成。
