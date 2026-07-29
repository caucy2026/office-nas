# P2 Office 上游可用性验证记录

**日期：** 2026-07-29

## 结论

P2 暂时**不进入代码集成**。原选定的 ONLYOFFICE Android 上游 `ONLYOFFICE/documents-app-android` 在本次验证时不可取得：GitHub 页面与官方源码归档均返回 HTTP 404。搜索引擎可见的项目介绍是旧缓存，不能作为构建或发布依赖的证据。

## 已执行检查

| 检查 | 结果 |
| --- | --- |
| Git 上游引用查询 | 返回 `Repository not found`。 |
| GitHub master 源码归档下载 | 返回 HTTP 404。 |
| GitHub 页面直接打开 | 返回 HTTP 404。 |
| 本机 ONLYOFFICE Android 源码副本 | 未找到。 |

因此，没有将任何 Office 上游代码、二进制或未经固定版本的依赖加入 DeskLink。

## 解除条件

任选其一后才能恢复 P2：

1. 提供仍可访问的 ONLYOFFICE Android 官方归档、明确 commit 或可审计 fork。
2. 指定一个可公开获取、许可兼容且可离线编辑 DOCX/XLSX/PPTX/PDF 的替代 Android 上游。

恢复后必须按 P2 原计划在隔离目录构建、安装、验证 KEMI `InputConnection`，再决定是否进入 `features:office`。不能以网页缓存、APK 逆向或手工复制文件代替该步骤。

