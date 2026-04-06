# 🦋 Chtholly

“这个世界上最幸福的女孩。” —— 献给每一台被赋予新生的小屏设备。
* 名字来自《末日在干什么？有没有空？可以来拯救吗？》中的这个世界上最幸福女孩——珂朵莉。

## 什么是 Chtholly？
Chtholly 是一个 **Android Agent 自动化框架**。
它通过 Android 原生无障碍服务（Accessibility Service）实时读取界面树结构，结合大模型（AI）的推理能力，实现对 Android 手机的完全控制。

它旨在让闲置的 Android 手机变身为智能代理，能够自主完成消息收发、群聊监控、自动化交互等任务。

## ✨ 核心功能
*   **多平台接入**：支持主流通讯 App（如 QQ），可轻松扩展至其他应用。
*   **群聊助手**：构建自动回复、智能互动的群聊天机器人。
*   **系统监控**：基于屏幕状态的自动化监测与警报。
*   **AI 驱动决策**：内置函数调用功能，支持 Gemini 等大模型，实现对手机的“意图式”操作。

## 🛠️ 技术架构
*   **界面交互**：Android Accessibility Service（获取界面 UI 树）。
*   **AI 推理**：支持 Gemini API（原生支持函数调用），OpenAI 模型适配中。
*   **音频反馈**：集成 TTS 引擎，支持网络流式语音播放。

---

## 🚀 开发者接入指南

Chtholly 具有极高的扩展性。如果你想让它支持更多的 App，请遵循以下流程：

### 1. 扩展 ChatHandler
所有聊天 App 的适配均需继承 `packageName.utils.BaseChatHandler`。参考 `QQChatHandler.java` 实现你的适配逻辑。

### 2. 界面分析
开启无障碍服务后，应用会将当前界面的 XML 树结构保存至 `/data/data/packageName/cache`。你可以通过分析这些缓存文件来编写适配规则。

### 3. 语音接入
在 `NekoChatService.java` 中的 `playTTSVoiceFromNetWork` 函数内接入你的 TTS 服务，实现语音回馈。

---

## ⚙️ 使用说明

1.  **准备环境**：准备一个 AI 服务提供商（推荐 Gemini）的 `API-Key`。
2.  **安装配置**：安装本程序，并根据引导完成初始配置。
3.  **开启服务**：在系统设置中开启本应用的 **无障碍服务**。