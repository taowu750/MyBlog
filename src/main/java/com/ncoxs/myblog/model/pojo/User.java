package com.ncoxs.myblog.model.pojo;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.exception.ImpossibleError;
import com.ncoxs.myblog.handler.response.FilterBlank;
import lombok.Data;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Date;

/**
 * user
 * @author wutao
 */
@Data
@FilterBlank
public class User implements Serializable, Cloneable {

    private static final long serialVersionUID = -3923796704508413980L;

    private Integer id;

    /*
    身份信息
     */
    @NotBlank(message = ParamValidateMsg.USER_NAME_BLANK)
    @Pattern(regexp = ParamValidateRule.NAME_REGEX, message = ParamValidateMsg.USER_NAME_FORMAT)
    private String name;

    private Integer type;

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
    private Integer status;

    @FilterBlank
    private Date limitTime;

    /*
    时间信息
     */
    private Date createTime;
    private Date modifyTime;


    @Override
    public User clone() {
        try {
            return (User) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new ImpossibleError(e);
        }
    }
}