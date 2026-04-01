//package com.zinhao.chtholly.session;
//
//import com.google.common.collect.ImmutableMap;
//import com.google.genai.Client;
//import com.google.genai.types.*;
//import com.zinhao.chtholly.BotApp;
//import com.zinhao.chtholly.entity.NetAiAskAble;
//import org.json.JSONException;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.function.Consumer;
//
//public class GeminiApi implements ChatSession {
//    Client client;
//
//    private GeminiApi(String baseUrl) {
//        HttpOptions httpOptions = HttpOptions.builder()
//                .baseUrl(baseUrl)
//                .headers(ImmutableMap.of("key", "value"))
//                .timeout(600)
//                .build();
//        client = Client.builder()
//                .apiKey(BotApp.getInstance().apiKey)
//                .httpOptions(HttpOptions.builder().apiVersion("v1alpha").build())
//                .build();
//    }
//
//    private static GeminiApi geminiApi;
//
//    public static GeminiApi getInstance() {
//        if (geminiApi == null) {
//            return new GeminiApi(BotApp.getInstance().getChatUrl());
//        }
//        return geminiApi;
//    }
//
//    @Override
//    public boolean requestChatCompletions(NetAiAskAble message) {
//        List<Content> contents = new ArrayList<>();
//        contents.add(Content.fromParts(Part.fromText(message.getQuestion().getMessage())));
//        CompletableFuture<GenerateContentResponse> responseFuture =
//                client.async.models.generateContent(GeminiSession.MODEL_GEMINI_3_FL_PRE,
//                        contents, null);
//        responseFuture
//                .thenAccept(
//                        response -> {
//                            message.doTextReply(response.text());
//                            System.out.println("Async response: " + response.text());
//                        })
//                .join();
//        return false;
//    }
//
//    @Override
//    public void requestChatSummarize() {
//
//    }
//
//    @Override
//    public boolean startAsk(NetAiAskAble message) throws JSONException {
//        return false;
//    }
//
//    @Override
//    public void setChatUrl(String chatUrl) {
//
//    }
//
//    @Override
//    public String getChatUrl() {
//        return "";
//    }
//
//    @Override
//    public String getChara() {
//        return "";
//    }
//
//    @Override
//    public void setChara(String desc) {
//
//    }
//
//    @Override
//    public String getContextChat() {
//        return "";
//    }
//
//    @Override
//    public int summarize() {
//        return 0;
//    }
//}
