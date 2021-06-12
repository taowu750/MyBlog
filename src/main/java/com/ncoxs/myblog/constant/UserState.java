package com.ncoxs.myblog.constant;

import lombok.Getter;

@Getter
public enum UserState {
    NORMAL(1, "账号正常"),
    NOT_ACTIVATED(2, "未激活"),
    FORBIDDEN(3, "被封禁"),
    CANCELLED(4, "已注销");

    private int state;
    private String stateNote;

    UserState(int state, String stateNote) {
        this.state = state;
        this.stateNote = stateNote;
    }

    public boolean is(int state) {
        return this.state == state;
    }
}
