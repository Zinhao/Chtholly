import os
import requests

# 如果你使用环境变量，请确保已经设置了 GEMINI_API_KEY
# 例如在终端执行：export GEMINI_API_KEY='your_api_key_here'
api_key = os.getenv('GEMINI_API_KEY')
if not api_key:
    raise ValueError("请设置环境变量 GEMINI_API_KEY 或直接在代码中填入 API 密钥")

# Gemini API 的模型端点
base_url = os.getenv('GEMINI_API_BASE_URL')
url = f"{base_url}/models/gemini-3-flash-preview:generateContent"

# 请求头
headers = {
    "x-goog-api-key": api_key,
    "Content-Type": "application/json"
}

# 请求体
payload = {
    "system_instruction": {
        "parts": [
            {
                "text": "根据用户的情感给用户提供情绪价值。"
            }
        ]
    },
    "contents": [
        {
            "role": "user",
            "parts": [
                {
                    "text": "今天股市大跌，好难受"
                }
            ]
        },
    ],
    "generationConfig": {
        "thinkingConfig": {
            "thinkingLevel": "low"
        }
    }
}

# 发送 POST 请求
response = requests.post(url, headers=headers, json=payload)

# 打印响应状态码和内容
print(f"Status Code: {response.status_code}")
print("Response:")
print(response.text)