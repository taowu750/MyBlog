package com.ncoxs.myblog.model.dto;

import com.ncoxs.myblog.model.pojo.User;
import lombok.Data;

@Data
public class UserAndIdentity {

    private User user;
    private String identity;
}
