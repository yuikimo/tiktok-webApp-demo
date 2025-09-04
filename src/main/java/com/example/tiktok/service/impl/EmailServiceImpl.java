package com.example.tiktok.service.impl;

import com.example.tiktok.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private SimpleMailMessage simpleMailMessage;

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    String fromName;

    @Override
    @Async
    public void send (String email, String context) {
        simpleMailMessage.setSubject("TikTok");
        simpleMailMessage.setFrom(fromName);
        simpleMailMessage.setTo(email);
        simpleMailMessage.setText(context);
        javaMailSender.send(simpleMailMessage);
    }
}
