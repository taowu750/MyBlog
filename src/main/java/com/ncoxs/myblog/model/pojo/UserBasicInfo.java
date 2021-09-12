package com.ncoxs.myblog.model.pojo;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.handler.response.FilterBlank;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Date;

// TODO: 增加职业枚举值
/**
 * user_basic_info
 * @author 
 */
@Data
public class UserBasicInfo implements Serializable {

    private Integer userId;

    /**
     * 头像图片路径
     */
    private String profilePicturePath;

    /**
     * 用户自我介绍
     */
    private String description;

    /**
     * 性别：1 男，2 女
     */
    private Integer sex;

    @Min(value = ParamValidateRule.AGE_MIN, message = ParamValidateMsg.USER_AGE_RANGE_MIN)
    @Max(value = ParamValidateRule.AGE_MAX, message = ParamValidateMsg.USER_AGE_RANGE_MAX)
    private Integer age;

    @FilterBlank
    private Date birthday;

    private Integer profession;

    private Date createTime;

    private Date modifyTime;

    private static final long serialVersionUID = 1L;
}