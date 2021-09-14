package com.ncoxs.myblog.controller.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.user.UserLogType;
import com.ncoxs.myblog.handler.encryption.Encryption;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.IpLocInfo;
import com.ncoxs.myblog.model.dto.UserAccessParams;
import com.ncoxs.myblog.model.dto.UserAndIdentity;
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
    public GenericResult<Object> register(@RequestBody RegisterParams params)
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
    public GenericResult<UserAndIdentity> loginByName(@RequestBody LoginByNameParams loginByNameParams) throws JsonProcessingException {
        loginByNameParams.rememberDays = loginByNameParams.rememberDays != null ? loginByNameParams.rememberDays : 0;
        loginByNameParams.source = loginByNameParams.source != null ? loginByNameParams.source : "";
        UserAndIdentity userAndIdentity = userService.loginUserByName(loginByNameParams);
        return checkUserAndIdentity(userAndIdentity);
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
    public GenericResult<UserAndIdentity> loginByEmail(@RequestBody LoginByEmailParams loginByEmailParams) throws JsonProcessingException {
        loginByEmailParams.rememberDays = loginByEmailParams.rememberDays != null ? loginByEmailParams.rememberDays : 0;
        loginByEmailParams.source = loginByEmailParams.source != null ? loginByEmailParams.source : "";
        UserAndIdentity userAndIdentity = userService.loginUserByEmail(loginByEmailParams);
        return checkUserAndIdentity(userAndIdentity);
    }

    private GenericResult<UserAndIdentity> checkUserAndIdentity(UserAndIdentity userAndIdentity) {
        if (userAndIdentity == null) {
            return GenericResult.error(ResultCode.USER_NOT_EXIST);
        } else if (userAndIdentity.getUser() == null) {
            return GenericResult.error(ResultCode.USER_PASSWORD_ERROR);
        } else {
            return GenericResult.success(userAndIdentity);
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
    public GenericResult<User> loginByIdentity(@RequestBody LoginByIdentityParams params) throws JsonProcessingException {
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
    public GenericResult<Boolean> sendForgetPasswordEmail(@RequestBody SendForgetPasswordEmailParams params)
            throws MessagingException, GeneralSecurityException, UnsupportedEncodingException {
        return GenericResult.success(userService.sendForgetPasswordMail(params.email, params.newPassword));
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
            } else if (!userService.existsEmail(email)) {
                mv.addObject("result", "non-exists");
            } else {
                userService.setNewPassword(UserLogType.FORGET_PASSWORD, email, newPassword);
                mv.addObject("result", "success");
            }
        }
        mv.setViewName("/view/forget-password-result");

        return mv;
    }

    @Data
    public static class ModifyPasswordParams {

        @NotBlank(message = ParamValidateMsg.USER_EMAIL_BLANK)
        @Pattern(regexp = ParamValidateRule.EMAIL_REGEX, message = ParamValidateMsg.USER_EMAIL_FORMAT)
        public String email;

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        public String oldPassword;

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        public String newPassword;
    }

    @PostMapping("/password/modify")
    @ResponseBody
    public GenericResult<Boolean> modifyPassword(@RequestBody ModifyPasswordParams params) throws JsonProcessingException {
        return GenericResult.success(userService.modifyPassword(params));
    }

    @Data
    public static class ModifyNameParams {

        @NotBlank(message = ParamValidateMsg.USER_NAME_BLANK)
        @Pattern(regexp = ParamValidateRule.NAME_REGEX, message = ParamValidateMsg.USER_NAME_FORMAT)
        public String oldName;

        @NotBlank(message = ParamValidateMsg.USER_NAME_BLANK)
        @Pattern(regexp = ParamValidateRule.NAME_REGEX, message = ParamValidateMsg.USER_NAME_FORMAT)
        public String newName;

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        public String password;
    }

    @PostMapping("/name/modify")
    @ResponseBody
    public GenericResult<Boolean> modifyName(@RequestBody ModifyNameParams params) throws JsonProcessingException {
        return GenericResult.success(userService.modifyName(params));
    }

    @PostMapping("/account/cancel")
    @ResponseBody
    public GenericResult<Boolean> canceledAccount(@RequestBody UserAccessParams params) {
        return GenericResult.success(userService.canceledAccount(params.getEmail(), params.getPassword()));
    }
}
