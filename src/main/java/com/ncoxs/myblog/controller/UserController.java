package com.ncoxs.myblog.controller;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.constant.RequestAttributeConst;
import com.ncoxs.myblog.constant.ResultCode;
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
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

// TODO: 密码强度验证
@Controller
@RequestMapping("/user")
@Validated
public class UserController {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/register/{name}")
    @ResponseBody
    public GenericResult<Object> register(@RequestAttribute(RequestAttributeConst.REQUEST_FLOW_ID) int requestFlowId,
                                          User user) throws MessagingException {
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
    public ModelAndView accountActivate(@RequestAttribute(RequestAttributeConst.REQUEST_FLOW_ID) int requestFlowId,
                                        @PathVariable("identity")
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

    @PostMapping("/login/name/{name}")
    @ResponseBody
    public GenericResult<UserAndIdentity> loginByName(@RequestAttribute(RequestAttributeConst.REQUEST_FLOW_ID) int requestFlowId,
                                                      LoginByNameReq loginByNameReq) {
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

    @PostMapping("/login/email/{email}")
    @ResponseBody
    public GenericResult<UserAndIdentity> loginByEmail(@RequestAttribute(RequestAttributeConst.REQUEST_FLOW_ID) int requestFlowId,
                                                       LoginByEmailReq loginByEmailReq) {
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
}
