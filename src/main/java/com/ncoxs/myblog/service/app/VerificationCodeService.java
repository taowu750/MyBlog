package com.ncoxs.myblog.service.app;

import com.ncoxs.myblog.model.bo.VerificationCode;
import com.ncoxs.myblog.util.general.SpringUtil;
import com.ncoxs.myblog.util.general.TimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 */
@Service
public class VerificationCodeService {

    @Value("${myapp.verification-code.plain.expireMinutes}")
    private int plainCodeExpire;


    private static final char[] DIGITS = "0123456789".toCharArray();
    private static final char[] WORDS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static final String SESSION_KEY_PLAIN_REGISTER = "plainRegisterVerificationCode";
    public static final String SESSION_KEY_PLAIN_LOGIN = "plainLoginVerificationCode";

    /**
     * 生成普通的验证码。
     */
    public VerificationCode generatePlainCode(String type, int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(WORDS[random.nextInt(WORDS.length)]);
        }

        return save(type, sb.toString());
    }

    public VerificationCode generatePlainCode(String type) {
        return generatePlainCode(type, 4);
    }

    public boolean verify(String type, String code) {
        HttpSession session = SpringUtil.currentSession();
        VerificationCode verificationCode = (VerificationCode) session.getAttribute(type);
        // 验证码是一次性的，验证完后就删除
        session.removeAttribute(type);
        if (verificationCode == null || verificationCode.getExpireAt() < System.currentTimeMillis()) {
            return false;
        }

        return verificationCode.getCode().equals(code);
    }

    private VerificationCode save(String type, String code) {
        VerificationCode verificationCode = new VerificationCode(code,
                TimeUtil.changeDateTime(plainCodeExpire, TimeUnit.MINUTES).getTime());
        SpringUtil.currentSession().setAttribute(type, verificationCode);

        return verificationCode;
    }
}
