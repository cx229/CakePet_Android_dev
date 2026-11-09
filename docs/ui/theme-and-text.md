# 主题、文字、状态与日夜间检查

> 第一阶段仅记录已有资源与期望约束。详细值应以仓库 `colors.xml` 和 `themes.xml` 的**当前文件为唯一准绳**，本文不再抄一份可能过期的数值表。

## 主题入口（已存在）

- 日间：`app/src/main/res/values/colors.xml`、`values/themes.xml`。
- 夜间：`app/src/main/res/values-night/colors.xml`、`values-night/themes.xml`。
- 开关状态：`res/color/switch_black_selector.xml`、`res/color-night/switch_black_selector.xml`，`values/themes.xml: Widget.CakePet.Switch`。
- 现有切换入口：`PetService.kt: applyNightMode()` 使用 `AppCompatDelegate`，区分强制开启/关闭/跟随系统；不要创建第二套独立的夜间布尔值。

## 语义颜色

| 用途 | 优先采用 |
|---|---|
| 整体页面与较暗/灰的控件表面 | `cp_bg` |
| 卡片、弹窗内容背景 | `cp_card_bg` |
| 主标题 | `cp_title` |
| 正文/普通文字 | `cp_text` 或当前场景的 `cp_value` |
| 次标题、模块标题 | `cp_sub` |
| 备注/辅助说明 | `cp_note` |
| 表单占位 | `cp_placeholder` |
| 边框/分割线 | `cp_divider`、`cp_subtle` |
| 强调/危险 | `cp_accent`、`cp_danger` |
| 主要/次要操作按钮 | `cp_btn_primary_bg`、`cp_btn_primary_text`、`cp_btn_outline_* ` |
| 触摸反馈 | `cp_ripple_on_light`、`cp_ripple_on_dark`，优先复用现有 ripple drawable |
| 动画编辑预览的底色、辅助线、HUD | `cp_preview_canvas`、`cp_preview_guide`、`cp_preview_bounds`、`cp_preview_anchor`、`cp_preview_hud_bg`、`cp_preview_hud_text` |

**例外**：美术资源本身、位图像素、颜色选择器、业务特定调试标记可以使用具体颜色；不要机械禁止所有 `Color.WHITE` 和 `Color.BLACK`。新增固定色须写明语义原因。

## 文字层级（已存在）

`res/values/styles.xml` 有 `SettingsOptionTitle`、`SettingsOptionNote`、`SettingsOptionSub`、`SettingsModuleTitle`、`SettingsModuleNote`、`SettingsOptionValue`。页面大标题见 `activity_threshold_settings.xml`；编辑弹窗标题见 `dialog_content_shell.xml`。

- 标题描述“这是什么”，备注描述“影响什么/什么时候生效”，提示交代短暂状态，错误明确说明缺失及可修复点。
- 不给每个标题加“默认”或“（默认）”；默认行为通过控件初始化和必要的小字语义表现（已有用户习惯场景另有独立说明）。
- 不用仅凭文字颜色承载错误状态，需保留显式提示。
- 避免备注字号、截断、宽度被局部 UI 优化意外改变。

## 必检状态矩阵（未来回归用）

| 控件 | 日间 | 夜间 | 状态变化 |
|---|---|---|---|
| 下拉 | 文字/箭头/展开行/分隔 | 同左，避免白底白字 | 选中、禁用、取消、长列表 |
| 开关 | 开启与关闭配色 | 开启与关闭配色 | 禁用状态、外层折叠与设置持久化 |
| 滑块及编辑框 | 轨道/输入值/光晕 | 同左 | 越界、输入同步、拖动 |
| 三/四连数值框 | 标签、数值、分隔 | 同左 | 默认值提示、横向宽度 |
| 资源卡片 | 背景/边框/文字 | 对应暗色表面 | 文件夹展开、无效资源提示 |
| 弹窗、底部抽屉 | 标题/遮罩/操作区 | 同左 | 取消/确认/危险操作 |
| 角色自然预览 | 透明背景/缩略图 | 不改变位图 | 暂停、摸头、召唤占位 |
| 动画编辑预览 | 画布、边框、HUD | 暗色画布和高对比信息 | 帧切换、镜像、锚点 |

## 已发现的维护风险

1. `SettingsSpinnerBinder.kt` 以前把下拉箭头写死黑色、弹出列表写死白色，近期已改为主题资源；新组件不可重新引入旧模式。
2. `RoleAnimationPreview.kt` 的画布和 HUD 已迁移至专用颜色，素材位图不能被主题滤镜污染。
3. `RolePackEditorActivity.kt` 的 UI 生成逻辑仍较多，日夜间颜色容易在单独方法中被重新写死。
4. XML 和 Kotlin 里都存在开关构造方式，依赖统一 selector 和主题；是否全部一致仍需实际界面测试。
5. 仅检查颜色值不足以验收：还需要检查重建/主题切换、可读性、触摸反馈与内容裁切。

## 检查清单

- [ ] 亮色、暗色均打开完整页面、普通编辑弹窗、关联弹窗、底部抽屉、三点菜单。
- [ ] 下拉框结果区、箭头、弹出框和每一条选项对比度足够。
- [ ] 允许触摸的开关/按钮与禁用的控件状态可区分；保存/取消行为不变。
- [ ] 黑白角色素材在两种预览环境下仍能识别，原始像素、透明度未被变色。
- [ ] 主题切换不要求手动重启以纠正新开的弹窗/预览。
- [ ] 长文本、不同字号缩放以及三/四连数值控件不发生布局溢出。

本清单目前是**验证计划**，非测试通过证明。
