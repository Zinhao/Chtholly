package com.zinhao.chtholly.network;

import java.io.File;

public interface ChatApi {
    boolean sendImageMessage(String imageKey)  throws Exception;
    boolean sendTextMessage(String text) throws Exception;
    String uploadImage(String imagePath) throws Exception;
    String uploadFile(File file) throws Exception;
    boolean sendFileMessage(String fileKey) throws Exception;
}
