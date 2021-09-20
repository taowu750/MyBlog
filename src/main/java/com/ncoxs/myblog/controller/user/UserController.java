package com.ncoxs.myblog.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.user.UserLogType;
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
    }

    @PostMapping("/register")
    @ResponseBody
    @Encryption
    public GenericResult<?> register(@RequestBody RegisterParams params)
            throws MessagingException, JsonProcessingException {
        int exists = userService.existsUser(params.user.getName(), params.user.getEmail());
        if (exists == 0 && userService.registerUser(params.user, params.ipLocInfo)) {
            return GenericResult.success();
        }

        switch (exists) {
            case 1:
            case 2:
            case 3:
                return GenericResult.error(ResultCode.USER_HAS_EXISTED);

            case 4:
                return GenericResult.error(ResultCode.USER_EMAIL_IS_BIND);

            case 5:
            default:
                return GenericResult.error(ResultCode.PARAM_NOT_COMPLETE);
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
        return checkUserAndIdentity(userLoginResp);
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
        return checkUserAndIdentity(userLoginResp);
    }

    private GenericResult<UserLoginResp> checkUserAndIdentity(UserLoginResp userLoginResp) {
        if (userLoginResp == null) {
            return GenericResult.error(ResultCode.USER_NOT_EXIST);
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
        return GenericResult.success(userService.loginByIdentity(params.identity, params.source, params.ipLocInfo));
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
            String token = params[0], newPassword = params[1];
            long expire = Long.parseLong(params[2]);
            if (expire < System.currentTimeMillis()) {
                mv.addObject("result", "expired");
                // 已过期，删除用户 token
                userService.quitByToken(token);
            } else if (userService.setNewPassword(UserLogType.FORGET_PASSWORD, token, newPassword) == ResultCode.SUCCESS) {
                mv.addObject("result", "success");
            } else {
                // token 不存在或新旧密码相同则失败
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
        public String newPassword;
    }

    @PostMapping("/password/modify")
    @ResponseBody
    public GenericResult<?> modifyPassword(@RequestBody ModifyPasswordParams params) throws JsonProcessingException {
        return GenericResult.byCode(userService.setNewPassword(UserLogType.MODIFY_PASSWORD, params.token, params.newPassword));
    }

    @Data
    public static class ModifyNameParams {

        @NotBlank(message = ParamValidateMsg.USER_LOGIN_TOKEN_BLANK)
        public String token;

        @NotBlank(message = ParamValidateMsg.USER_NAME_BLANK)
        @Pattern(regexp = ParamValidateRule.NAME_REGEX, message = ParamValidateMsg.USER_NAME_FORMAT)
        public String newName;
    }

    @PostMapping("/name/modify")
    @ResponseBody
    public GenericResult<?> modifyName(@RequestBody ModifyNameParams params) throws JsonProcessingException {
        return GenericResult.byCode(userService.modifyName(params));
    }

    @PostMapping("/account/cancel")
    @ResponseBody
    public GenericResult<?> canceledAccount(@RequestBody String token) {
        return GenericResult.byCode(userService.canceledAccount(token));
    }
}
