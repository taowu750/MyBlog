package com.ncoxs.myblog.model.dto;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import lombok.*;

import javax.validation.constraints.NotBlank;

/**
 * 一些包含图片的对象（例如博客、评论等）的上传参数，包含图片 token
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageHolderParams<T> extends UserAccessParams {

    private T imageHolder;

    @NotBlank(message = ParamValidateMsg.UPLOAD_IMAGE_TOKEN_BLANK)
    private String imageToken;
}
