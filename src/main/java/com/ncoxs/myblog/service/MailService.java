package com.ncoxs.myblog.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class MailService {

    private JavaMailSender javaMailSender;
    private TemplateEngine templateEngine;

    @Autowired
    public void setJavaMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Autowired
    public void setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * 发生模板邮件。
     */
    public void sendTemplateEmail(String templateName, String from, String to,
                                  String subject, Context context) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        // 解析邮件模板
        String text = templateEngine.process("mail/" + templateName, context);
        helper.setText(text, true);
        javaMailSender.send(message);
    }

    public void sendTemplateEmail(String templateName, String from, String to, String subject) throws MessagingException {
        sendTemplateEmail(templateName, from, to, subject, new Context());
    }
}
