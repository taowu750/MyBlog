package com.ncoxs.myblog.model.dto;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import lombok.Data;

import javax.validation.constraints.Pattern;

/**
 * 客户端定位（根据 ip）数据。
 */
@Data
public class IpLocInfo {

    @Pattern(regexp = ParamValidateRule.IP_REGEX, message = ParamValidateMsg.IP_FORMAT)
    private String ip;

    private String province;

    private String city;
}
