package com.company.attendance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailService {
    private final ObjectProvider<JavaMailSender> sender;
    @Value("${app.mail.from:attendance@company.com}")
    private String from;

    public MailService(ObjectProvider<JavaMailSender> sender) { this.sender = sender; }

    /** Never throws: a mail problem must not break attendance or approvals. */
    public void send(String to, String subject, String body) {
        JavaMailSender s = sender.getIfAvailable();
        if (s == null || to == null || to.isBlank()) { log.warn("Mail not sent to '{}' (mail not configured)", to); return; }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from); m.setTo(to); m.setSubject(subject); m.setText(body);
            s.send(m);
            log.info("Mail sent to {} : {}", to, subject);
        } catch (Exception ex) {
            log.warn("Mail to {} failed: {}", to, ex.getMessage());
        }
    }
}
