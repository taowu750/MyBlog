package com.ncoxs.myblog.controller;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.handler.encryption.Encryption;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserAndIdentity;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.UserService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.mail.MessagingException;
import javax.validation.constraints.*;
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


    @PostMapping("/register")
    @ResponseBody
    @Encryption
    public GenericResult<Object> register(User user) throws MessagingException {
        int exists = userService.existsUser(user.getName(), user.getEmail());
        if (exists == 0 && userService.registerUser(user)) {
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

    @GetMapping("/account-activate/{identity}")
    public ModelAndView accountActivate(@PathVariable("identity")
                                        @NotBlank(message = ParamValidateMsg.USER_ACTIVATE_IDENTITY_BLANK)
                                                String identity) {
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
    public static class LoginByNameReq {

        @NotBlank(message = ParamValidateMsg.USER_NAME_BLANK)
        @Pattern(regexp = ParamValidateRule.NAME_REGEX, message = ParamValidateMsg.USER_NAME_FORMAT)
        public String name;

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        public String password;

        @Min(value = ParamValidateRule.LOGIN_REMEMBER_DAYS_MIN, message = ParamValidateMsg.USER_LOGIN_REMEMBER_DAYS_MIN)
        @Max(value = ParamValidateRule.LOGIN_REMEMBER_DAYS_MAX, message = ParamValidateMsg.USER_LOGIN_REMEMBER_DAYS_MAX)
        public Integer rememberDays;

        public String source;
    }

    @PostMapping("/login/name")
    @ResponseBody
    @Encryption
    public GenericResult<UserAndIdentity> loginByName(LoginByNameReq loginByNameReq) {
        loginByNameReq.rememberDays = loginByNameReq.rememberDays != null ? loginByNameReq.rememberDays : 0;
        loginByNameReq.source = loginByNameReq.source != null ? loginByNameReq.source : "";
        UserAndIdentity userAndIdentity = userService.loginUserByName(loginByNameReq.name, loginByNameReq.password,
                loginByNameReq.rememberDays, loginByNameReq.source);
        return checkUserAndIdentity(userAndIdentity);
    }

    @Data
    public static class LoginByEmailReq {

        @NotBlank(message = ParamValidateMsg.USER_EMAIL_BLANK)
        @Pattern(regexp = ParamValidateRule.EMAIL_REGEX, message = ParamValidateMsg.USER_EMAIL_FORMAT)
        public String email;

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        public String password;

        @Min(value = ParamValidateRule.LOGIN_REMEMBER_DAYS_MIN, message = ParamValidateMsg.USER_LOGIN_REMEMBER_DAYS_MIN)
        @Max(value = ParamValidateRule.LOGIN_REMEMBER_DAYS_MAX, message = ParamValidateMsg.USER_LOGIN_REMEMBER_DAYS_MAX)
        public Integer rememberDays;

        public String source;
    }

    @PostMapping("/login/email")
    @ResponseBody
    @Encryption
    public GenericResult<UserAndIdentity> loginByEmail(LoginByEmailReq loginByEmailReq) {
        loginByEmailReq.rememberDays = loginByEmailReq.rememberDays != null ? loginByEmailReq.rememberDays : 0;
        loginByEmailReq.source = loginByEmailReq.source != null ? loginByEmailReq.source : "";
        UserAndIdentity userAndIdentity = userService.loginUserByEmail(loginByEmailReq.email, loginByEmailReq.password,
                loginByEmailReq.rememberDays, loginByEmailReq.source);
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

    @PostMapping("/login/identity")
    @ResponseBody
    @Encryption
    public GenericResult<User> loginByIdentity(@NotNull String identity,
                                               @NotNull String source) {
        return GenericResult.success(userService.loginByIdentity(identity, source));
    }

    @PostMapping("/password/send-forget")
    @ResponseBody
    @Encryption
    public GenericResult<Boolean> sendForgetPasswordEmail(@NotBlank(message = ParamValidateMsg.USER_EMAIL_BLANK)
                                                          @Pattern(regexp = ParamValidateRule.EMAIL_REGEX, message = ParamValidateMsg.USER_EMAIL_FORMAT)
                                                                  String email,
                                                          @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
                                                          @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
                                                                  String newPassword)
            throws MessagingException, GeneralSecurityException, UnsupportedEncodingException {
        return GenericResult.success(userService.sendForgetPasswordMail(email, newPassword));
    }

    @PostMapping("/password/forget/{encryptedParams}")
    public ModelAndView forgetPassword(@PathVariable("encryptedParams")
                                       @NotBlank String encryptedParams) throws GeneralSecurityException, UnsupportedEncodingException {
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
                userService.setNewPassword(email, newPassword);
                mv.addObject("result", "success");
            }
        }
        mv.setViewName("/view/forget-password-result");

        return mv;
    }
}
