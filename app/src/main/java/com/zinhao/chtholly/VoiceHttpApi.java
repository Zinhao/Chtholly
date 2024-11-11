package com.zinhao.chtholly;

import android.net.Uri;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;

public class VoiceHttpApi {
    public static void getModelInfo(AsyncHttpClient.JSONArrayCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(BotApp.getInstance().getTtsUrl() + "/model_info"), "GET");
        request.setTimeout(5000);
        AsyncHttpClient.getDefaultInstance().executeJSONArray(request, callback);
    }
}
