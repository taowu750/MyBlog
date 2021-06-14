package com.ncoxs.myblog.model.dto;

import com.ncoxs.myblog.handler.response.FilterBlank;
import com.ncoxs.myblog.model.pojo.User;
import lombok.Data;

@Data
@FilterBlank
public class UserAndIdentity {

    private User user;
    private String identity;
}
