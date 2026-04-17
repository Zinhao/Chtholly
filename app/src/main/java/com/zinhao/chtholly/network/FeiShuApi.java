package com.zinhao.chtholly.network;

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
import com.lark.oapi.service.im.v1.model.CreateFileReq;
import com.lark.oapi.service.im.v1.model.CreateFileReqBody;
import com.lark.oapi.service.im.v1.model.CreateFileResp;
import com.lark.oapi.service.im.v1.model.CreateImageReq;
import com.lark.oapi.service.im.v1.model.CreateImageReqBody;
import com.lark.oapi.service.im.v1.model.CreateImageResp;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.zinhao.chtholly.network.feishu.OpenIdResult;
import com.zinhao.chtholly.utils.FileLogger;

import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FeiShuApi implements ChatApi{
    private static final String TAG = "FeiShuApi";


    private final Client client;
    private final com.lark.oapi.ws.Client eventClient;
    private String receiveOpenId = "";

    public void setReceiveOpenId(String receiveOpenId) {
        this.receiveOpenId = receiveOpenId;
    }

    public FeiShuApi(String appId, String appSecret) {
        client = Client.newBuilder(appId, appSecret) // 默认配置为自建应用
                .appType(AppType.SELF_BUILT)
                .openBaseUrl(BaseUrlEnum.FeiShu) // 设置域名，默认为飞书
                .requestTimeout(3, TimeUnit.SECONDS) // 设置httpclient 超时时间，默认永不超时
                .logReqAtDebug(true) // 在 debug 模式下会打印 http 请求和响应的 headers、body 等信息。
                .build();
        // 构建 client Build client
        eventClient = new com.lark.oapi.ws.Client.Builder(appId, appSecret)
                .eventHandler(EVENT_HANDLER)
                .build();
        // 建立长连接 Establish persistent connection
        eventClient.start();
    }

    // 注册事件 Register event
    private static final EventDispatcher EVENT_HANDLER = EventDispatcher.newBuilder("", "")
            .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                @Override
                public void handle(P2MessageReceiveV1 event) throws Exception {
                    FileLogger.INSTANCE.i("[ onP2MessageReceiveV1 access ], data: %s\n", Jsons.DEFAULT.toJson(event.getEvent()));
                }
            })
            .build();


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
    public boolean sendTextMessage(String text) throws Exception {
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
