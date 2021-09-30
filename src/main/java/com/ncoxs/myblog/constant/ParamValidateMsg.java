package com.ncoxs.myblog.constant;

public interface ParamValidateMsg {
    
    String USER_NAME_BLANK = "user.name.blank:用户名不能为空";
    String USER_NAME_FORMAT = "user.name.format:用户名格式错误";

    String USER_AGE_RANGE_MIN = "user.age.range:用户年龄不能小于 " + ParamValidateRule.AGE_MIN + " 岁";
    String USER_AGE_RANGE_MAX = "user.age.range:用户年龄不能大于 " + ParamValidateRule.AGE_MAX + " 岁";

    String USER_PASSWORD_BLANK = "user.password.blank:密码不能为空";
    String USER_PASSWORD_FORMAT = "user.password.format:密码格式错误";

    String USER_EMAIL_BLANK = "user.password.blank:邮箱不能为空";
    String USER_EMAIL_FORMAT = "user.email.format:邮箱格式错误";

    String USER_ACTIVATE_IDENTITY_BLANK = "user.activateIdentity.blank:用户激活标识不能为空";

    String USER_IDENTITY_BLANK = "user.loginIdentity.blank:用户登录标识不能为空";
    String USER_IDENTITY_SOURCE_BLANK = "user.identitySource.blank:用户登录标识来源不能为空";

    String USER_LOGIN_REMEMBER_DAYS_MIN = "user.loginRememberDays.range:用户登录状态保留天数不能小于 "
            + ParamValidateRule.LOGIN_REMEMBER_DAYS_MIN;
    String USER_LOGIN_REMEMBER_DAYS_MAX = "user.loginRememberDays.range:用户登录状态保留天数不能大于 "
            + ParamValidateRule.LOGIN_REMEMBER_DAYS_MAX;

    String USER_LOGIN_TOKEN_BLANK = "user.login.token.blank:用户登录 token 不能为空";

    String IP_FORMAT = "device.ip:客户端 IP 格式错误";

    String BLOG_TITLE_BLANK = "blog.title.blank:博客标题不能为空";
    String BLOG_TITLE_LEN = "blog.title.len:博客标题长度超出最大长度 " + ParamValidateRule.BLOG_TITLE_MAX_LEN;
    String BLOG_CONTENT_BLANK = "blog.content.blank:博客内容不能为空";
    String BLOG_CONTENT_LEN = "blog.content.len:博客内容长度超出最大长度限制 " + ParamValidateRule.BLOG_CONTENT_MAX_LEN;
    String BLOG_WORD_COUNT_RANGE = "blog.wordCount.range:博客字数应在 " + ParamValidateRule.BLOG_WORD_COUNT_MIN + " 到 " +
            ParamValidateRule.BLOG_WORD_COUNT_MAX + " 字之间";

    String VERIFICATION_CODE_BLANK = "verificationCode.blank:验证码不能为空";

    String UPLOAD_IMAGE_TARGET_TYPE_INVALID = "uploadImage.targetType.invalid:上传图片 targetType 不正确";
}
