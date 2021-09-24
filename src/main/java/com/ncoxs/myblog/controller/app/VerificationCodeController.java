package com.ncoxs.myblog.controller.app;


import com.ncoxs.myblog.handler.encryption.Encryption;
import com.ncoxs.myblog.model.bo.VerificationCode;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.service.app.VerificationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码控制器。
 */
@RestController
@RequestMapping("/app/verification-code")
public class VerificationCodeController {

    private VerificationCodeService verificationCodeService;

    @Autowired
    public void setVerificationCodeService(VerificationCodeService verificationCodeService) {
        this.verificationCodeService = verificationCodeService;
    }


    @GetMapping("/generate/plain")
    @Encryption
    public GenericResult<VerificationCode> generate(String type) {
        return GenericResult.success(verificationCodeService.generatePlainCode(type));
    }
}
