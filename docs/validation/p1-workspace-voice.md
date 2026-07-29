# P1 工作区与语音协调验证记录

**日期：** 2026-07-29  
**设备：** `192.168.1.10:5555`，Android 12 / API 31，双显示器 D0、D2。

## 已实现

- `WorkspaceRepository` 使用应用私有 `SharedPreferences` 保存文档草稿、选区版本、媒体引用和播放状态；NAS 账户、密码、令牌均不保存于此。
- 主屏编辑内容每次修改后立即保存，`onPause` 时再进行一次无差别持久化。
- `VoiceSessionReducer` 是无 Android 依赖的状态机：只有原先播放的媒体会在 IME 打开时暂停，并且只有相同语音请求关闭时才恢复。
- `VoiceMediaCoordinator` 位于副屏：监听真实 `WindowInsets.Type.ime()` 可见性，语音期间释放媒体音频焦点，结束后按原始播放状态请求恢复。
- 副屏不创建任何文字输入控件，避免抢走 D0 编辑器焦点；窗口采用 `SOFT_INPUT_ADJUST_NOTHING`，避免语音 IME 触发布局压缩。

## 自动化验证结果

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 状态机单测 | 通过 | 覆盖播放中暂停/恢复、暂停状态不误恢复、无关 requestId 不影响状态。 |
| APK 编译 | 通过 | `:core:workspace:test :app:assembleDebug`。 |
| 文本恢复 | 通过 | 输入 `P1_PERSIST`，回桌面、`am force-stop`、重启后，UI 层级仍显示 `P1_PERSIST` 且编辑框保持焦点。 |
| D2 路由回归 | 通过 | 日志连续出现 `Launching MediaActivity on display=2` 与 `MediaActivity ready on display=2`。 |

## 人工验收项（唯一未自动化项）

1. 在 D0 编辑框聚焦后，用用户实际启用的 KEMI 语音 IME 在 D2 说一句文本。
2. 确认文本只提交到 D0，D2 媒体窗口没有获得可编辑焦点。
3. 在 P3 接入真实 LibVLC 后，确认“播放中 → D2 语音 IME 可见 → 暂停/释放焦点 → IME 关闭 → 恢复”的全链路。

这项需要真实话筒与 KEMI 输入法的交互，ADB `input text` 只能验证 Android 文本提交通道，不能伪造语音识别质量或 KEMI 的跨屏显示行为。

