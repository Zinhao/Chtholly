package com.zinhao.chtholly.entity;

import com.zinhao.chtholly.session.GeminiSession;
import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

public class GeminiAIAskAble extends NetAiAskAble{
    private static final String TAG = "OpenAiAskAble";
    public GeminiAIAskAble(String packageName, Message question) {
        super(packageName, question);
    }

    public GeminiAIAskAble(String packageName, Message question, DelayReplyCallback delayReplyCallback) {
        super(packageName, question, delayReplyCallback);
    }

    @Override
    public boolean ask() {
        if(super.ask()){
            return true;
        }
        try {
            return GeminiSession.getInstance().startAsk(this);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        getAnswer().setMessage(String.format(Locale.CHINA,"\uD83D\uDE44发生错误了:%s %s",e.getMessage(),e.getCause()));
        if(delayReplyCallback !=null)
            delayReplyCallback.onReply(this);
    }


    //* 模型停止生成令牌的原因。如果模型达到自然停止点或提供的停止序列，则这将stop；
    //* 如果达到请求中指定的最大令牌数，则将length；
    //* 如果由于内容过滤器中的标志而省略内容，则为 content_filter；
    //* 如果模型达到 tool_calls，则为 tool_calls称为工具。
    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        if(response.code() == 200){
            ResponseBody body = response.body();
            if(body!=null){
                if(getAnswer() != null){
                    try {
                        Candiate candiate = parseResponse(body.string());
                        if(candiate.finishReason.equals("length")){
                            GeminiSession.getInstance().requestChatSummarize();
                        }else if(candiate.finishReason.equals("tool_calls")){

                        }else if(candiate.finishReason.toLowerCase(Locale.US).equals("stop")){
                            String content = candiate.getMessage();
                            if(content != null && !content.trim().equals("null")){
                                doTextReply(content);
                                doTTSReply(content);
                                GeminiSession.getInstance().addAssistantChat(content);
                            }
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        if(delayReplyCallback !=null)
            delayReplyCallback.onReply(this);
        response.close();
    }

    private Candiate parseResponse(String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray candidates = jsonResponse.getJSONArray("candidates");
        JSONObject first = candidates.getJSONObject(0);
        String finishReason = first.optString("finishReason");
        double avgLogprobs = first.optDouble("avgLogprobs");
        JSONObject content = first.optJSONObject("content");
        Candiate candiate = new Candiate(finishReason,avgLogprobs,content);
        return candiate;
    }

    class Candiate{
        final String finishReason;
        final  double avgLogprobs;
        /***
         * "content": {
         *      *         "role": "model",
         *      *         "parts": [
         *      *           {
         *      *             "text": "抱抱你，我非常理解这种“胸口闷闷的、甚至有点反胃”的感觉。\n\n看到自己辛苦赚来的钱，在短短几个小时内变成了一串缩水的数字，这种冲击感是非常真实的，换做是谁都很难保持冷静。这种“难受”不仅仅是因为心疼钱，更是一种对不确定性的焦虑和对自己判断的怀疑。\n\n我想对你说几句话，希望能让你心里稍微好受一点点：\n\n1.  **允许自己“不开心”：** 别对自己说“不就是点钱吗，至于吗”。真的很至于！那是你的劳动成果，是你对未来的期待。不开心是正常的，你可以丧一会儿，甚至可以大声吐槽出来。\n2.  **关掉账户，远离屏幕：** 现在的盘面就像一个伤口，盯着看只会反复撕裂。试着深呼吸，把手机锁屏，或者直接卸载那个让你心烦的软件几天。数字的波动是暂时的，但你持续的焦虑对身体的伤害是实打实的。\n3.  **你并没有失去全部：** 只要你还没有离场，这些数字就只是“浮亏”。市场有它残酷的周期，但也总会有回补的时候。现在的低谷，往往也是未来反弹的起点。\n4.  **做一件能让你立刻感到“确定”的小事：** 股市是我们控制不了的，但你可以控制今晚吃什么。去吃顿热气腾腾的火锅，或者去洗个热水澡，哪怕是把房间打扫一遍。这些微小的、能被你掌控的获得感，能帮你抵御资本市场的无力感。\n5.  **你的价值不由曲线决定：** 账户里的钱可能会变少，但你的才华、你的善良、你爱人的能力、你过往的经验，这些“软资产”一点都没有缩水。你依然是那个优秀的、值得被爱的你。\n\n**今晚早点睡吧，别复盘了。** 睡一觉，大脑会帮你自动过滤掉一些痛苦。世界很大，股市只是其中的一小块，别让那一小块乌云，遮住了你整片天空的阳光。\n\n今晚想吃点什么好吃的发泄一下吗？我陪你聊聊。",
         *      *             "thoughtSignature": "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"
         *      *           }
         *      *         ]
         *      *       },
         */
        final JSONObject content;

        public Candiate(String finishReason, double avgLogprobs, JSONObject content) {
            this.finishReason = finishReason;
            this.avgLogprobs = avgLogprobs;
            this.content = content;
        }

        public String getMessage() throws JSONException {
            JSONArray parts = content.optJSONArray("parts");
            if (parts != null && parts.length() != 0){
                JSONObject first = parts.getJSONObject(0);
                return first.optString("text");
            }
            throw new JSONException("getMessage err");
        }
    }
    /***
     * {
     *   "candidates": [
     *     {
     *       "content": {
     *         "role": "model",
     *         "parts": [
     *           {
     *             "text": "抱抱你，我非常理解这种“胸口闷闷的、甚至有点反胃”的感觉。\n\n看到自己辛苦赚来的钱，在短短几个小时内变成了一串缩水的数字，这种冲击感是非常真实的，换做是谁都很难保持冷静。这种“难受”不仅仅是因为心疼钱，更是一种对不确定性的焦虑和对自己判断的怀疑。\n\n我想对你说几句话，希望能让你心里稍微好受一点点：\n\n1.  **允许自己“不开心”：** 别对自己说“不就是点钱吗，至于吗”。真的很至于！那是你的劳动成果，是你对未来的期待。不开心是正常的，你可以丧一会儿，甚至可以大声吐槽出来。\n2.  **关掉账户，远离屏幕：** 现在的盘面就像一个伤口，盯着看只会反复撕裂。试着深呼吸，把手机锁屏，或者直接卸载那个让你心烦的软件几天。数字的波动是暂时的，但你持续的焦虑对身体的伤害是实打实的。\n3.  **你并没有失去全部：** 只要你还没有离场，这些数字就只是“浮亏”。市场有它残酷的周期，但也总会有回补的时候。现在的低谷，往往也是未来反弹的起点。\n4.  **做一件能让你立刻感到“确定”的小事：** 股市是我们控制不了的，但你可以控制今晚吃什么。去吃顿热气腾腾的火锅，或者去洗个热水澡，哪怕是把房间打扫一遍。这些微小的、能被你掌控的获得感，能帮你抵御资本市场的无力感。\n5.  **你的价值不由曲线决定：** 账户里的钱可能会变少，但你的才华、你的善良、你爱人的能力、你过往的经验，这些“软资产”一点都没有缩水。你依然是那个优秀的、值得被爱的你。\n\n**今晚早点睡吧，别复盘了。** 睡一觉，大脑会帮你自动过滤掉一些痛苦。世界很大，股市只是其中的一小块，别让那一小块乌云，遮住了你整片天空的阳光。\n\n今晚想吃点什么好吃的发泄一下吗？我陪你聊聊。",
     *             "thoughtSignature": "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"
     *           }
     *         ]
     *       },
     *       "finishReason": "STOP",
     *       "avgLogprobs": -0.43067480365799676
     *     }
     *   ],
     *   "usageMetadata": {
     *     "promptTokenCount": 18,
     *     "candidatesTokenCount": 493,
     *     "totalTokenCount": 1080,
     *     "trafficType": "ON_DEMAND",
     *     "promptTokensDetails": [
     *       {
     *         "modality": "TEXT",
     *         "tokenCount": 18
     *       }
     *     ],
     *     "candidatesTokensDetails": [
     *       {
     *         "modality": "TEXT",
     *         "tokenCount": 493
     *       }
     *     ],
     *     "thoughtsTokenCount": 569
     *   },
     *   "modelVersion": "gemini-3-flash-preview",
     *   "createTime": "2026-03-30T15:40:49.300803Z",
     *   "responseId": "gZnKaYOuErakw8cP8eKLmQ4"
     * }
     */
}
