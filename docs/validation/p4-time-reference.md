# P4 视频时间点引用验证记录

**日期：** 2026-07-29  
**范围：** 可替换的引用核心与 D0/D2 临时编辑器闭环；不宣称 Collabora 文档写入已经完成。

## 已交付

- 新增纯 Kotlin `core:reference` 模块和 `ReferenceService`。
- D2 在已准备媒体上可生成当前毫秒位置的可读 Markdown 引用，例如 `《维修演示.mp4》 0:06`。
- 深链格式为 `kemi-desklink://media/{provider}/{asset}?t={positionMs}`；provider 与 asset 用 URL-safe Base64 路径段编码，避免 `content://`、SMB 路径中的 `/` 破坏解析。
- D2 将引用追加到共享 `WorkspaceSession.draftText` 并持久化；D0 通过仅应用内的动态广播刷新临时编辑器。若 D0 未运行，下次恢复仍从工作区状态加载。
- D2 底部控件重排为三行双列/单列组合，加入引用按钮后仍可在 1920×1280 副屏完整显示。

## 自动化验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 深链往返 | 通过 | `ReferenceServiceTest` 覆盖包含斜杠、空格和中文的 `content://` asset id 往返解析。 |
| 可读引用 | 通过 | 覆盖 Markdown 标题转义、`0:06` 格式化和追加换行规则。 |
| 非法链接 | 通过 | 覆盖非 DeskLink scheme 与损坏/负时间链接拒绝。 |
| 完整 APK | 通过 | `./gradlew :core:reference:test :core:workspace:test :features:media:testDebugUnitTest :app:assembleDebug`，87 个任务完成。 |

## 产品边界与待验收

- 当前 D0 是输入法验证用的 `EditText`，引用采用追加 Markdown 的降级方式；不能当作 DOCX/XLSX/PPTX/PDF 的真实超链接、批注或选区写入。
- Collabora `OfficeAdapter` 可用后，应使用 `selectionVersion` 校验选区，并将同一 `ReferenceService` 输出插入为 Office 引擎原生链接/批注。
- 点击深链后激活 D2 与精确 seek、修复版本地 VOD 断点续播、真实 NAS VOD 引用均需在没有外部应用抢占双屏焦点的窗口做真机验收。
