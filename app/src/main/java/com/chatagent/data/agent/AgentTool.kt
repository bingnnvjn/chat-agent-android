package com.chatagent.data.agent

/**
 * Agent 工具定义 — LLM 通过 name+description 决定何时调用
 */
data class AgentTool(
    val name: String,
    val description: String,
    /** JSON Schema 格式的参数描述 */
    val parameters: Map<String, Any>,
    /** 执行函数：参数 Map → 结果字符串 */
    val execute: (Map<String, Any>) -> String
)
