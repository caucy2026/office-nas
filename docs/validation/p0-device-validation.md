# P0 真机验证记录

**日期：** 2026-07-29  
**设备：** `192.168.1.10:5555`，Android 12 / API 31，双显示器 D0、D2。

## 构建验证

命令：

```bash
./gradlew :core:workspace:test :app:assembleDebug
```

结果：通过。`WorkspaceCoordinatorTest` 通过，Debug APK 成功生成。

首次编译发现 Android Java 编译目标默认为 1.8，而 Kotlin 为 17；已在 `app` 与 `core:platform` 统一设为 Java/Kotlin 17 后通过。该修复属于构建配置，不影响运行时设计。

## 安装与主屏验证

通过：

```bash
adb -s 192.168.1.10:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.1.10:5555 shell am start -n com.kemi.desklink/.app.OfficeActivity
```

真机日志确认：`OfficeActivity ready on display=0`。

首次安装暴露 Manifest Activity 包名少写 `.app` 的问题，已修复并覆盖安装；修复后进程正常存在，无新的崩溃。

## 双屏路由验证

从 D0 的“在副屏打开媒体验证面”按钮进入，真机日志依次确认：

```text
Launching MediaActivity on display=2
MediaActivity ready on display=2
```

ADB 直接启动 `MediaActivity` 被系统拒绝，因为它是 `exported=false` 的内部 Activity。这符合设计，实际用户路径必须由 D0 的 `DisplayRouter` 发起。

## 输入通道验证

在 D0 `EditText` 聚焦后，注入无敏感测试文字 `DeskLink_P0`；UI 自动化层级确认编辑框持有焦点并显示该文字。说明应用侧的标准 Android `InputConnection` 正常可写入，满足 KEMI 跨屏语音 IME 的应用接入前提。

## 尚待人工验证

- 用实际 KEMI 语音 IME 在 D2 发声输入，确认结果回填 D0。
- 观察语音 IME 打开/关闭时 D2 的真实 Insets 状态。
- P1 完成后验证：媒体播放 → 语音 → 音频焦点释放 → 条件恢复播放。

