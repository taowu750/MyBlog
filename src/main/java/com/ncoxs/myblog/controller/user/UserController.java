package com.ncoxs.myblog.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.handler.encryption.Encryption;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.IpLocInfo;
import com.ncoxs.myblog.model.dto.UserLoginResp;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.user.UserService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.mail.MessagingException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

@Controller
@RequestMapping("/user")
@Validated
public class UserController {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Data
    public static class RegisterParams {

        public User user;

        public IpLocInfo ipLocInfo;

        @NotBlank(message = ParamValidateMsg.VERIFICATION_CODE_BLANK)
        public String verificationCode;
    }

    @PostMapping("/register")
    @ResponseBody
    @Encryption
    public GenericResult<?> register(@RequestBody RegisterParams params)
            throws MessagingException, JsonProcessingException {
        int exists = userService.existsUser(params.user.getName(), params.user.getEmail());
        ResultCode resultCode = ResultCode.SUCCESS;
        if (exists == 0 && (resultCode = userService.registerUser(params)) == ResultCode.SUCCESS) {
            return GenericResult.success();
        }

        switch (exists) {
            case 1:
            case 2:
            case 3:
                return GenericResult.error(ResultCode.USER_HAS_EXISTED);

            case 4:
                return GenericResult.error(ResultCode.USER_EMAIL_IS_BIND);

            default:
                return GenericResult.error(resultCode);
        }
    }

    @GetMapping("/account/activate/{identity}")
    public ModelAndView accountActivate(@PathVariable("identity")
                                        @NotBlank(message = ParamValidateMsg.USER_ACTIVATE_IDENTITY_BLANK)
                                                String identity) throws JsonProcessingException {
        User user = userService.activateUser(identity);
        ModelAndView modelAndView = new ModelAndView();
        if (user == UserService.USER_EXPIRED) {
            modelAndView.addObject("result", "expired");
        } else if (user == UserService.USER_ACTIVATED) {
            modelAndView.addObject("result", "activated");
        } else if (user == null) {
            modelAndView.addObject("result", "non-exist");
        } else {
            modelAndView.addObject("result", "success");
        }
        modelAndView.setViewName("view/user-activate-result");

        return modelAndView;
    }

    @Data
    public static class LoginParams {

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        public String password;

        // 记住多少天
        @Min(value = ParamValidateRule.LOGIN_REMEMBER_DAYS_MIN, message = ParamValidateMsg.USER_LOGIN_REMEMBER_DAYS_MIN)
        @Max(value = ParamValidateRule.LOGIN_REMEMBER_DAYS_MAX, message = ParamValidateMsg.USER_LOGIN_REMEMBER_DAYS_MAX)
        public Integer rememberDays;

        // 客户端标识
        public String source;

        public IpLocInfo ipLocInfo;

        @NotBlank(message = ParamValidateMsg.VERIFICATION_CODE_BLANK)
        public String verificationCode;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class LoginByNameParams extends LoginParams {

        @NotBlank(message = ParamValidateMsg.USER_NAME_BLANK)
        @Pattern(regexp = ParamValidateRule.NAME_REGEX, message = ParamValidateMsg.USER_NAME_FORMAT)
        public String name;
    }

    @PostMapping("/login/name")
    @ResponseBody
    @Encryption
    public GenericResult<UserLoginResp> loginByName(@RequestBody LoginByNameParams loginByNameParams) throws JsonProcessingException {
        loginByNameParams.rememberDays = loginByNameParams.rememberDays != null ? loginByNameParams.rememberDays : 0;
        loginByNameParams.source = loginByNameParams.source != null ? loginByNameParams.source : "";
        UserLoginResp userLoginResp = userService.loginUserByName(loginByNameParams);
        return checkUserLoginResp(userLoginResp);
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class LoginByEmailParams extends LoginParams {

        @NotBlank(message = ParamValidateMsg.USER_EMAIL_BLANK)
        @Pattern(regexp = ParamValidateRule.EMAIL_REGEX, message = ParamValidateMsg.USER_EMAIL_FORMAT)
        public String email;
    }

    @PostMapping("/login/email")
    @ResponseBody
    @Encryption
    public GenericResult<UserLoginResp> loginByEmail(@RequestBody LoginByEmailParams loginByEmailParams) throws JsonProcessingException {
        loginByEmailParams.rememberDays = loginByEmailParams.rememberDays != null ? loginByEmailParams.rememberDays : 0;
        loginByEmailParams.source = loginByEmailParams.source != null ? loginByEmailParams.source : "";
        UserLoginResp userLoginResp = userService.loginUserByEmail(loginByEmailParams);
        return checkUserLoginResp(userLoginResp);
    }

    private GenericResult<UserLoginResp> checkUserLoginResp(UserLoginResp userLoginResp) {
        if (userLoginResp == null) {
            return GenericResult.error(ResultCode.USER_NON_EXISTS);
        } else if (userLoginResp == UserService.PASSWORD_RETRY_ERROR) {
            return GenericResult.error(ResultCode.USER_PASSWORD_RETRY_ERROR);
        } else if (userLoginResp == UserService.VERIFICATION_CODE_ERROR) {
            return GenericResult.error(ResultCode.PARAMS_VERIFICATION_CODE_ERROR);
        } else if (userLoginResp == UserService.ALREADY_LOGIN) {
            return GenericResult.error(ResultCode.USER_ALREADY_LOGIN);
        } else if (userLoginResp.getUser() == null) {
            return GenericResult.error(ResultCode.USER_PASSWORD_ERROR);
        } else {
            return GenericResult.success(userLoginResp);
        }
    }

    @Data
    public static class LoginByIdentityParams {

        @NotBlank(message = ParamValidateMsg.USER_IDENTITY_BLANK)
        public String identity;

        @NotBlank(message = ParamValidateMsg.USER_IDENTITY_SOURCE_BLANK)
        public String source;

        public IpLocInfo ipLocInfo;
    }

    @PostMapping("/login/identity")
    @ResponseBody
    @Encryption
    public GenericResult<UserLoginResp> loginByIdentity(@RequestBody LoginByIdentityParams params) throws JsonProcessingException {
        return checkUserLoginResp(userService.loginByIdentity(params.identity, params.source, params.ipLocInfo));
    }

    @Data
    public static class LogoutParams {

        @NotBlank(message = ParamValidateMsg.USER_LOGIN_TOKEN_BLANK)
        public String token;

        public int logoutType;
    }

    @PostMapping("/logout")
    @ResponseBody
    public GenericResult<?> logout(@RequestBody LogoutParams params) throws JsonProcessingException {
        if (userService.quitByToken(params.token, params.logoutType)) {
            return GenericResult.success();
        } else {
            return GenericResult.error(ResultCode.USER_ACCESS_ERROR);
        }
    }

    @Data
    public static class SendForgetPasswordEmailParams {

        @NotBlank(message = ParamValidateMsg.USER_EMAIL_BLANK)
        @Pattern(regexp = ParamValidateRule.EMAIL_REGEX, message = ParamValidateMsg.USER_EMAIL_FORMAT)
        String email;

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        String newPassword;
    }

    @PostMapping("/password/send-forget")
    @ResponseBody
    public GenericResult<?> sendForgetPasswordEmail(@RequestBody SendForgetPasswordEmailParams params)
            throws MessagingException, GeneralSecurityException, UnsupportedEncodingException {
        return GenericResult.byCode(userService.sendForgetPasswordMail(params.email, params.newPassword));
    }

    @GetMapping("/password/forget/{encryptedParams}")
    public ModelAndView forgetPassword(@PathVariable("encryptedParams")
                                       @NotBlank String encryptedParams)
            throws GeneralSecurityException, UnsupportedEncodingException, JsonProcessingException {
        ModelAndView mv = new ModelAndView();
        String[] params = userService.decryptForgetPasswordParams(encryptedParams).split(" ");
        if (params.length != 3) {
            mv.addObject("result", "params-error");
        } else {
            String email = params[0], newPassword = params[1];
            long expire = Long.parseLong(params[2]);
            if (expire < System.currentTimeMillis()) {
                mv.addObject("result", "expired");
            } else if (userService.resetPassword(email, newPassword)) {
                mv.addObject("result", "success");
            } else {
                // email 不存在则失败
                mv.addObject("result", "failed");
            }
        }
        mv.setViewName("/view/forget-password-result");

        return mv;
    }

    @Data
    public static class ModifyPasswordParams {

        @NotBlank(message = ParamValidateMsg.USER_LOGIN_TOKEN_BLANK)
        public String token;

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        public String oldPassword;

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        public String newPassword;
    }

    @PostMapping("/password/modify")
    @ResponseBody
    public GenericResult<?> modifyPassword(@RequestBody ModifyPasswordParams params) throws JsonProcessingException {
        return GenericResult.byCode(userService.setNewPassword(params.token, params.oldPassword, params.newPassword));
    }

    @Data
    public static class AccessParams {

        @NotBlank(message = ParamValidateMsg.USER_LOGIN_TOKEN_BLANK)
        public String token;

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        public String password;
    }

    @Data
    public static class ModifyNameParams extends AccessParams {

        @NotBlank(message = ParamValidateMsg.USER_NAME_BLANK)
        @Pattern(regexp = ParamValidateRule.NAME_REGEX, message = ParamValidateMsg.USER_NAME_FORMAT)
        public String newName;
    }

    @PostMapping("/name/modify")
    @ResponseBody
    public GenericResult<?> modifyName(@RequestBody ModifyNameParams params) throws JsonProcessingException {
        return GenericResult.byCode(userService.modifyName(params));
    }

    @PostMapping("/verify/password")
    @ResponseBody
    public GenericResult<?> verifyPassword(@RequestBody AccessParams params) {
        return GenericResult.byCode(userService.verifyPassword(params.token, params.password));
    }

    @PostMapping("/account/send-cancel")
    @ResponseBody
    public GenericResult<?> sendCancelAccountEmail(@RequestBody AccessParams params)
            throws MessagingException, GeneralSecurityException, UnsupportedEncodingException {
        return GenericResult.byCode(userService.sendCancelAccountEmail(params.token, params.password));
    }

    @GetMapping("/account/cancel/{encryptedParams}")
    public ModelAndView cancelAccount(@PathVariable("encryptedParams") String encryptedParams)
            throws GeneralSecurityException, UnsupportedEncodingException, JsonProcessingException {
        ResultCode result = userService.cancelAccount(encryptedParams);
        ModelAndView mv = new ModelAndView();
        switch (result) {
            case PARAM_IS_INVALID:
                mv.addObject("result", "params-invalid");
                break;

            case USER_NON_EXISTS:
                mv.addObject("result", "user-non-exists");
                break;

            case USER_STATUS_INVALID:
                mv.addObject("result", "user-status-invalid");
                break;

            case PARAMS_EXPIRED:
                mv.addObject("result", "params-expired");
                break;

            case SUCCESS:
                mv.addObject("result", "success");
                break;
        }
        mv.setViewName("/view/user-account-cancel-result");

        return mv;
    }
}
