# 开源许可证与第三方依赖策略

## 项目许可证方向

KEMI DeskLink 计划以 **AGPL-3.0-or-later** 发布，以便在直接集成 AGPL Office 组件时保持许可证一致性。正式发布前必须由项目维护者完成一次许可证核验；本文件是工程策略，不构成法律意见。

## 拟接入依赖

| 依赖 | 角色 | 许可证风险与处理 |
| --- | --- | --- |
| ONLYOFFICE Documents Android | Office/PDF 编辑能力来源 | 上游采用 AGPL 体系。若复制、修改或链接其代码，DeskLink 的发布、对应源码提供和修改说明必须满足 AGPL 要求。固定 commit 并保留 Notices。 |
| LibVLC `org.videolan.android:libvlc-all:3.6.5` | 当前媒体播放与网络 URI 解码 | 本工程仅声明该 AAR；其 Maven POM 标注 LGPL-2.1。VLC Android 整体应用的 GPL 与此 AAR 不能混同。正式发布前仍须对 AAR 内 native `.so` 与传递依赖生成精确清单并完成许可证核验。 |
| Jellyfin Android | 可选媒体库 Provider | 仅在启用模块时纳入 Notices 与许可证审查；不应把其品牌、服务条款或服务端要求误写成 DeskLink 的基础能力。 |

## 必须执行的发布检查

1. 记录每个上游依赖的 URL、commit/tag、许可证文件与本地修改。
2. 发布 APK 的同时提供与该 APK 对应的完整源码、构建脚本和修改说明。
3. 保留第三方版权声明、NOTICE、商标使用要求与许可证文本。
4. 对 native `.so`、Gradle 传递依赖和媒体编解码插件运行许可证扫描，生成 `THIRD_PARTY_NOTICES.md`。
5. 不使用 ONLYOFFICE、VLC、Jellyfin 商标作为本项目名称或暗示官方背书。

## 上游同步原则

- 所有上游改动用可审查的 Git patch 或独立 fork 保存，禁止手工复制未记录代码。
- `features:office` 和 `features:media` 以适配接口隔离上游差异。
- 升级上游版本必须重新跑构建、设备回归、许可证清单与 Notices 检查。
