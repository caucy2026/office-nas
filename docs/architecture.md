# 架构设计

## 1. 设计原则

1. **两个可独立恢复的 Activity。** D0 与 D2 分别承载 Office 和媒体，不用一个 Activity 硬拼两块显示器。
2. **状态唯一。** `WorkspaceCoordinator` 是文档、媒体、焦点和语音会话的唯一真相；两个 Activity 均可被系统回收后恢复。
3. **系统输入法优先。** KEMI 跨屏语音输入法是文本提交通道，应用不重复接入 ASR。
4. **文件不做私有格式改写。** 文档引用通过 Office 引擎支持的超链接/批注/扩展元数据接口写入；不得直接解压修改 `.docx` ZIP。
5. **协议可插拔。** 本地、SMB/NFS、UPnP/DLNA 是媒体来源适配器，Jellyfin 是可选适配器。

## 2. 双屏拓扑

```text
┌──────────────────── D0：主屏 ────────────────────┐
│ OfficeActivity                                    │
│  Collabora Office adapter（P2 验证中）              │
│  编辑焦点 / Android InputConnection               │
└───────────────────────┬──────────────────────────┘
                        │ WorkspaceSession
                        │ documentUri, selectionVersion,
                        │ mediaId, positionMs, voiceRequestId
┌───────────────────────┴──────────────────────────┐
│ MediaActivity（D2，singleInstance）                │
│  MediaDock → LibVLC → Local / SMB / NFS / UPnP    │
│  播放、选源、暂停恢复、时间点引用                   │
└──────────────────── D2：副屏 ────────────────────┘

              KEMI 系统语音 IME
     显示于与编辑器相对的屏幕，commitText → D0 编辑器
```

### 显示器和启动策略

- `OfficeActivity` 是主任务入口，优先放在 Display 0（D0）。
- `MediaActivity` 固定在 Display 2（D2），配置 `singleInstance`，防止多媒体窗口重复。
- 主入口被错误地从 D2 启动时，平台层负责重定向到 D0；D2 只恢复已有媒体会话。
- 两 Activity 都只保存可重建 UI 状态；持久会话在 `WorkspaceRepository` 中恢复。
- 不使用 `Presentation` 承载可操作的副屏工作流，因为它不适合独立任务、输入和恢复。

## 3. 模块职责

| 模块 | 责任 | 不应承担 |
| --- | --- | --- |
| `app` | 依赖装配、导航入口、权限声明 | 业务状态判断 |
| `core:platform` | Display 路由、Activity 恢复、IME 观测、AudioFocus | Office/播放器具体 API |
| `core:workspace` | `WorkspaceSession`、持久化、跨屏事件 | 渲染 UI |
| `core:reference` | 视频时间点引用、解析、跳转协议 | 直接编辑 Office 文件压缩包 |
| `features:office` | 文档打开、编辑器桥接、选区及链接插入 | NAS 浏览和播放器控制 |
| `features:media` | `MediaEngine` 播放/seek 接口；P3b/P3c 的 LibVLC 适配器处理本地与网络 URI，平台播放器仅作本地兜底 | 文档编辑实现、NAS 凭据持久化 |

## 4. 核心状态模型

```kotlin
data class WorkspaceSession(
    val sessionId: String,
    val documentUri: String?,
    val documentTitle: String?,
    val selectionVersion: Long,
    val media: MediaRef?,
    val playback: PlaybackState,
    val mediaPositionMs: Long,
    val mediaDurationMs: Long,
    val mediaWasPlayingBeforeVoice: Boolean,
    val voice: VoiceSession?
)

data class MediaRef(
    val provider: String, // local, smb, nfs, upnp, jellyfin
    val assetId: String,
    val displayName: String,
    val uri: String
)

data class VoiceSession(
    val requestId: String,
    val targetDisplayId: Int,
    val targetSelectionVersion: Long,
    val state: VoiceState
)
```

`selectionVersion` 防止发生这种错误：用户启动语音后又移动了文档光标，旧的语音结果仍插入到错误位置。若版本不一致，平台层不进行自动补写，交由编辑器或用户确认。

## 5. 语音与音频状态机

```text
MEDIA_PLAYING
  └─ 编辑器请求 KEMI 语音 → VOICE_PREPARING
       └─ 媒体 pause + abandonAudioFocus → VOICE_LISTENING
            └─ IME commitText 到 D0 → VOICE_FINISHED
                 └─ 若原先播放：requestAudioFocus + resume → MEDIA_PLAYING
                 └─ 否则 → MEDIA_PAUSED
```

- D2 的媒体控件不得创建可编辑的 `InputConnection`，避免抢走 D0 文档焦点。
- 以系统 IME 的真实可见性/提交事件为准，不能只依据“请求语音”的应用内部布尔值。
- 视频在语音期间暂停而非只降低音量，防止麦克风回声和语音识别污染。

## 6. 文档与视频联动

引用统一表示为：

```text
kemi-desklink://media/{provider}/{assetId}?t={positionMs}
```

显示给用户的文案为“《维修演示.mp4》 00:12:43”。插入过程：

1. D2 读取当前媒体 ID 和播放位置。
2. `ReferenceService` 生成稳定 URI 与显示文案。
3. `OfficeAdapter` 用引擎提供的插入超链接/批注能力写到当前选区。
4. 用户点击链接时，D2 激活同一 `MediaRef` 并 seek 到 `t`。

若 Office 引擎第一期没有可靠的写入扩展 API，MVP 只实现“复制引用文本”和“从应用内引用面板跳转”；不要用破坏文档兼容性的临时写法。

## 7. 媒体能力与性能边界

经已调试的目标设备配置，两个物理显示器均以 `1920×1280@60Hz` 工作；标准 H.264/H.265 解码应以 4K 播放为压力测试上限，而不是在产品中承诺端侧 8K 编解码。首期组合目标：

- D0 文档渲染与编辑；D2 一路本地或 NAS 1080p/4K 解码。
- 语音时停止播放，语音结束后恢复。
- 4K、高码率 NAS、字幕切换、休眠/拔插屏幕都必须做真机长测。
- 录像和转码留给 NAS/NVR/SRS 等服务端；客户端先做观看、引用和工作记录。

当前 P3b/P3c 已固定 `org.videolan.android:libvlc-all:3.6.5`，并用 LibVLC 自带 `VLCVideoLayout` 在 D2 真机输出了公开 HLS 视频帧。D0 只接受不带 `userInfo` 的 `smb`、`nfs`、`upnp`、`http(s)`、`rtsp` URI，避免把 NAS 用户名或密码落入 `WorkspaceRepository`；`MediaLibraryRepository` 只记录无凭据媒体来源、播放位置和收藏。`MediaEngine` 仍是明确边界：下一步增加 SMB/NFS/UPnP 浏览、Keystore 凭据 Provider 和可 seek VOD 断点真机验收；D2 生命周期、语音协调和工作区模型不需要重写。

## 8. 安全与隐私

- NAS 密码通过 Android Keystore 加密存储，日志不得包含 URL 中的用户名、密码、令牌或完整私有路径。
- 默认不上传文档、视频索引、语音文本。
- Jellyfin 等服务端令牌仅在用户主动配置后保存在本机，并提供“清除账户与缓存”。
