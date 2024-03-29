package com.ncoxs.myblog.controller.app;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.handler.validate.UserLoginToken;
import com.ncoxs.myblog.handler.validate.UserValidate;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.service.app.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/app/image")
@Validated
@Slf4j
public class ImageController {

    private ImageService imageService;

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }


    /**
     * 上传图片，成功返回图片 URL。
     *
     * @param userLoginToken 用户登录 token
     * @param imageFile      图片文件
     * @param targetType     图片所属对象的类型
     */
    @PostMapping("/upload")
    @UserValidate
    public GenericResult<String> uploadImage(@UserLoginToken String userLoginToken, MultipartFile imageFile,
                                             @Range(min = UploadImageTargetType.BLOG,
                                                     max = UploadImageTargetType.USER_PROFILE_PICTURE,
                                                     message = ParamValidateMsg.UPLOAD_IMAGE_TARGET_TYPE_INVALID)
                                                     int targetType) {
        try {
            String url = imageService.saveImage(userLoginToken, imageFile, targetType);
            if (url == null) {
                return GenericResult.error(ResultCode.FILE_UPLOAD_IMAGE_ERROR);
            } else {
                return GenericResult.success(url);
            }
        } catch (IOException e) {
            log.error("保存图片出错", e);
            return GenericResult.error(ResultCode.FILE_SAVE_IMAGE_ERROR);
        }
    }
}
