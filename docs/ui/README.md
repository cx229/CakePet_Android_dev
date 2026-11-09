# CakePet UI 规范与组件索引（第一阶段：现状盘点）

> 基线：`dev/2611091925-night-theme`，盘点日期 2026-11-09。本文是**现有代码索引与后续整理约束**，不代表所有 UI 已完成统一；本阶段不更改界面与业务行为。

## 新增或修改 UI 前的查找顺序

1. 先确定**场景**：完整页面／居中弹窗／底部抽屉／浮出菜单；再确定**语义**：设置项／目录／素材资源／操作按钮／预览。
2. 查 [components.md](components.md) 中的**现存组件与调用入口**，搜索对应 Kotlin/XML 的使用处。
3. 查 [surfaces-and-cards.md](surfaces-and-cards.md) 中的资源卡片背景环境规则，以及 [dialogs-and-navigation.md](dialogs-and-navigation.md) 中的容器与操作区。
4. 读取 [theme-and-text.md](theme-and-text.md)，检查昼夜间配色、文字等级、状态反馈。
5. 复用不能满足时，先注明原因与新旧差异；优先给已有组件补充**显式变体**，不要在业务 Activity 内复制样式常量。
6. 仅优化 UI 不应擅自改变：配置存储、素材包 JSONC、动画链、角色物理行为、点击事件、拖动排序、文件夹展开状态、错误/无效数据提示。

## 资产清单（已经存在，不是拟新增）

| 类别 | 推荐先读 | 当前状态 |
|---|---|---|
| 页面头部、滚动容器 | `app/src/main/res/layout/activity_threshold_settings.xml`、`activity_role_pack_editor.xml` | 已有多种近似布局，尚无全局统一 PageShell |
| 设置卡片、入口、标题备注 | `res/values/styles.xml`、`include_module_header.xml`、`drawable/card_bg.xml`、`drawable/item_divider.xml` | 已有规范，部分页面又手动拼装 |
| 用户习惯及阈值项 | `UserHabitsActivity.kt`、`BaseThresholdActivity.kt`、`item_slider_bound.xml`、`item_dual_bound.xml`、`item_switch_bound.xml` | 可参考真实设置逻辑及三/四数值布局 |
| 个性化设置 | `PersonalizationSettingsActivity.kt` | 直接复用页面资源，但 `module/caption/selector` 在 Activity 内本地定义 |
| 滑块及输入同步 | `SliderHelper.kt` | 已有统一封装；不要另写 SeekBar 监听转换链 |
| 下拉列表 | `SettingsSpinnerBinder.kt` | 统一下拉外观与选中事件；已适配日夜间 |
| 表单输入 | `FormInputField.kt`、`styles.xml: CakePetFormInput` | 已有描边标签输入框 |
| 通用确认与重命名 | `DialogExt.kt`、`dialog_confirm.xml`、`dialog_rename_role.xml` | 已有确认/取消/警告约定 |
| 编辑/选择弹窗壳 | `dialog_content_shell.xml`、`RolePackEditorActivity.kt: editor()/pickerShell()` | 复用外壳，但调用方法仍在大型 Activity 内 |
| 卡片三点选项 | `UnifiedOptionMenu.kt`、`PopupMenuHelper.kt`、`popup_*.xml` | 两条已有菜单路径，迁移前辨别锚点与数据来源 |
| 底部抽屉 | `sheet_*.xml`、`PetMenu.kt`、`FishMenu.kt`、`FreezeMenu.kt` | 多种菜单，尚未抽象共用 SheetShell |
| 首页角色卡片 | `item_role_card.xml`、`MainActivity.kt` | 独立召唤/归位/召回行为，不等同于素材卡片 |
| 素材包列表卡片 | `item_role_catalog.xml`、`RoleManageActivity.kt` | 有预览、数量、作者、操作菜单 |
| 功能/动画/图片/碎碎念卡片 | `RolePackEditorActivity.kt: functionItem()/modes()/resourceBranch()/renderThinkingGroups()` | 样式及逻辑散落；优先做页面/弹窗表面样式收敛 |
| 角色预览 | `PreviewPlayer.kt`、`MainActivity.kt`、`RoleManageActivity.kt`、`RolePackEditorActivity.kt` | 各入口复用播放核心，页面政策不同 |
| 动作编辑预览 | `RoleAnimationPreview.kt`、`RolePackEditorActivity.kt: animate()` | 编辑调试画布，不能与普通角色缩略图强行合并 |
| 颜色与主题 | `res/values/colors.xml`、`res/values-night/colors.xml`、`res/values/themes.xml`、`res/values-night/themes.xml` | 有语义色；局部硬编码仍需后续审计 |

## 已识别的不同类型的重复（待处理，不是已完成）

- **布局容器重复**：`activity_threshold_settings.xml`、`activity_role_pack_editor.xml`、`activity_global_settings.xml` 等有类似顶部/滚动结构，但保留各自差异。
- **局部 UI 工厂重复**：`PersonalizationSettingsActivity.kt` 中局部 `module/caption/selector`；`RolePackEditorActivity.kt` 中 `menuCard/editor/pickerShell/selectionSurface/detailSection/dialogSection`，尚未成为跨 Activity 公共 API。
- **同一资源在两种容器内重复绘制**：`functionTree()` 供功能完整页面及关联弹窗调用，`renderThinkingGroups()` 供碎碎念完整页面及关联弹窗调用；必须显式处理不同背景环境。
- **按钮与弹窗操作区**：`dialog_confirm.xml`、`dialog_content_shell.xml` 和编辑器局部按钮有共同颜色规范，但并非一个全局按钮组件。
- **下拉/开关的主题接入**：有专用绑定器和选择器，仍需要逐入口回归；不能因为某处用统一主题就假定全部场景无误。

## 现阶段不做的事

不迁移 Activity、不重构任何素材包逻辑、不改变默认值与文案、不统一成一个巨大万能卡片、不新增硬依赖的组件框架。先建立索引，后续按组件逐项迁移并保持回归基线。

## 下一阶段优先级

1. 统一设计令牌与日夜间状态清单；将现有有效视觉参数落实为可引用资源（不盲改已有外观）。
2. 优先把资源卡片的**页面/弹窗环境**抽成显式表面样式入口；梳理功能、动画、图片、碎碎念以及缺失/关联弹窗。
3. 将成熟的设置项、按钮、弹窗壳逐步提升为可跨页面复用的 API。
4. 在 CI 加**告警式** UI 复用审计；只对证实不合规的用法考虑阻断。

> 维护规则：任何组件迁移完成时，同步更新上述清单的「当前状态」与真实路径；没有实测的效果写「待验证」，不要写「已统一」。
