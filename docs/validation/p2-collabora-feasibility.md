# P2 Collabora Office 可行性验证

**日期：** 2026-07-29  
**结论：** 推荐作为 P2 的下一候选，但它是“完整 Office 宿主/源码分叉”路线，不是可直接加进当前 Gradle 的轻量 AAR。先完成 Linux 原生引擎构建与 KEMI 真机冒烟，再决定正式集成。

## 已核验的事实

| 项目 | 结果 | 证据 |
| --- | --- | --- |
| 官方上游 | 可访问 | `CollaboraOnline/online` 的 `distro/collabora/co-25.04-mobile` 分支可直接读取。固定检查提交：`2e18fe10a0045ac445d32a345ac6f5b7cd5298f6`。 |
| Android 工程 | 存在 | 上游包含 `android/app` 和 `android/lib`；后者提供 `org.libreoffice.androidlib`。 |
| 设备兼容性 | 静态满足 | 上游 Android 配置 `minSdk 26`、`targetSdk 35`，并产出 `arm64-v8a`；目标设备为 Android 12 / API 31、arm64-v8a。 |
| 官方发布物 | 存在 | 官方 F-Droid 索引列出 `com.collabora.libreoffice` 的 arm64 `25.04.9.1`，`minSdk 26`，APK 约 278MB，并给出 SHA-256 和签名指纹。 |
| 原 ONLYOFFICE 地址 | 仍不可用 | 直接 `git ls-remote https://github.com/ONLYOFFICE/documents-app-android.git` 返回 `Repository not found`；搜索页面不能替代 Git 可达性。 |

## 关键工程约束

1. Collabora 的 `android/lib` 依赖由 LibreOffice/Collabora 原生引擎生成的 `.so`、配置和资源；不能只复制 Java/Kotlin 层或当作 Maven AAR 接入。
2. 官方构建说明要求在 **Linux** 上先编译 Android 原生 engine，再以 `--enable-androidapp --with-lo-builddir=... --with-android-abi=arm64-v8a` 构建 Android 壳。当前 macOS 主机不应伪造“源码构建已通过”。当前 Gerrit `main` 的实际配置检查已要求 NDK `>= 27`，因此 CI 固定 Android r27 LTS `27.3.13750724`，不继续沿用页面中的旧 r23 示例。
3. GitHub 镜像用于发现、版本固定和 Android 壳审查；官方说明目前把完整 engine 构建放在 Collabora Gerrit 单仓库。正式构建必须固定 Gerrit 的可复现 commit/镜像，而不是只依赖 GitHub 页面。
4. 官方 F-Droid 元数据显示发布包为 MPL-2.0；原生 engine 与传递依赖仍须在发布前做完整许可证/SBOM 扫描，不能据此直接下最终法律结论。

## 推荐集成形态

```text
Collabora Office Android（固定源码与原生 engine）
  └─ D0：Collabora 的文档编辑 Activity / androidlib
       └─ KEMI DeskLink OfficeAdapter
            ├─ 标准 Android InputConnection（KEMI 语音 IME）
            └─ 文档 ↔ 媒体引用桥接

DeskLink MediaActivity（已有，D2）
  └─ LibVLC：本地 / NAS / HLS
```

不要把 Collabora 作为独立 APK 通过 Intent 与 DeskLink 拼接；这样无法保证编辑焦点、工作区恢复和视频引用。应以其 Android `lib`/宿主代码为基础，把 DeskLink 的 D0、D2 与工作区模块合为一个 APK。

## 下一道必须通过的门

在 Linux 构建节点完成以下冒烟，全部通过才开始写 `OfficeAdapter`。GitHub Linux Runner 的分工、版本固定和触发方式详见 [GitHub Actions 构建记录](p2-github-actions-build.md)：

1. 固定上游提交和 NDK/SDK 版本，产出仅 `arm64-v8a` 的 APK。
2. 安装到 `192.168.1.10:5555`；确认文档编辑 UI 固定在 D0，不创建 D2 Office 窗口。
3. 用无敏感样例验证 DOCX、XLSX、PPTX 和 PDF 的打开；验证至少一个 DOCX 的编辑、保存、重新打开。
4. D0 编辑区聚焦后，使用真实 KEMI 语音 IME 验证文本只回填 D0。
5. 与现有 D2 LibVLC 同时运行，验证语音开启时媒体暂停、结束后条件恢复。

## 当前不做的事

- 不复制任何 Collabora/LibreOffice 源码到 DeskLink。
- 不把未完整下载、未核验 SHA-256 的第三方 APK 安装到测试设备。
- 不宣称 SMB/NAS、PDF 批注或全部 Office 格式已经由 Collabora 在本设备上验收通过。
