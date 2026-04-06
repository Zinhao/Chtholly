package com.zinhao.chtholly.utils;

import android.content.Context;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.zinhao.chtholly.BotApp;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class LocalFileCache{
    private static final String TAG = "LocalFileCache";
    private static LocalFileCache instance;

    public static synchronized LocalFileCache getInstance() {
        if (instance == null) {
            instance = new LocalFileCache();
        }
        return instance;
    }

    private LocalFileCache() {}

    public File getExternalAppRootDir() throws FileNotFoundException {
        File rootDir;
        rootDir = BotApp.getInstance().getExternalFilesDir(null);
        if(rootDir == null){
            rootDir = BotApp.getInstance().getFilesDir();
        }
        return rootDir;
    }


    public File getWorkSpaceDir() {
        File appExternalFilesDir = null;
        try {
            appExternalFilesDir = getExternalAppRootDir();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        File workSpace = new File(appExternalFilesDir, "WorkSpace");
        if (!workSpace.exists()) {
            if (!workSpace.mkdirs()) {
                return null;
            }
        }
        return workSpace;
    }

    public void writeText(final File save, final String text) {
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                try {
                    writeTextSync(save, text);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void writeTextSync(final File save, final String text) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(save));
        bufferedWriter.write(text);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public void readText(final File save, AsyncHttpClient.StringCallback callback) {
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = readTextSync(save);
                    callback.onCompleted(null, new LocalResponse(200), result);
                } catch (Exception e) {
                    callback.onCompleted(e, null, null);
                }
            }
        });
    }

    public String readTextSync(File save) throws IOException {
        if (!save.exists()) {
            throw new FileNotFoundException(String.format("%s not exists!", save.getAbsoluteFile()));
        }
        InputStreamReader isr = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            isr = new InputStreamReader(Files.newInputStream(save.toPath()), StandardCharsets.UTF_8);
        }else{
            isr = new InputStreamReader(new FileInputStream(save),StandardCharsets.UTF_8);
        }
        BufferedReader bufferedReader = new BufferedReader(isr);
        String text = null;
        StringBuilder stringBuilder = new StringBuilder();
        while ((text = bufferedReader.readLine()) != null) {
            stringBuilder.append(text).append("\n");
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }

    /**
     * 保存JSONObject到内部私有目录
     */
    public void saveJSONObject(Context context, JSONObject jsonObject, String name) {
        if (jsonObject == null)
            return;
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                File file = new File(context.getCacheDir(), name);
                try {
                    writeTextSync(file, jsonObject.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 从内部私有目录读取JSONObject
     */
    public void readJSONObject(Context context, String name, AsyncHttpClient.JSONObjectCallback callback) {
        File file = new File(context.getCacheDir(), name);
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = readTextSync(file);
                    JSONObject jsonObject = new JSONObject(result);
                    callback.onCompleted(null, new LocalResponse(200), jsonObject);
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onCompleted(e, new LocalResponse(404), null);
                }
            }
        });
    }
}
