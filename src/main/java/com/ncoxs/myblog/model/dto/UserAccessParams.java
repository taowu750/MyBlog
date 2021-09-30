package com.ncoxs.myblog.model.dto;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserAccessParams {

    @NotBlank(message = ParamValidateMsg.USER_LOGIN_TOKEN_BLANK)
    private String userLoginToken;
}
