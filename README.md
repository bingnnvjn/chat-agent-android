# ChatAgent

**一个 Android 原生的 Agent Harness。**

不是又一个聊天客户端。这个项目要回答的问题是：把 Agent 核心做成一层可替换、可扩展的 harness，让它能在 Android 上承载真正的任务执行 —— 并且在这一路上，把 Codex、Claude Code、Pi Agent、DeepSeek Harness 是怎么划分自己的边界，逐条拆开研究透。

> **English**: ChatAgent is a research-driven effort to build a native Android Agent Harness. We study how Codex, Claude Code, Pi Agent and DeepSeek Harness draw the line between the agent core and the host shell, then apply what generalizes — and explicitly reject what doesn't — to a mobile-first platform. The repo currently contains a working Android shell (Kotlin + Compose) with a functioning agent loop and tool-calling runtime, plus the design record that will govern the harness core.

---

## 为什么做这件事

现在主流的 Agent 产品都是桌面优先。它们的 harness 设计里有一大块假设直接绑在"你有一台永远在线、有完整文件系统、有 shell 的机器"上：工作区目录、进程沙箱、任意命令执行、常驻后台。

Android 一条都不成立。

所以直接移植是没有意义的。这个项目的做法是：**先把四家的边界拆清楚，再决定哪些原则是普适的、哪些是桌面工作区特有的、哪些带进移动端就是错的。**

研究不是前置作业，是主线。产品定义是从研究结论里长出来的，不是先画好再找理由。

## 架构方向

```
┌─────────────────────────────────────────────┐
│              Platform Shell                 │
│         Kotlin + Jetpack Compose            │
│                                             │
│   UI · 系统能力 · 工具的**实际执行**          │
│   可替换：换掉 Shell，Harness 不受影响        │
└──────────────────┬──────────────────────────┘
                   │  能力适配层
                   │  核心输出：文本 / 状态 / 能力请求
                   │  外壳返回：执行结果 / 拒绝
┌──────────────────┴──────────────────────────┐
│              Rust Harness Core              │
│          独立 · 跨平台 · 可复用 SDK          │
│                                             │
│   Agent 逻辑 · 会话与上下文 · 模型接口        │
│   工具请求与结果流程 · 事件 · 控制 · 核心扩展点 │
└─────────────────────────────────────────────┘
```

两条边界原则：

- **核心必须能在没有任何工具的情况下跑通最小文本闭环。** 工具是外壳的能力，不是核心的前提。
- **Kotlin/Compose 层可以被整体替换，且不得承载 Agent 核心逻辑。** 外壳负责"在哪跑、怎么跑"，核心负责"跑什么、为什么跑"。

核心与外壳持续双向通信：核心发出能力请求，外壳执行或拒绝后返回结果，核心据此决定继续还是结束。具体的进程边界、FFI 方案和插件 ABI **尚未锁定** —— 这是方向，不是定稿。

## 对标研究

四家各有各的答案，我们把它们放在同一组维度上比：产品定位、Harness/Shell 边界、扩展模型、Agent 工作流、用户控制、上下文与持久化、安全与信任边界。

| 研究对象 | 核心发现 | 明确不照搬的部分 |
|---|---|---|
| **Codex** | 宿主产品负责 UI、业务上下文、工具、运行位置和审批；harness 负责会话、Agent loop、事件、工具编排、沙箱与审批策略 | 桌面 shell 与沙箱边界 |
| **Claude Code** | Coding Agent 的产品定位、客户端与 harness 的分工、上下文组织方式、扩展与权限原则 | 具体实现不直接迁移 |
| **DeepSeek Harness** | 可装配的插件树、service/provider/consumer 三种 seam、typed events、scope、profile/bundle/patch | 高信任扩展模型 |
| **Pi Agent** | 把 Agent core 与 coding-agent CLI/TUI 彻底分开，minimal harness + primitives-not-features 保持核心可替换 | 同进程扩展 + 任意 bash 的信任模型 |

**收敛出的产品定义**（研究结论，非设想）：

- 第一身份是 **Android 原生 Agent 工具平台**，Code Agent 是首个重点场景 —— 但不是唯一。
- **核心保持薄。** 越厚的内核越无法被替换。
- **用户扩展与开发者扩展分属不同信任层，不能共用同一套规则。** 用户插件做的是"组合已有能力、改变怎么用"；开发者模块做的是"增加或替换运行能力"。后者要进核心运行环境，信任要求天然更高。信任按**实际能力**判定，不按声明判定。
- 用户侧扩展参考 Codex / Claude Code；开发者侧参考 DeepSeek Harness / Pi。

完整的研究过程、每一条结论的推演和反例，都在 issue tracker 上留档 —— 见 [文档与研究记录](#文档与研究记录)。

## 代码现状

诚实分层，不夸大：

### 已实现（可运行）

**Agent loop + 工具调用运行时**

- `ChatRepository.agentSendMessage()` / `agentContinueWithToolResult()` —— 非流式 Agent 请求 + 工具结果回填
- `ChatViewModel` 的 Agent 模式开关（`_agentMode`），`executePendingToolCalls()` 负责递归续跑，用 `processedIds` 去重防止重复执行
- `AgentExecutor`：工具注册表、JSON Schema 构建、执行与错误兜底
- 内置工具 4 个：`web_search`、`stock_quote`、`calculator`、`get_time`

**协议层已经具备承载 tool calling 的完整数据结构**

`ApiRequest.kt` 里定义了全链路模型：`ChatRequest`（含 `tools` / `tool_choice`）、`Tool` / `ToolFunction` / `ToolChoice`、`ToolCall` / `ToolCallFunction`、以及 `Delta.tool_calls` 用于流式场景下接收工具调用。`ApiMessage.content` 是 `JsonElement`，原生支持文本与多模态混排。

思考过程分离：`Delta.reasoning_content` 走独立回调，UI 上不和正文混在一起。

**多供应商适配层**

一份枚举定义全部供应商差异（base URL / 模型列表 / 默认模型），加一家只需改枚举。API Key 存在 `EncryptedSharedPreferences`（`androidx.security-crypto`），其余设置走 DataStore。

**液态玻璃 UI 子系统**

独立的 `:backdrop` Gradle 模块，不是第三方依赖：

- `RuntimeShader` + `RuntimeShaderCache` —— AGSL 着色器运行时与缓存
- `DrawBackdropModifier` —— 背景采样与合成
- 效果：`Blur`、`ColorFilter`、`Lens`、`Highlight` / `HighlightStyle`、`InnerShadow` / `Shadow`
- G2 连续圆角（`ContinuousCurvatureRoundedRectangleCornerBuilder`）

全套效果带开关，低端机可以关掉 —— 这在真机上验证过，早期版本因为着色器编译触发过 ANR，所以默认关闭、按需延迟编译。

### 设计中（方案已定，代码未落地）

- Rust Harness Core 作为独立可复用 SDK
- 核心与外壳的能力适配层契约
- 核心扩展点的具体类型、版本与兼容策略

### 未开始

- 自动化测试（当前**一个测试文件都没有** —— 这是已知欠债，见 issue #1 的自述）
- 具体的 Agent loop 状态机、审批流程、持久化方案、插件 ABI
- 超出现有 4 个内置工具之外的工具集

> 本项目**不接入** Codex app-server。运行逻辑自己实现 —— 这也是研究的主要动机之一。

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin 100% |
| UI | Jetpack Compose（BOM 2026.04.01）+ Material 3 |
| 架构 | MVVM + Repository + DataSource（UDF） |
| DI | Hilt 2.59.2 |
| 网络 | Retrofit 2.12.0 + OkHttp 4.12.0 + kotlinx-serialization 1.11.0 |
| 存储 | DataStore 1.1.3（设置与对话）+ EncryptedSharedPreferences（API Key） |
| 构建 | AGP 9.0.1 · Kotlin 2.3.21 · KSP 2.3.9 · Gradle Kotlin DSL |
| 模块 | `:app` + `:backdrop` |

## 构建

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease
```

- 环境要求：JDK 17+（CI 用 21），Android SDK 36
- `minSdk 34` · `targetSdk 36` · `compileSdk 36`
- CI：`.github/workflows/build.yml` 在 push / PR 到 `main` 时构建 debug APK 并上传为 artifact

> 仓库里的 `app/debug.keystore` 是一个**自签名 debug 密钥**（别名 `debug`，口令为标准占位 `android`，证书主体 `CN=Debug`），仅用于本地与 CI 构建 debug 包。它不保护任何发布产物，也不是 release 签名密钥。

## 项目状态

**早期，但方向已定。**

已经走完的路：把 Android 聊天壳做扎实（UI、多供应商、流式、工具调用骨架）→ 反过来研究四家竞品的边界 → 从研究结论倒推产品定义与架构。

还没走的路：把 Rust 核心从设计变成代码，把研究结论变成可运行的契约。

这个仓库当前承载的是**边界定义与验证载体**。代码量和研究深度不匹配 —— 这是刻意的，也是接下来要还的债。

## 文档与研究记录

| 文档 / 入口 | 内容 |
|---|---|
| [`AGENT_ARCHITECTURE_RESEARCH.md`](AGENT_ARCHITECTURE_RESEARCH.md) | 从聊天应用改造为 Agent 的路线图：现状盘点、Agent 循环、工具系统设计、Android 平台限制 |
| [产品定义与竞品研究地图](https://github.com/bingnnvjn/chat-agent-android/issues/2) | 总纲。每条已确立结论的索引 |
| [Codex](https://github.com/bingnnvjn/chat-agent-android/issues/3) · [Claude Code](https://github.com/bingnnvjn/chat-agent-android/issues/4) · [DeepSeek Harness](https://github.com/bingnnvjn/chat-agent-android/issues/5) · [Pi Agent](https://github.com/bingnnvjn/chat-agent-android/issues/6) | 四家竞品的边界研究 |
| [综合原则](https://github.com/bingnnvjn/chat-agent-android/issues/7) · [Harness/Shell 边界](https://github.com/bingnnvjn/chat-agent-android/issues/8) · [SDK 能力边界](https://github.com/bingnnvjn/chat-agent-android/issues/10) · [最小交互流程](https://github.com/bingnnvjn/chat-agent-android/issues/11) | 架构决策 |
| [扩展分层专项](https://github.com/bingnnvjn/chat-agent-android/issues/12) · [分层原则](https://github.com/bingnnvjn/chat-agent-android/issues/9) | 用户扩展 vs 开发者扩展 |

## License

MIT
