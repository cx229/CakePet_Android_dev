# 页面、对话框、抽屉与菜单布局索引

> 第一阶段记录现有所有权，不创建新弹窗。页面、普通 Dialog、底部 Sheet、PopupWindow 功能与生命周期不同，只统一**共同视觉参数**，不能强制共享同一 View。

| 容器 | 现有入口 | 业务注意事项 |
|---|---|---|
| 完整设置页 | `activity_threshold_settings.xml`、`BaseThresholdActivity.kt`、`UserHabitsActivity.kt` | 统一返回/副标题/滚动/模块；用户习惯存在单项应用与重置 |
| 全局设置页 | `activity_global_settings.xml`、`GlobalSettingsActivity.kt` | 含全局入口、权限状态、去水印设置 |
| 个性化设置页 | `PersonalizationSettingsActivity.kt` 复用 `activity_threshold_settings.xml` | 动态拼装两个模块；不是新造一个独立颜色系统 |
| 素材编辑页 | `activity_role_pack_editor.xml`、`RolePackEditorActivity.kt` | 功能/动画/图片/碎碎念页面共享导航逻辑 |
| 普通确认对话框 | `DialogExt.kt: showConfirmDialog()`、`dialog_confirm.xml` | 标题/正文/备注/确认取消/危险操作；尊重异步执行与取消语义 |
| 通用编辑弹窗外壳 | `dialog_content_shell.xml`、`RolePackEditorActivity.kt: editor()` | 多种属性编辑和关联查看，共用标题、关闭、操作栏 |
| 资源选择弹窗外壳 | `RolePackEditorActivity.kt: pickerShell()` | 基于 `dialog_content_shell.xml`，列表内的资源外观参考 [surfaces-and-cards](surfaces-and-cards.md) |
| 名称编辑 | `DialogExt.kt: showRenameDialog()`、`dialog_rename_role.xml`、`FormInputField.kt` | 含角色名/简称，不要自动合并素材包名编辑 |
| 底部抽屉 | `sheet_menu.xml`、`sheet_actions.xml`、`sheet_fish.xml`、`sheet_freeze.xml`、`sheet_interact.xml` 及更多 `sheet_*.xml`，绑定于 `PetMenu.kt/FishMenu.kt/FreezeMenu.kt` 等 | 需保持触发位置、手势、按钮点击区域、关闭逻辑 |
| 三点弹出菜单 | `UnifiedOptionMenu.kt`、`PopupMenuHelper.kt`、`popup_*.xml` | 同为浮出菜单但选项生成方式不同，不要默默替换导致溢出/位置问题 |

## 公共规则

1. 页面级背景通常用 `cp_bg`，卡片通常用 `cp_card_bg`；主题意义参考 [theme-and-text](theme-and-text.md)。
2. 弹窗首选现有带标题、关闭 X、可滚动内容和底部取消/确认区域；只有已有业务确实需要时再自建 Dialog。
3. 操作按钮区必须维持明确的取消/确认优先级和危险操作提示；不可将选项勾选回调当成最终提交。
4. 底部 Sheet 不等于普通 Dialog：定位、系统安全区域和操作按钮排布不同。
5. 三点菜单必须以实际被点击的 View 作为锚点；禁用项和危险项要保留原有语义。
6. 关联素材只读弹窗与可编辑弹窗共用壳时，**权限与变更确认**逻辑保持不变。
7. 主题切换后应重建/刷新所需的资源色与预览辅助层，不要求重启 APP 才看见变化。
8. 文案区分页面标题、设置标题、备注、提示、错误；不应在普通标题/选项文字后附加“（默认）”。

## 待统一但不能在第一阶段直接修改的点

- 多个完整页面的状态栏占位与顶部布局存在近似实现；先建立 PageShell 变体清单。
- 资源编辑器的 `editor()`、`pickerShell()` 目前私有且嵌在超大 Activity 中，未来可以提取为通用外壳构建器。
- `dialog_confirm.xml` 与 `dialog_content_shell.xml` 使用相似按钮语义，先审查交互和视觉差异，再抽取按钮样式。
- `sheet_*.xml` 多套目录与选项布局需要逐一对照，不以一个大 Search/Replace 统一。
