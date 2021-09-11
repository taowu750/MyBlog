package com.ncoxs.myblog.model.pojo;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.handler.response.FilterBlank;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 用户基本信息。
 */
@Data
public class UserBasicInfo {

    @FilterBlank
    private String note;

    @Min(value = ParamValidateRule.AGE_MIN, message = ParamValidateMsg.USER_AGE_RANGE_MIN)
    @Max(value = ParamValidateRule.AGE_MAX, message = ParamValidateMsg.USER_AGE_RANGE_MAX)
    @FilterBlank
    private Integer age;

    @Min(value = ParamValidateRule.SEX_MIN, message = ParamValidateMsg.USER_SEX_VALUE)
    @Max(value = ParamValidateRule.SEX_MAX, message = ParamValidateMsg.USER_SEX_VALUE)
    @FilterBlank
    private Byte sex;
}
