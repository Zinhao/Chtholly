package com.zinhao.chtholly.network;

import android.net.Uri;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.zinhao.chtholly.BotApp;

public class VoiceHttpApi {
    public static void getModelInfo(AsyncHttpClient.JSONArrayCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(BotApp.getInstance().getTtsUrl() + "/model_info"), "GET");
        request.setTimeout(5000);
        AsyncHttpClient.getDefaultInstance().executeJSONArray(request, callback);
    }
}
