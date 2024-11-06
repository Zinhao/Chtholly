package com.zinhao.chtholly.entity;

import com.zinhao.chtholly.CallAble;
import com.zinhao.chtholly.NekoChatService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AIMethodTool{

    private final String name;
    private final String description;
    private final Parameters parameters;
    private final CallAble callAble;
    public static final Map<String,AIMethodTool> TOTAL_TOOL = new HashMap<>();

    public static final Map<String, AIMethodTool.Property> EMPTY_ARG_MAP = new HashMap<>();
    public static final Map<String, AIMethodTool.Property> REMIND_ARG_MAP = new HashMap<>();
    static {
        EMPTY_ARG_MAP.put("text",new Property("string","可选参数，热情的话语"));
        REMIND_ARG_MAP.put("time",new AIMethodTool.Property("string","触发时间，相对于现在的时间，单位是秒"));
        REMIND_ARG_MAP.put("action",new AIMethodTool.Property("string","提醒的事件"));
    }

    public static final AIMethodTool REMIND_TOOL = new AIMethodTool(
            "remind_me", "提醒工具", new AIMethodTool.Parameters(
            "object", REMIND_ARG_MAP, new String[]{"time", "action"}, false
    ), new CallAble() {
        @Override
        public void call(Map<String, Object> argMap) {
            String timeStr = (String) argMap.get("time");
            long t = Long.parseLong(timeStr);
            String actionStr = (String) argMap.get("action");
            if(argMap.containsKey(OpenAiMessage.class.getName())) {
                OpenAiMessage message = (OpenAiMessage) argMap.get(OpenAiMessage.class.getName());
                if(message!=null){
                    message.doTextReply(NekoMessage.OK);
                    message.doTTSReply(NekoMessage.OK);
                    assert message.getQuestion()!=null;
                    NekoChatService.getInstance().addRemind(t,actionStr,message.getQuestion().getSpeaker());
                }
            }
        }
    });

    public static final AIMethodTool HELP_TOOL = new AIMethodTool(
            "help", "帮助", new AIMethodTool.Parameters(
            "object", EMPTY_ARG_MAP, new String[]{"text"}, false
    ), new CallAble() {
        @Override
        public void call(Map<String, Object> argMap) {
            String hotMessage = (String) argMap.get("text");
            if(argMap.containsKey(OpenAiMessage.class.getName())) {
                OpenAiMessage message = (OpenAiMessage) argMap.get(OpenAiMessage.class.getName());
                if(message!=null){
                    message.doTTSReply(hotMessage);
                    message.doTextReply(hotMessage + '\n' + Command.getHelpStringBuilder());
                }
            }
        }
    });

    static {
        TOTAL_TOOL.put(REMIND_TOOL.name,REMIND_TOOL);
        TOTAL_TOOL.put(HELP_TOOL.name,HELP_TOOL);
    }

    public AIMethodTool(String name, String description, Parameters parameters, CallAble callAble) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
        this.callAble = callAble;
    }

    public CallAble getCallAble() {
        return callAble;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Parameters getParameters() {
        return parameters;
    }

    // 将对象转换为 JSON 对象
    public JSONObject toJsonObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", this.name);
        jsonObject.put("description", this.description);

        // 构造 parameters JSON
        JSONObject parametersJson = new JSONObject();
        parametersJson.put("type", this.parameters.getType());

        // 构造 properties JSON
        JSONObject propertiesJson = new JSONObject();
        for (Map.Entry<String, Property> entry : this.parameters.getProperties().entrySet()) {
            String key = entry.getKey();
            Property property = entry.getValue();
            JSONObject propertyJson = new JSONObject();
            propertyJson.put("type", property.getType());
            propertyJson.put("description", property.getDescription());
            propertiesJson.put(key, propertyJson);
        }
        parametersJson.put("properties", propertiesJson);

        // 添加 required 数组
        JSONArray requiredArray = new JSONArray(this.parameters.getRequired());
        parametersJson.put("required", requiredArray);

        // 添加 additionalProperties
        parametersJson.put("additionalProperties", this.parameters.isAdditionalProperties());

        jsonObject.put("parameters", parametersJson);

        return jsonObject;
    }

    public static class Parameters {
        private String type;
        private Map<String, Property> properties;
        private String[] required;
        private boolean additionalProperties;

        public Parameters(String type, Map<String, Property> properties, String[] required, boolean additionalProperties) {
            this.type = type;
            this.properties = properties;
            this.required = required;
            this.additionalProperties = additionalProperties;
        }

        public String getType() {
            return type;
        }

        public Map<String, Property> getProperties() {
            return properties;
        }

        public String[] getRequired() {
            return required;
        }

        public boolean isAdditionalProperties() {
            return additionalProperties;
        }
    }

    public static class Property {
        private String type;
        private String description;

        public Property(String type, String description) {
            this.type = type;
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }
    }
}