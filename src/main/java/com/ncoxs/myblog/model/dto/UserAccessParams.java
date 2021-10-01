package com.ncoxs.myblog.model.dto;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.handler.validate.UserLoginToken;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserAccessParams {

    @NotBlank(message = ParamValidateMsg.USER_LOGIN_TOKEN_BLANK)
    @UserLoginToken
    private String userLoginToken;
}
