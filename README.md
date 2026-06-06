# Chat Agent - Liquid Glass 风格 Android 应用

模仿 ChatGPT iOS Liquid Glass 风格的 Android 原生应用。

## 功能特性

- 🎨 Liquid Glass 毛玻璃效果
- 💬 多供应商 AI 对话（Agnes AI、DeepSeek、MiniMax、GLM、Kimi、MiMo、OpenAI）
- 📱 流式输出，打字机效果
- 📁 文件上传支持
- 🎤 语音输入
- 📝 Markdown 渲染和代码高亮
- 🌙 深色/浅色主题切换
- 💾 本地对话历史保存

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **网络**: Retrofit + OkHttp
- **存储**: DataStore

## 系统要求

- Android 14+ (API 34)
- 目标版本: Android 15 (API 35)

## 构建

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

## APK 体积目标

- < 5MB (Release)

## 运行内存目标

- < 50MB

## 许可证

MIT License
