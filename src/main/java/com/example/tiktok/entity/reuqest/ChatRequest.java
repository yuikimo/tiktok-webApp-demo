package com.example.tiktok.entity.reuqest;

public class ChatRequest {
    private String model;
    private String systemContent;
    private String userContent;

    public ChatRequest(String model, String systemContent, String userContent) {
        this.model = model;
        this.systemContent = systemContent;
        this.userContent = userContent;
    }

    public String getModel() {
        return model;
    }

    public String getSystemContent() {
        return systemContent;
    }

    public String getUserContent() {
        return userContent;
    }
}

