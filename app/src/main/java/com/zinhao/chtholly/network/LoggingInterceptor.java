package com.zinhao.chtholly.network;

import android.util.Log;

import com.zinhao.chtholly.NekoChatService;

import com.zinhao.chtholly.utils.FileLogger;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class LoggingInterceptor implements Interceptor {
    private static final String TAG = "LoggingInterceptor";
    @Override
    public @NotNull Response intercept(Chain chain) throws IOException {
        // 获取请求
        Request request = chain.request();

        // 打印请求信息
        FileLogger.INSTANCE.i(TAG,"Sending request to URL: " + request.url());
        FileLogger.INSTANCE.i(TAG,"Sending request to URL: " + request.url());
        FileLogger.INSTANCE.i(TAG,"Request method: " + request.method());
        if (request.body() != null) {
            FileLogger.INSTANCE.i(TAG,"Request body: " + request.body());
        }

        // 继续发送请求
        Response response = chain.proceed(request);

        // 读取响应体
        ResponseBody responseBody = response.body();
        assert responseBody != null;
        String responseBodyString = responseBody.string(); // 读取响应体

        // 打印响应信息
        FileLogger.INSTANCE.i(TAG,"Received response from URL: " + response.request().url());
        FileLogger.INSTANCE.i(TAG,"Received response from URL: " + response.request().url());
        FileLogger.INSTANCE.i(TAG,"Response code: " + response.code());
        FileLogger.INSTANCE.i(TAG,"Response body: " + responseBodyString);

        // 创建新的 ResponseBody，以便在后续处理中使用
        ResponseBody newResponseBody = ResponseBody.create(responseBodyString, responseBody.contentType());

        // 返回一个新的 Response 对象
        return response.newBuilder()
                .body(newResponseBody) // 替换为新的 ResponseBody
                .build();
    }
}