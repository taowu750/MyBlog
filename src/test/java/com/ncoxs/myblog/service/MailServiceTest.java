package com.ncoxs.myblog.service;

import com.ncoxs.myblog.constant.EmailTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class MailServiceTest {

    @Value("${spring.mail.username}")
    private String mailSender;

    @Autowired
    MailService mailService;

    @Test
    public void testSendTemplateMail() throws MessagingException, InterruptedException {
        Context context = new Context();
        context.setVariable("username", "test");
        context.setVariable("activateUrl", "test");
        mailService.sendTemplateEmail(EmailTemplate.USER_ACTIVATE_NAME, mailSender,
                "wutaoyx163@163.com", EmailTemplate.USER_ACTIVATE_SUBJECT, context);

        TimeUnit.SECONDS.sleep(10);
    }
}
