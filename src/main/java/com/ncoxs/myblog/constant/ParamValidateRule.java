package com.ncoxs.myblog.constant;

public interface ParamValidateRule {

    String NAME_REGEX = "^[a-zA-Z0-9_\\-\\u4e00-\\u9fa5]{3,12}$";

    String PASSWORD_REGEX = "^[\\u0021-\\u007e]{5,18}$";

    String EMAIL_REGEX = "^[a-zA-Z0-9]+@[a-zA-Z0-9\\-]+(\\.[a-zA-Z0-9\\-]+)+$";

    int AGE_MIN = 1;
    int AGE_MAX = 150;

    int SEX_MIN = 1;
    int SEX_MAX = 2;

    int LOGIN_REMEMBER_DAYS_MIN = 0;
    int LOGIN_REMEMBER_DAYS_MAX = 30;
}
