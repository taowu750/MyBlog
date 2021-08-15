package com.ncoxs.myblog.model.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户行为状态日志表。
 */
@Data
@NoArgsConstructor
public class UserLog implements Serializable {
    private Integer id;

    private Integer userId;

    private Integer type;

    private String description;

    private Date createTime;

    private static final long serialVersionUID = 1L;

    public UserLog(Integer userId, Integer type, String description) {
        this.userId = userId;
        this.type = type;
        this.description = description;
    }
}