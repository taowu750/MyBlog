package com.ncoxs.myblog.model.dto;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class UserAccessParams {

    @NotBlank(message = ParamValidateMsg.USER_EMAIL_BLANK)
    @Pattern(regexp = ParamValidateRule.EMAIL_REGEX, message = ParamValidateMsg.USER_EMAIL_FORMAT)
    private String email;

    @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
    @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
    private String password;
}
