package com.ncoxs.myblog.model.pojo;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.handler.response.FilterBlank;
import lombok.Data;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Date;

// TODO: 以后可能要加入用户登录表等用户相关信息记录表
/**
 * user
 * @author wutao
 */
@Data
@FilterBlank
public class User implements Serializable {

    private static final long serialVersionUID = -3923796704508413980L;

    private Integer id;

    /*
    个人信息
     */
    @NotBlank(message = ParamValidateMsg.USER_NAME_BLANK)
    @Pattern(regexp = ParamValidateRule.NAME_REGEX, message = ParamValidateMsg.USER_NAME_FORMAT)
    private String name;

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

    /*
    身份信息
     */
    @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
    @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
    @FilterBlank(alwaysNull = true)
    private String password;

    @Email(message = ParamValidateMsg.USER_EMAIL_FORMAT, regexp = ParamValidateRule.EMAIL_REGEX)
    private String email;

    @FilterBlank(alwaysNull = true)
    private String salt;

    /*
    账户状态信息
     */
    private Integer state;

    private String stateNote;

    @FilterBlank
    private Date limitTime;

    /*
    时间信息
     */
    private Date createTime;
    private Date modifyTime;
}