package com.ncoxs.myblog.constant;

public interface ParamValidateMsg {
    
    String USER_NAME_BLANK = "user.name.blank:用户名不能为空";
    String USER_NAME_FORMAT = "user.name.format:用户名格式错误";

    String USER_AGE_RANGE_MIN = "user.age.range:用户年龄不能小于 " + ParamValidateRule.AGE_MIN + " 岁";
    String USER_AGE_RANGE_MAX = "user.age.range:用户年龄不能大于 " + ParamValidateRule.AGE_MAX + " 岁";

    String USER_SEX_VALUE = "user.sex.value:用户性别值不合法";

    String USER_PASSWORD_BLANK = "user.password.blank:密码不能为空";
    String USER_PASSWORD_FORMAT = "user.password.format:密码格式错误";

    String USER_EMAIL_BLANK = "user.password.blank:邮箱不能为空";
    String USER_EMAIL_FORMAT = "user.email.format:邮箱格式错误";

    String USER_ACTIVATE_IDENTITY_BLANK = "user.activate_identity.blank:用户激活标识不能为空";

    String USER_IDENTITY_BLANK = "user.login_identity.blank:用户登录标识不能为空";
    String USER_IDENTITY_SOURCE_BLANK = "user.identity_source.blank:用户登录标识来源不能为空";

    String USER_LOGIN_REMEMBER_DAYS_MIN = "user.login_remember_days.range:用户登录状态保留天数不能小于 "
            + ParamValidateRule.LOGIN_REMEMBER_DAYS_MIN;
    String USER_LOGIN_REMEMBER_DAYS_MAX = "user.login_remember_days.range:用户登录状态保留天数不能大于 "
            + ParamValidateRule.LOGIN_REMEMBER_DAYS_MAX;

    String IP_FORMAT = "device.ip:客户端 IP 格式错误";
}
