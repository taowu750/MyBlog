package com.ncoxs.myblog.controller.app;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.app.ImageService;
import com.ncoxs.myblog.service.user.UserService;
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

    private UserService userService;

    private ImageService imageService;


    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * 上传图片，成功返回图片 URL。
     *
     * @param userLoginToken 用户登录 token
     * @param imageFile      图片文件
     * @param imageToken     一组图片的唯一标识
     * @param targetType     图片所属对象的类型
     */
    @PostMapping("/upload")
    public GenericResult<String> uploadImage(String userLoginToken, MultipartFile imageFile,
                                             String imageToken,
                                             @Range(min = UploadImageTargetType.BLOG,
                                                     max = UploadImageTargetType.USER_PROFILE_PICTURE,
                                                     message = ParamValidateMsg.UPLOAD_IMAGE_TARGET_TYPE_INVALID)
                                                     int targetType) {
        User user = userService.accessByToken(userLoginToken);
        if (user == null) {
            return GenericResult.error(ResultCode.USER_NOT_LOGGED_IN);
        }

        try {
            String url = imageService.saveImage(user, imageFile, imageToken, targetType);
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
