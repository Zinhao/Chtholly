package com.zinhao.chtholly.network;

import android.util.Log;
import com.google.android.material.timepicker.TimeFormat;
import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.enums.AppType;
import com.lark.oapi.core.enums.BaseUrlEnum;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.service.contact.v3.model.BatchGetIdUserReq;
import com.lark.oapi.service.contact.v3.model.BatchGetIdUserReqBody;
import com.lark.oapi.service.contact.v3.model.BatchGetIdUserResp;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.*;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.network.feishu.OpenIdResult;
import com.zinhao.chtholly.utils.FileLogger;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FeiShuApi implements ChatApi{
    private static final String TAG = "FeiShuApi";

    public static final String FEI_SHU_PACKAGE = "com.lark.oapi";
    private final Client client;
    private final com.lark.oapi.ws.Client eventClient;
    private String receiveOpenId = "";
    private HashMap<String, EventMessage> messageMap = new HashMap<>();
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    public FeiShuApi(String appId, String appSecret) {
        receiveOpenId = BotApp.getInstance().getFeishuReceiveOpenid();
        client = Client.newBuilder(appId, appSecret) // 默认配置为自建应用
                .appType(AppType.SELF_BUILT)
                .openBaseUrl(BaseUrlEnum.FeiShu) // 设置域名，默认为飞书
                .requestTimeout(3, TimeUnit.SECONDS) // 设置httpclient 超时时间，默认永不超时
                .logReqAtDebug(true) // 在 debug 模式下会打印 http 请求和响应的 headers、body 等信息。
                .build();
        // 构建 client Build client
        eventClient = new com.lark.oapi.ws.Client.Builder(appId, appSecret)
                .eventHandler(eventHandler)
                .build();
        // 建立长连接 Establish persistent connection
        eventClient.start();
    }

    private String waitOpenId =  "";
    private String waitCode = "";

    // 注册事件 Register event
    private final EventDispatcher eventHandler = EventDispatcher.newBuilder("", "")
            .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                @Override
                public void handle(P2MessageReceiveV1 event) throws Exception {
                    FileLogger.INSTANCE.i(TAG,"handle: "+ Jsons.DEFAULT.toJson(event.getEvent()));
                    String sendOpenId = event.getEvent().getSender().getSenderId().getOpenId();
                    if(receiveOpenId.isEmpty()){
                        sendWaitCode(sendOpenId);
                    }else{
                        if(receiveOpenId.equals(sendOpenId)){
                            if(NekoChatService.getInstance()!=null){
                                EventMessage eventMessage = event.getEvent().getMessage();
                                String messageId = eventMessage.getMessageId();
                                if(messageMap.containsKey(messageId)){
                                    FileLogger.INSTANCE.d(TAG,"repeat message:[ "+messageId + " ]: "+eventMessage.getContent());
                                }else {
                                    FileLogger.INSTANCE.d(TAG,"new message:[ "+messageId + " ]: "+eventMessage.getContent());
                                    boolean isNearSend = false;
                                    try {
                                        String t = eventMessage.getCreateTime();
                                        long time = Long.parseLong(t);
                                        FileLogger.INSTANCE.d(TAG,"create time:[ "+ dateTimeFormat.format(time) + " ]: "+eventMessage.getContent());
                                        isNearSend = Math.abs(System.currentTimeMillis() - time) < 10000;
                                    }catch (Exception e){
                                        isNearSend = true;
                                    }
                                    if(isNearSend){
                                        Message m = new Message(
                                                BotApp.getInstance().getAdminName(),
                                                eventMessage.getContent(),
                                                System.currentTimeMillis());
                                        m.setEnableCommand(true);
                                        NekoChatService.getInstance().onFindFeiShuMessage(m);
                                        messageMap.put(messageId,eventMessage);
                                    }
                                }
                            }
                        }else {
                            sendWaitCode(sendOpenId);
                        }
                    }


                }
            }).build();

    private void sendWaitCode(String sendOpenId) throws Exception {
        String waitCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        boolean sendResult = sendTextMessage("未验证客户端,请输入 "+waitCode+" 验证",sendOpenId);
        if(sendResult){
            this.waitCode = waitCode;
            waitOpenId = sendOpenId;
        }
    }

    public boolean inputWaitCode(String code){
        if(waitCode == null || code.isEmpty()){
            return false;
        }
        if(code.equals(waitCode)){
            this.receiveOpenId = waitOpenId;
            BotApp.getInstance().setFeishuReceiveOpenid(waitOpenId);
            return true;
        }else{
            waitCode = "";
            waitOpenId = "";
            return false;
        }
    }


    @Override
    public boolean sendImageMessage(String imageKey) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("image_key",imageKey);
        // 创建请求对象
        CreateMessageReq req = CreateMessageReq.newBuilder()
                .receiveIdType("open_id")
                .createMessageReqBody(CreateMessageReqBody.newBuilder()
                        .receiveId(receiveOpenId)
                        .msgType("image")
                        .content(jsonObject.toString())
                        .uuid(UUID.randomUUID().toString())
                        .build())
                .build();

        // 发起请求
        CreateMessageResp resp = client.im().v1().message().create(req);

        // 处理服务端错误
        if (!resp.success()) {
            FileLogger.INSTANCE.d(TAG,String.format("code:%s,msg:%s,reqId:%s, resp:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId(),
                    Jsons.createGSON(true, false).toJson(
                            JsonParser.parseString(
                                    new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)))));
            return false;
        }

        // 业务数据处理
        FileLogger.INSTANCE.d(TAG,Jsons.DEFAULT.toJson(resp.getData()));
        return true;
    }

    @Override
    public boolean sendTextMessage(String text) throws Exception{
        return sendTextMessage(text,receiveOpenId);
    }

    private boolean sendTextMessage(String text,String receiveOpenId) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("text",text);
        // 创建请求对象
        CreateMessageReq req = CreateMessageReq.newBuilder()
                .receiveIdType("open_id")
                .createMessageReqBody(CreateMessageReqBody.newBuilder()
                        .receiveId(receiveOpenId)
                        .msgType("text")
                        .content(jsonObject.toString())
                        .uuid(UUID.randomUUID().toString())
                        .build())
                .build();

        // 发起请求
        CreateMessageResp resp = client.im().v1().message().create(req);

        // 处理服务端错误
        if (!resp.success()) {
            FileLogger.INSTANCE.d(TAG,String.format("code:%s,msg:%s,reqId:%s, resp:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId(),
                    Jsons.createGSON(true, false).toJson(
                            JsonParser.parseString(
                                    new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)))));
            return false;
        }

        // 业务数据处理
        FileLogger.INSTANCE.d(TAG,Jsons.DEFAULT.toJson(resp.getData()));
        return true;
    }

    @Override
    public String uploadImage(String imagePath) throws Exception {
        // 创建请求对象

        File file = new File(imagePath);
        CreateImageReq req = CreateImageReq.newBuilder()
                .createImageReqBody(CreateImageReqBody.newBuilder()
                        .imageType("message")
                        .image(file)
                        .build())
                .build();

        // 发起请求
        CreateImageResp resp = client.im().v1().image().create(req);

        // 处理服务端错误
        if(!resp.success()) {
            FileLogger.INSTANCE.d(TAG,String.format("code:%s,msg:%s,reqId:%s, resp:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId(), Jsons.createGSON(true, false).toJson(JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)))));
            return null;
        }

        // 业务数据处理
        ///  {
        ///     "image_key": "img_v3_02vn_dfb29f05-f3d4-4d84-ad85-7f69829de7bg"
        ///   }
        String jsonStr = Jsons.DEFAULT.toJson(resp.getData());
        JSONObject jsonObject = new JSONObject(jsonStr);
        FileLogger.INSTANCE.d(TAG,jsonStr);
        return jsonObject.getString("image_key");
    }

    @Override
    public String uploadFile(File file) throws Exception {
        // 创建请求对象
        CreateFileReq req = CreateFileReq.newBuilder()
                .createFileReqBody(CreateFileReqBody.newBuilder()
                        .file(file)
                        .fileType("mp4")
                        .fileName(file.getName())
//                        .duration()
                        .build())
                .build();

        // 发起请求
        CreateFileResp resp = client.im().v1().file().create(req);

        // 处理服务端错误
        if(!resp.success()) {
            FileLogger.INSTANCE.d(TAG,String.format("code:%s,msg:%s,reqId:%s, resp:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId(), Jsons.createGSON(true, false).toJson(JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)))));
            return null;
        }

        // 业务数据处理
        ///  {"file_key":"file_v3_00vn_1a7f578d-0a43-4d8f-b556-818f12b63c3g"}
        String jsonStr = Jsons.DEFAULT.toJson(resp.getData());
        JSONObject jsonObject = new JSONObject(jsonStr);
        FileLogger.INSTANCE.d(TAG,jsonStr);
        return jsonObject.getString("file_key");

    }

    @Override
    public boolean sendFileMessage(String fileKey) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("file_key",fileKey);
        // 创建请求对象
        CreateMessageReq req = CreateMessageReq.newBuilder()
                .receiveIdType("open_id")
                .createMessageReqBody(CreateMessageReqBody.newBuilder()
                        .receiveId(receiveOpenId)
                        .msgType("media")
                        .content(jsonObject.toString())
                        .uuid(UUID.randomUUID().toString())
                        .build())
                .build();

        // 发起请求
        CreateMessageResp resp = client.im().v1().message().create(req);

        // 处理服务端错误
        if (!resp.success()) {
            FileLogger.INSTANCE.d(TAG,String.format("code:%s,msg:%s,reqId:%s, resp:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId(),
                    Jsons.createGSON(true, false).toJson(
                            JsonParser.parseString(
                                    new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)))));
            return false;
        }

        // 业务数据处理
        FileLogger.INSTANCE.d(TAG,Jsons.DEFAULT.toJson(resp.getData()));
        return true;
    }

    public String getUserReceiveOpenId(String phoneNumber) throws Exception {
        // 创建请求对象
        BatchGetIdUserReq req = BatchGetIdUserReq.newBuilder()
                .userIdType("open_id")
                .batchGetIdUserReqBody(BatchGetIdUserReqBody.newBuilder()
                        .mobiles(new String[] {
                                phoneNumber
                        })
                        .includeResigned(true)
                        .build())
                .build();

        // 发起请求
        BatchGetIdUserResp resp = client.contact().v3().user().batchGetId(req);

        // 处理服务端错误
        if(!resp.success()) {
            System.out.println(String.format("code:%s,msg:%s,reqId:%s, resp:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId(),
                    Jsons.createGSON(true, false).toJson(JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)))));
            return null;
        }
        String jsonStr = Jsons.DEFAULT.toJson(resp.getData());
        OpenIdResult openIdResult = Jsons.DEFAULT.fromJson(jsonStr, OpenIdResult.class);
        // 业务数据处理
        if(openIdResult.getData().getUser_list().isEmpty()){
            return null;
        }
        return  openIdResult.getData().getUser_list().get(0).getUser_id();
    }
}
