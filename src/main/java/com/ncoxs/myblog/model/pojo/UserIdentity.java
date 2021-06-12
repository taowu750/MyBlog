package com.ncoxs.myblog.model.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * user_identity
 * @author wutao
 */
@Data
public class UserIdentity implements Serializable {
    private Integer id;

    private Integer userId;

    private String identity;

    private Byte type;

    private String source;

    private Date expire;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}