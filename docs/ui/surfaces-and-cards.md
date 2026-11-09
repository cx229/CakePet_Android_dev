# 页面、弹窗与资源卡片外观（第一阶段：已确认规则）

> 这份表规定**视觉语义**，不是“只有文件树”或“只有勾选弹窗”。素材资源无论可选择、可编辑、只读、被引用、缺失修复，均按所处容器选择样式。本文未实施全量迁移。

## 环境 × 资源类型

| 资源语义 | 灰色背景的完整页面 | 白色背景的弹窗 |
|---|---|---|
| 文件夹/分组 | 白底 + 灰色细边框 | 白底 + 灰色细边框 |
| 内容/叶子卡片（功能、动画、图片、碎碎念等） | 白底 + 无边框 | 浅灰底 + 无边框 |

补充：
- **“内容卡片”不要求是树叶**：功能缺失、只读关联动画、碎碎念引用展示也可以是资源内容卡片。
- 角色首页卡片、素材包列表卡片是**业务卡片变体**；整体背景与边框需按其所在场景和现有 UI 协调，不能简单覆盖召唤按钮、图片缩略图等内层组件。
- 文件夹边框应作用于卡片背景自身；**不能通过增加外部 padding 假装边框**而改变缩进。
- 展开/收起、排序、复选、多选、三点菜单、标题/备注与缺失红字都不属于本次表面样式应修改的行为。
- 使用主题语义色表达“白/灰”，夜间应对应为 `cp_card_bg` / `cp_bg`（具体对比度待视觉验收），不是强制使用 `Color.WHITE`。
- 页面级卡片、表单模块、设置项不是一律“灰色叶子”；应先判断是否在展示素材资源，不得机械批量替换 `dialogSection()`。

## 实现现场和迁移目标

| 资源场景 | 已证实现有路径 | 发现/下一步 |
|---|---|---|
| 功能映射页 | `RolePackEditorActivity.kt: functionItem(), functionTree(), functions()` | `functionItem` 同时供文件夹和内容使用，需明确传递资源语义 |
| 编辑功能/动画关联弹窗 | `RolePackEditorActivity.kt: showLinkedFunctions()` | 复用 `functionTree`；必须显式传递容器环境而非从 Activity 推测 |
| 功能缺失弹窗 | `RolePackEditorActivity.kt: renderMissingLinkedCards()` | 是资源内容卡片，不是普通开关/设置项 |
| 动画列表与分组 | `RolePackEditorActivity.kt: modes()` | 组标题与动画条目目前手动使用 `card_bg` |
| 图片资源页面 | `RolePackEditorActivity.kt: resourceBranch()` | 使用 `selectionSurface(isFolder)`，页面不能被默认弹窗样式覆盖 |
| 碎碎念完整页面及设置入口弹窗 | `RolePackEditorActivity.kt: renderThinkingGroups(target,linkedPreview)`、`showLinkedThinking()` | 同一渲染函数跨容器；需把页面/弹窗环境独立为显式参数 |
| 绑定功能/选择后续动画 | `RolePackEditorActivity.kt: chooseModes()` 和动画绑定功能选项弹窗 | 目录与资源选项两种表面 |
| 选择图片素材 | `RolePackEditorActivity.kt: showImageChooser()` | 目录/图片列表，用相同资源样式规范 |
| 选择碎碎念文本 | `RolePackEditorActivity.kt: chooseThinkingPool()` | 分组/文本项以及时间设置要分清内容卡片与表单 |
| 编辑功能 / 编辑动画 / 编辑碎碎念文本 | `RolePackEditorActivity.kt: functionDetails()/modeDetails()/thinkingDetails()` | 核对关联素材的卡片，而非无差别改动全部属性区块 |

## 样式代码现状（仍未统一）

- `RolePackEditorActivity.kt: selectionSurface(view,isFolder)`：已区分目录与叶子背景，**但二者目前都设置边框**，与目标规范不同。
- `RolePackEditorActivity.kt: functionItem()`：通用功能项与目录项共用卡片背景，需要区分。
- `RolePackEditorActivity.kt: modes()/renderThinkingGroups()`：大量使用 `card_bg` 的行内构造。
- `RolePackEditorActivity.kt: dialogSection()/detailSection()`：独立业务模块容器，不应以资源表面样式覆盖。
- `res/drawable/card_bg.xml`、`res/drawable/bg_thumb_border.xml`、`res/drawable/bg_btn_outline.xml`：各自有用途，先查用途再改。

## 建议的迁移方式（仅设计，未创建 API）

统一的**资源表面工厂**接收 `surfaceContext = Page | Dialog` 和 `kind = Folder | Content`，只决定背景、细边框、圆角、水波纹和可用的基础间距，不承接文件树数据、JSONC、选择逻辑或动画状态。

完整业务卡片（角色、素材、功能、动画、图片、碎碎念）再把内容插入外观容器；禁止同一个“万能卡片”承担全部业务。

### 验收矩阵

须按**页面/弹窗 × 日间/夜间 × 文件夹/内容 × 正常/无效状态**验证。展开后缩进不跳变；列表重新渲染后滚动位置不异常；触摸与长按命中不变；素材缩略图不被主题着色。
