package com.zinhao.chtholly.entity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/***
 *  Choice(
 *      finish_reason='tool_calls',
 *      index=0,
 *      logprobs=None,
 *      message=chat.completionsMessage(
 *          content=None,
 *          role='assistant',
 *          function_call=None,
 *          tool_calls=[
 *              chat.completionsMessageToolCall(
 *                  id='call_62136354',
 *                  function=Function(
 *                      arguments='{"order_id":"order_12345"}',
 *                      name='get_delivery_date'),
 *                  type='function')
 *          ])
 *  )
 */
public class Choice {
    private String finishReason;
    private int index;
    private Object logprobs; // 这里使用 Object，具体类型根据需要调整
    private Message message;
    public Choice(String finishReason, int index, Object logprobs, Message message) {
        this.finishReason = finishReason;
        this.index = index;
        this.logprobs = logprobs;
        this.message = message;
    }

    // Getters 和 Setters
    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Object getLogprobs() {
        return logprobs;
    }

    public void setLogprobs(Object logprobs) {
        this.logprobs = logprobs;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public static Choice fromJson(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        String finishReason = jsonObject.getString("finish_reason");
        int index = jsonObject.getInt("index");
        Object logprobs = jsonObject.opt("logprobs"); // 使用 opt，避免缺失时抛出异常
        Message message = Message.fromJson(jsonObject.getJSONObject("message").toString());

        return new Choice(finishReason, index, logprobs, message);
    }


    public static class Message {
        private String content;
        private String role;
        private String functionCall;
        private List<ToolCall> toolCalls;
        private String rawJsonString;
        public Message(String content, String role, String functionCall, List<ToolCall> toolCalls) {
            this.content = content;
            this.role = role;
            this.functionCall = functionCall;
            this.toolCalls = toolCalls;
        }

        // Getters 和 Setters
        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getFunctionCall() {
            return functionCall;
        }

        public void setFunctionCall(String functionCall) {
            this.functionCall = functionCall;
        }

        public List<ToolCall> getToolCalls() {
            return toolCalls;
        }

        public String getRawJsonString() {
            return rawJsonString;
        }

        public void setToolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
        }
        public static Message fromJson(String jsonString) throws JSONException {
            JSONObject jsonObject = new JSONObject(jsonString);
            String content = jsonObject.optString("content", null); // 使用 optString 以处理缺失字段
            String role = jsonObject.getString("role");
            String functionCall = jsonObject.optString("function_call", null);

            List<ToolCall> toolCalls = new ArrayList<>();
            if (jsonObject.has("tool_calls")) {
                JSONArray toolCallsArray = jsonObject.getJSONArray("tool_calls");
                for (int i = 0; i < toolCallsArray.length(); i++) {
                    ToolCall toolCall = ToolCall.fromJson(toolCallsArray.getJSONObject(i).toString());
                    toolCalls.add(toolCall);
                }
            }
            Message message = new Message(content, role, functionCall, toolCalls);
            message.rawJsonString = jsonString;
            return message;
        }
    }

    public static class ToolCall {
        private String id;
        private Function function;
        private String type;

        public ToolCall(String id, Function function, String type) {
            this.id = id;
            this.function = function;
            this.type = type;
        }

        // Getters 和 Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Function getFunction() {
            return function;
        }

        public void setFunction(Function function) {
            this.function = function;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getArgsMap() throws JSONException {
            Choice.Function function = getFunction();
            JSONObject args = new JSONObject(function.getArguments());
            // 创建 Map 来存储结果
            Map<String, Object> argsMap = new HashMap<>();
            // 遍历 JSONObject，将键值对放入 Map 中
            Iterator<String> keys = args.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = args.getString(key);
                argsMap.put(key, value);
            }
            return argsMap;
        }

        public static ToolCall fromJson(String jsonString) throws JSONException {
            JSONObject jsonObject = new JSONObject(jsonString);
            String id = jsonObject.getString("id");
            Function function = Function.fromJson(jsonObject.getJSONObject("function").toString());
            String type = jsonObject.getString("type");

            return new ToolCall(id, function, type);
        }
    }

    public static class Function {
        private String arguments;
        private String name;

        public Function(String arguments, String name) {
            this.arguments = arguments;
            this.name = name;
        }

        // Getters 和 Setters
        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public static Function fromJson(String jsonString) throws JSONException {
            JSONObject jsonObject = new JSONObject(jsonString);
            String arguments = jsonObject.getString("arguments");
            String name = jsonObject.getString("name");

            return new Function(arguments, name);
        }
    }
}
