package com.ncoxs.myblog.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCode {

    private String token;
    private String code;
    private long expireAt;
}
