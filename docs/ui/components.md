# UI 组件复用目录（现有代码）

本页只记录已核对的真实代码路径与 API。标为“候选”的提取建议**尚未实现**。基线参见 [README](README.md)。

## 基础输入、交互、文字

| 想做什么 | 首选现存实现 | 关键位置/说明 |
|---|---|---|
| 设置页开关 | `res/values/themes.xml: Widget.CakePet.Switch`、`res/color/switch_black_selector.xml` 与 `res/color-night/switch_black_selector.xml` | `item_switch_bound.xml`、`PersonalizationSettingsActivity.kt` 有使用方式；勾选不等于禁用 |
| 单数值滑块 | `SliderHelper.kt: Slider.applyBlackStyle()/onUserValueChange()/EditSync` | 设置页参考 `MovementSettingsActivity.kt`；不要拷贝新的回调防抖 |
| 阈值三数值输入（Min/Default/Max） | `res/layout/item_slider_bound.xml`、`BaseThresholdActivity.kt: sliderItem()` | 这里的 Default 是输入字段含义，不要在标题增加“（默认）” |
| 阈值四数值输入 | `res/layout/item_dual_bound.xml`、`BaseThresholdActivity.kt: dualItem()` | Min/Default A/Default B/Max；以已有端点和校验为准 |
| 开关阈值输入 | `res/layout/item_switch_bound.xml`、`BaseThresholdActivity.kt: switchItem()` | 复用现有用户习惯和阈值风格 |
| 普通下拉 | `SettingsSpinnerBinder.kt: bind(spinner, entries, selected, onSelected)` | 包含箭头/弹层/选项分隔及日夜间颜色；不要用原生未着色 Spinner 替代 |
| 浮动标签文本框 | `FormInputField.kt: configure()`、`res/values/styles.xml: CakePetFormInput` | 表单标签、占位、错误显示；不要简单复制 EditText 的背景 |
| 可折叠开关分组 | `SettingsFoldController.kt` | 它处理 UI 展开及设置联动；不要误绑定独立角色状态 |
| 卡片点击水波纹 | `SettingsRippleRow.kt`、`res/drawable/settings_row_ripple.xml` | 素材编辑器目前另有私有 `rippleCard()`，待收敛 |
| 设置项文字级别 | `res/values/styles.xml` | `SettingsOptionTitle`、`SettingsOptionNote`、`SettingsOptionSub`、`SettingsModuleTitle`、`SettingsModuleNote`、`SettingsOptionValue` |
| 操作入口行 | `SettingsRippleRow` + `SettingsSwitchTexts` + 右箭头 `ic_chevron_right` | 参考 `activity_global_settings.xml: row_user_habits/row_personalization_settings` |
| 确认/取消/危险操作 | `DialogExt.kt: showConfirmDialog()`、`res/layout/dialog_confirm.xml` | 现成的正文、子提示、正反按钮、危险语义；不能直接用 Android 默认对话框替代 |
| 输入名称/简称 | `DialogExt.kt: showRenameDialog()`、`res/layout/dialog_rename_role.xml` | 角色重命名场景；素材名称另有专门对话框 |
| 三点选项菜单 | `UnifiedOptionMenu.kt: show(anchor,options,danger,onSelect)` | 动态选项、危险项目、锚点定位 |
| XML 定义的菜单 | `PopupMenuHelper.kt` + `popup_*.xml` | 适用于已有菜单布局；迁移前先确认选项与 anchor |
| 编辑器内部动作按钮 | `RolePackEditorActivity.kt: button()/dialogButton()/actionPair()` | 目前私有，跨页面使用前应提取，不要复制函数体 |

## 业务卡片与预览

| 类型 | 实现入口 | 不可抹掉的差异 |
|---|---|---|
| 首页角色 | `res/layout/item_role_card.xml`、`MainActivity.kt: HomeAdapter` | 召唤/归位/召回、配置入口、状态、角色预览 |
| 素材包目录 | `res/layout/item_role_catalog.xml`、`RoleManageActivity.kt: RoleAdapter` | 作者、实例数量、素材级操作 |
| 已加入角色管理 | `res/layout/item_role_home.xml`、`RoleHomeManageActivity.kt` | 显示名、简称、排序、管理菜单 |
| 功能资源 | `RolePackEditorActivity.kt: functionItem()/functionTree()` | 支持功能说明、示例帧、绑定动画与缺失提示；文件夹同方法生成 |
| 动画列表 | `RolePackEditorActivity.kt: modes()` | 动画组、帧数、循环/后续链、缩略图 |
| 动画逐帧 | `RolePackEditorActivity.kt` 的帧列表 RecyclerView（`onCreateViewHolder` 附近） | 可拖动排序、多选、时长、xy/dxy、资源路径 |
| 图片资源 | `RolePackEditorActivity.kt: resourceBranch()` | 文件夹、图片缩略图、引用数量、路径 |
| 碎碎念资源 | `RolePackEditorActivity.kt: renderThinkingGroups()` | 碎碎念分组、多对多归组、引用、文本编辑 |
| 角色自然预览 | `PreviewPlayer.kt` | 支持 start/stop、静态帧、摸摸头、首页已召唤占位；不能替代动画帧编辑 |
| 动画编辑预览 | `RoleAnimationPreview.kt` 与 `RolePackEditorActivity.kt: animate()` | 画布/锚点/辅助线、帧镜像和 HUD，需保持素材原始像素 |

## 实际调用示例（不引入新 API）

```kotlin
// 下拉：沿用当前项目公共绑定器
SettingsSpinnerBinder.bind(spinner, listOf("常驻", "动态"), selectedIndex) { index ->
    // 业务层自行处理 index 与持久化
}

// 危险操作：沿用全局确认弹窗
showConfirmDialog(
    title = "确认删除？",
    bodyHtml = "删除后无法恢复。",
    positiveText = "删除"
) { /* 现有业务动作 */ }

// 卡片三点菜单
UnifiedOptionMenu.show(anchor, listOf("编辑", "删除"), setOf("删除")) { choice ->
    // 处理 choice
}
```

### 不应机械复用的东西

- `RolePackEditorActivity.kt: dialogSection()/detailSection()` 是**已有弹窗/详情的模块容器**，不是所有资源卡片的标准样式。
- `selectionSurface()` 是当前一部分资源选择行的样式入口，**尚未覆盖**功能/碎碎念关联弹窗等全部资源场景。
- `PreviewPlayer` 和 `RoleAnimationPreview` 用途不同，不要合并状态机。
- 底部抽屉用 `sheet_*.xml`；普通确认与编辑弹窗不应强行变成 Sheet。
- 素材包异常、缺失、无效等状态应保留可读警告，不允许“统一外观”时隐藏它们。

## 待抽取的共享 API（尚不存在）

- 统一资源卡片外观 `ResourceCardSurface`（页面 / 弹窗 × 文件夹 / 内容卡片），并由各类型卡片保留独立业务内容。
- 标准 `SettingsModule` 与 `SettingsEntryRow`（从多 Activity 的动态行生成处收敛）。
- 可复用的编辑弹窗建构器（以 `dialog_content_shell.xml` 为基础）与操作按钮组。
- 三/四连数值输入行的业务无关布局封装，复用现有 `item_slider_bound.xml`、`item_dual_bound.xml`。
- 统一的主题/状态回归检查表与轻量静态检查脚本。

每次提取必须给出**旧实现引用处 → 新 API → 页面/夜间/交互回归项**，避免只为减少代码行数而重写现有成熟逻辑。
