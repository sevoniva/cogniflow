package com.chatbi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 邮件推送服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailPushService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    /**
     * 发送邮件推送
     * @param to 收件人邮箱
     * @param title 邮件标题
     * @param content 邮件内容
     */
    public void push(String to, String title, String content) {
        if (!StringUtils.hasText(to)) {
            throw new IllegalArgumentException("收件人邮箱不能为空");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("邮件服务未启用，请配置 spring.mail 后再使用 EMAIL 推送");
        }
        String subject = StringUtils.hasText(title) ? title : "ChatBI 订阅推送";
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件推送成功：{} - {}", to, subject);
        } catch (MailException ex) {
            throw new IllegalStateException("邮件推送失败: " + ex.getMessage(), ex);
        }
    }
}
