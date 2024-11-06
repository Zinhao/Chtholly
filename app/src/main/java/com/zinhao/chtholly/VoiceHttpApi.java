package com.zinhao.chtholly;

import android.net.Uri;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;

public class VoiceHttpApi {
    public static final String HOST = "http://192.168.31.253";
    public static void getModelInfo(AsyncHttpClient.JSONArrayCallback callback) {
        AsyncHttpRequest request = new AsyncHttpRequest(Uri.parse(HOST + "/model_info"), "GET");
        request.setTimeout(5000);
        AsyncHttpClient.getDefaultInstance().executeJSONArray(request, callback);
    }
}
