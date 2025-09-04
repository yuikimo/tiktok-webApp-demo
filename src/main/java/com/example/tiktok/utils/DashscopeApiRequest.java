package com.example.tiktok.utils;

import com.example.tiktok.entity.response.ChatResponse;
import com.example.tiktok.entity.reuqest.ChatRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DashscopeApiRequest {

    public static ChatResponse sendRequest(ChatRequest chatRequest) {
        try {
            // 设置请求的URL
            URL url = new URL("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 设置请求方法为POST
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + System.getenv("DASHSCOPE_API_KEY"));
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);  // 允许发送请求体

            // 根据输入的ChatRequest对象生成JSON数据
            String jsonInputString = "{\n" +
                                     "    \"model\": \"" + chatRequest.getModel() + "\",\n" +
                                     "    \"messages\": [\n" +
                                     "        {\n" +
                                     "            \"role\": \"system\",\n" +
                                     "            \"content\": \"" + chatRequest.getSystemContent() + "\"\n" +
                                     "        },\n" +
                                     "        {\n" +
                                     "            \"role\": \"user\", \n" +
                                     "            \"content\": \"" + chatRequest.getUserContent() + "\"\n" +
                                     "        }\n" +
                                     "    ]\n" +
                                     "}";

            // 将请求体发送到服务器
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 获取响应码
            int responseCode = conn.getResponseCode();

            // 读取响应内容
            BufferedReader br =
                    new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            // 封装响应到ChatResponse对象并返回
            return new ChatResponse(responseCode, response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse(500, "Error: " + e.getMessage());  // 出现异常时返回错误信息
        }
    }
}
