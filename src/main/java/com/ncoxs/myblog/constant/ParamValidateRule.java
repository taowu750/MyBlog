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

    String IP_REGEX = "^(((\\d)|([1-9]\\d)|((1\\d\\d)(2[0-4]\\d)(25[0-5])))\\.){3}" +
            "((\\d)|([1-9]\\d)|((1\\d\\d)(2[0-4]\\d)(25[0-5])))$";

    int BLOG_TITLE_MIN_LEN = 1;
    int BLOG_TITLE_MAX_LEN = 50;
    int BLOG_CONTENT_MIN_LEN = 10;
    int BLOG_CONTENT_MAX_LEN = 30000;
    int BLOG_WORD_COUNT_MIN = 20;
    int BLOG_WORD_COUNT_MAX = 15000;
}
