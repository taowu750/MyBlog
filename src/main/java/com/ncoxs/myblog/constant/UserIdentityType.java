package com.ncoxs.myblog.constant;

import lombok.Getter;

@Getter
public enum UserIdentityType {
    ACTIVATE_IDENTITY((byte) 1, "用户激活标识"),
    LOGIN_IDENTITY((byte) 2, "用户登录标识");

    private byte type;
    private String description;

    UserIdentityType(byte type, String description) {
        this.type = type;
        this.description = description;
    }

    public boolean is(byte type) {
        return this.type == type;
    }
}
