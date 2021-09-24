package com.ncoxs.myblog.service.app;

import com.ncoxs.myblog.model.bo.VerificationCode;
import com.ncoxs.myblog.util.general.TimeUtil;
import com.ncoxs.myblog.util.general.UUIDUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
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

    private ConcurrentHashMap<String, VerificationCode> token2codeMap = new ConcurrentHashMap<>();

    /**
     * 生成普通的验证码。
     *
     * @param lastToken 用户刷新验证码时，上一个验证码的 token
     */
    public VerificationCode generatePlainCode(String lastToken, int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(WORDS[random.nextInt(WORDS.length)]);
        }

        return save(lastToken, sb.toString());
    }

    public VerificationCode generatePlainCode(String lastToken) {
        return generatePlainCode(lastToken, 4);
    }

    // TODO: 增加定时任务，删除过期验证码
    public boolean verify(String token, String code) {
        // 验证码是一次性的，验证完后就删除
        VerificationCode verificationCode = token2codeMap.remove(token);
        if (verificationCode == null || verificationCode.getExpireAt() < System.currentTimeMillis()) {
            return false;
        }

        return verificationCode.getCode().equals(code);
    }

    private VerificationCode save(String lastToken, String code) {
        String token = lastToken != null ? lastToken : UUIDUtil.generate();
        VerificationCode verificationCode = new VerificationCode(token, code,
                TimeUtil.changeDateTime(plainCodeExpire, TimeUnit.MINUTES).getTime());
        token2codeMap.put(token, verificationCode);

        return verificationCode;
    }
}
