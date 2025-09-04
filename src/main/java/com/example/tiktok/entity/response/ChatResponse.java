package com.example.tiktok.entity.response;

public class ChatResponse {
    private int responseCode;
    private String responseBody;

    public ChatResponse(int responseCode, String responseBody) {
        this.responseCode = responseCode;
        this.responseBody = responseBody;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
               "responseCode=" + responseCode +
               ", responseBody='" + responseBody + '\'' +
               '}';
    }
}

