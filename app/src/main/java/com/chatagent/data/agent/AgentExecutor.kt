package com.chatagent.data.agent

import com.chatagent.data.model.Tool
import com.chatagent.data.model.ToolFunction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Agent 工具注册和执行器
 * 管理所有可用工具，将工具定义转换为 LLM 可识别的格式
 */
class AgentExecutor {

    private val tools = mutableMapOf<String, AgentTool>()

    /** 注册一个工具 */
    fun register(tool: AgentTool) {
        tools[tool.name] = tool
    }

    /** 获取所有已注册的工具 */
    fun getAll(): List<AgentTool> = tools.values.toList()

    /**
     * 将内部工具定义转换为 LLM API 可识别的 Tool 格式
     */
    fun toApiTools(): List<Tool> {
        return tools.values.map { tool ->
            Tool(
                type = "function",
                function = ToolFunction(
                    name = tool.name,
                    description = tool.description,
                    parameters = buildJsonSchema(tool.parameters)
                )
            )
        }
    }

    /**
     * 执行指定工具
     * @return 工具执行结果文本
     */
    fun execute(name: String, args: Map<String, Any>): String {
        val tool = tools[name] ?: return "错误: 未知工具 '$name'"
        return try {
            tool.execute(args)
        } catch (e: Exception) {
            "错误: 工具 '$name' 执行失败 — ${e.message}"
        }
    }

    /** 判断是否有某个工具 */
    fun has(name: String): Boolean = tools.containsKey(name)

    /** 工具数量 */
    val size: Int get() = tools.size

    // ─── 内置工具 ───

    /** 注册所有内置工具 */
    fun registerDefaultTools(
        webSearch: (String) -> String = { "需要注入 Web 搜索实现" },
        stockQuote: (String) -> String = { "需要注入股票查询实现" }
    ) {
        register(AgentTool(
            name = "web_search",
            description = "搜索互联网获取最新信息。当你需要实时信息、新闻、或者不确定的内容时使用。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "query" to mapOf("type" to "string", "description" to "搜索关键词")
                ),
                "required" to listOf("query")
            ),
            execute = { args ->
                val query = args["query"]?.toString() ?: return@AgentTool "错误: 缺少 query 参数"
                webSearch(query)
            }
        ))

        register(AgentTool(
            name = "stock_quote",
            description = "查询股票实时行情。支持A股代码（如600519）和港股代码（如0700）。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "code" to mapOf("type" to "string", "description" to "股票代码")
                ),
                "required" to listOf("code")
            ),
            execute = { args ->
                val code = args["code"]?.toString() ?: return@AgentTool "错误: 缺少 code 参数"
                stockQuote(code)
            }
        ))

        register(AgentTool(
            name = "calculator",
            description = "数学计算。当你需要计算数字时使用（四则运算、幂运算、三角函数等）。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "expression" to mapOf("type" to "string", "description" to "数学表达式，如 1 + 2 * 3")
                ),
                "required" to listOf("expression")
            ),
            execute = { args ->
                val expr = args["expression"]?.toString() ?: return@AgentTool "错误: 缺少 expression 参数"
                evaluateExpression(expr)
            }
        ))

        register(AgentTool(
            name = "get_time",
            description = "获取当前本地时间和日期。",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "format" to mapOf("type" to "string", "description" to "时间格式（可选）：date/ time/ datetime")
                ),
                "required" to emptyList<String>()
            ),
            execute = { args ->
                val fmt = args["format"]?.toString() ?: "datetime"
                val sdf = SimpleDateFormat(
                    when (fmt) {
                        "date" -> "yyyy-MM-dd"
                        "time" -> "HH:mm:ss"
                        else -> "yyyy-MM-dd HH:mm:ss"
                    },
                    Locale.getDefault()
                )
                sdf.format(Date())
            }
        ))
    }

    // ─── JSON Schema 构建 ───

    private fun buildJsonSchema(params: Map<String, Any>): JsonObject {
        return buildJsonObject {
            put("type", (params["type"] as? String) ?: "object")
            (params["properties"] as? Map<*, *>)?.let { props ->
                putJsonObject("properties") {
                    props.forEach { (key, value) ->
                        if (key is String && value is Map<*, *>) {
                            putJsonObject(key) {
                                put("type", (value["type"] as? String) ?: "string")
                                (value["description"] as? String)?.let { put("description", it) }
                            }
                        }
                    }
                }
            }
            (params["required"] as? List<*>)?.let { required ->
                put("required", kotlinx.serialization.json.buildJsonArray {
                    required.forEach { add(JsonPrimitive(it.toString())) }
                })
            }
        }
    }

    // ─── 数学表达式求值（安全沙箱版）───

    private fun evaluateExpression(expr: String): String {
        return try {
            val sanitized = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", Math.PI.toString())
                .replace("e", Math.E.toString())
            // 只允许安全字符
            if (!sanitized.matches(Regex("^[\\d\\s\\+\\-\\*\\.\\/\\^\\%\\(\\)\\,\\s\\.eE]+$"))) {
                return "错误: 表达式包含不安全的字符"
            }
            // 使用 JavaScript 引擎或简单的表达式解析
            // 简单实现：只支持基础四则运算
            val result = try {
                // 使用 javax.script.ScriptEngineManager (Android 可用)
                val manager = javax.script.ScriptEngineManager()
                val engine = manager.getEngineByExtension("js")
                if (engine != null) {
                    engine.eval(sanitized)
                } else {
                    // fallback: 简单解析
                    simpleEval(sanitized)
                }
            } catch (_: Exception) {
                simpleEval(sanitized)
            }
            if (result is Double) {
                if (result == result.toLong().toDouble()) result.toLong().toString()
                else String.format("%.4f", result).trimEnd('0').trimEnd('.')
            } else {
                result.toString()
            }
        } catch (e: Exception) {
            "错误: 表达式 '$expr' 计算失败 — ${e.message}"
        }
    }

    private fun simpleEval(expr: String): Double {
        // 极简实现：只支持加减乘除
        val tokens = expr.trim().split(Regex("(?<=[\\+\\-\\*\\/])|(?=[\\+\\-\\*\\/])"))
        var result = tokens.first().trim().toDouble()
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i].trim()
            val num = tokens.getOrNull(i + 1)?.trim()?.toDouble() ?: break
            result = when (op) {
                "+" -> result + num
                "-" -> result - num
                "*" -> result * num
                "/" -> if (num != 0.0) result / num else throw ArithmeticException("除数不能为0")
                else -> break
            }
            i += 2
        }
        return result
    }
}
