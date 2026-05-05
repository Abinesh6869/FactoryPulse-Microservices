package org.cts.fp_identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendResetEmail(String to,String token) {
        String link = "http://localhost:5173/email/reset?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Factory Pulse Password Reset");
        message.setText("Click here to reset password: " + link + " Only valid for 15 minutes.");
        mailSender.send(message);
    }
}
