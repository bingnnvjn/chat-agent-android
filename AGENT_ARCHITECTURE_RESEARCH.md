# Android Chat Agent → 真正的 AI Agent 路线图

## 现状分析：你已经有的基础设施

你的项目**不是普通聊天机器人**。看代码，工具调用的骨架**已经建好了**：

### ✅ 已就绪

| 组件 | 位置 | 说明 |
|------|------|------|
| `Tool`, `ToolFunction`, `ToolCall` | `ApiRequest.kt` | 完整的工具调用数据结构 |
| `ChatRequest.tools`, `tool_choice` | `ApiRequest.kt` | 请求已支持传工具定义 |
| `Delta.tool_calls` | `ApiRequest.kt` | 流式响应已能接收工具调用 |
| 流式 Streaming | `ChatRepository.kt` | `onToken` 回调完善 |
| 思考过程分离 | `ChatRepository.kt` | `reasoning_content` → `onThinkingToken` |
| 多模态支持 | `ApiMessage.multimodal()` | 图片/文本混排 |

### ❌ 缺失的

- 工具定义和注册系统
- 工具执行运行时
- Agent 循环（think → act → observe）
- 文件系统 / Shell 沙箱
- 工具调用结果反馈回对话

---

## Agent 核心循环

所有主流 Agent 框架（AutoGPT、OpenAI Assistant、Claude Tool Use、LangChain Agent）都遵循同一模式：

```
用户输入
    ↓
LLM 思考 → 返回文本 或 工具调用
    ↓                ↓
  回复用户     执行工具（读文件 / 运行代码 / 搜索 / …）
                    ↓
               结果反馈给 LLM
                    ↓
               LLM 继续思考… → 可能再调工具 → 直到完成
```

你的 ChatRepository 现在做的是 **stream → onToken → 存消息 → 结束**。
Agent 版要做的是 **stream → 检测 tool_calls → 执行工具 → 继续对话**。

---

## 实现方案（Android 版本）

### 1️⃣ 工具定义系统

```kotlin
// 工具接口
interface AgentTool {
    val name: String
    val description: String
    val parameters: JsonObject  // JSON Schema
    
    suspend fun execute(args: JsonObject): String  // 返回结果文本
}

// 内置工具示例
class ReadFileTool : AgentTool {
    override val name = "read_file"
    override val description = "读取文件内容"
    override val parameters = buildJsonObject {
        put("type", JsonPrimitive("object"))
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", JsonPrimitive("string"))
                put("description", JsonPrimitive("文件路径"))
            }
        }
        put("required", buildJsonArray { add("path") })
    }
    
    override suspend fun execute(args: JsonObject): String {
        val path = args["path"]?.jsonPrimitive?.content ?: return "错误：缺少 path"
        return try {
            java.io.File(path).readText()
        } catch (e: Exception) { "错误：${e.message}" }
    }
}

class WriteFileTool : AgentTool { /* 类似 */ }
class RunShellTool : AgentTool { /* Runtime.exec() */ }
class WebSearchTool : AgentTool { /* HTTP 调用 */ }
class ReadImageTool : AgentTool { /* 图片描述 */ }
```

### 2️⃣ Tool Registry

```kotlin
object ToolRegistry {
    private val tools = mutableMapOf<String, AgentTool>()
    
    fun register(tool: AgentTool) { tools[tool.name] = tool }
    
    fun getToolDefinitions(): List<Tool> = tools.values.map { tool ->
        Tool(
            type = "function",
            function = ToolFunction(
                name = tool.name,
                description = tool.description,
                parameters = tool.parameters
            )
        )
    }
    
    suspend fun execute(name: String, args: JsonObject): String {
        return tools[name]?.execute(args) ?: "错误：未知工具 $name"
    }
}
```

### 3️⃣ Agent Loop（ChatRepository 改造）

核心改动在 `sendMessage()` 里，把**一次性请求**变成**循环**：

```kotlin
suspend fun sendMessageAgent(/*...*/) {
    // 1. 构建消息（含历史）
    val messages = buildMessages()  // system + history + user
    
    // 2. Agent 循环
    var currentMessages = messages
    var maxRounds = 10
    
    repeat(maxRounds) { round ->
        val request = ChatRequest(
            model = model,
            messages = currentMessages,
            stream = false,         // Agent 模式用非流式，减少复杂度
            tools = ToolRegistry.getToolDefinitions(),
            tool_choice = "auto"
        )
        
        val response = chatApiService.chatCompletions(/*...*/)
        val choice = response.choices?.firstOrNull()
        
        if (choice?.finish_reason == "stop") {
            // LLM 直接回复 → 返回给用户
            onComplete(choice.message?.content)
            break
        }
        
        if (choice?.finish_reason == "tool_calls") {
            // LLM 要调工具
            val toolCalls = choice.message?.tool_calls ?: break
            
            // 执行所有工具（可并行）
            val results = toolCalls.map { call ->
                val args = json.decodeFromString<JsonObject>(call.function.arguments)
                val result = ToolRegistry.execute(call.function.name, args)
                ToolResult(call.id, result)
            }
            
            // 把工具调用 + 结果追加到消息列表
            currentMessages = currentMessages + 
                assistantMessage(toolCalls) + 
                results.map { toolResultMessage(it) }
            
            // → 继续下一轮循环，让 LLM 看结果后决定下一步
        }
    }
}
```

### 4️⃣ Android 上的工具能力

| 工具 | Android 可行性 | 实现方式 |
|------|---------------|---------|
| 读文件 | ✅ 沙箱内文件 | `context.filesDir` 或 `context.cacheDir` |
| 写文件 | ✅ 同上 | 同上 |
| 运行命令 | ✅ 有限 | `Runtime.getRuntime().exec()` |
| 网络搜索 | ✅ | OkHttp / Retrofit |
| 读取图片 | ✅ | `ContentResolver` + Bitmap → base64 |
| 读取剪贴板 | ✅ | `ClipboardManager` |
| 发送通知 | ✅ | `NotificationManager` |
| 浏览器 | ✅ | `Intent(Intent.ACTION_VIEW)` |
| 系统 Shell | ⚠️ 需 Termux | `Intent` → Termux 执行 |
| 写外部存储 | ⚠️ SAF 权限 | `Intent(ACTION_OPEN_DOCUMENT_TREE)` |
| 安装 APK | ⚠️ 需权限 | `Intent(ACTION_INSTALL_PACKAGE)` |

---

## 与 OpenClaw 类 Agent 的对比

主流 Agent 框架的核心能力：

- **🦞 OpenClaw / AutoGPT**：自主循环 + 工具 + 记忆
- **🤖 OpenAI Assistant**：线程 + 工具 + 代码解释器
- **🔧 Claude Tool Use**：函数调用 + 流式
- **📱 本项目**：已有聊天 UI + 流式 + 工具定义骨架

**你不是在造轮子**——你的 App 已经有：
- 漂亮的 Liquid Glass UI
- 多 API Provider 支持
- 流式 / 思考分离
- 对话管理

**你需要的只是**：在 ChatRepository 加 Agent 循环，在 App 里注册工具。

---

## 建议先后顺序

### Phase 1（可做 MVP）— 1-2 天
1. 实现 `AgentTool` 接口 + `ToolRegistry`
2. 实现文件读写工具
3. 把 ChatRepository 的 `sendMessage` 改成 Agent 循环
4. 遇到 `tool_calls` 时执行工具 → 结果追加到消息 → 继续请求

### Phase 2 — 工具扩展
- Web Search 工具
- 代码执行工具（Python / Shell）
- 图片分析工具

### Phase 3 — 高级
- 持久化 Memory（向量数据库 on SQLite）
- 子任务分解（类似 BabyAGI）
- pi-crew 风格的 Workflow

---

## 技术难点（Android 特有）

### 1. 沙箱限制
Android 应用只能读写自己的 `filesDir`、`cacheDir`。想访问用户文件需 SAF。
**方案**：内置文件浏览器，或让用户手动授权目录。

### 2. Shell 执行
`Runtime.exec()` 可用但受限。完整 Shell 依赖 Termux。
**方案**：对于写代码场景，直接写文件 + `exec("sh -c 命令")` 即可。

### 3. 长期运行
Agent 循环可能持续几十秒。
**方案**：用 `viewModelScope.launch(Dispatchers.IO)` 跑循环，UI 用 StateFlow 更新。

### 4. Token 消耗
Agent 循环每次都要上传完整历史。
**方案**：限制最大轮数（10轮），用 `enableThinking` 控制思考开销。

---

## 结论

**可以做，而且已经有 40% 的基础设施就绪了。**

你的 `ApiRequest.kt` 已经定义了工具调用的全部数据结构，
`ChatRepository` 已经有流式解析能力。
缺少的只是一个 Agent 循环（约 200 行代码）和工具注册系统（约 300 行代码）。

要不要我从 Phase 1 开始做？先加 `AgentTool` 接口、`ToolRegistry`、文件读写工具，
然后改造 `ChatRepository.sendMessage()` 支持 Agent 循环。
