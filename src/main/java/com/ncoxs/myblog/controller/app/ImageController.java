package com.ncoxs.myblog.controller.app;

import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.service.app.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/app/image")
public class ImageController {

    private ImageService imageService;

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }


    // TODO: 如何防止恶意图片上传
    /**
     * 图片上传。
     */
    @PostMapping("/upload")
    public GenericResult<String> uploadImage(MultipartFile imageFile, int targetType, int targetId) {
        try {
            String url = imageService.saveImage(imageFile, targetType, targetId);
            if (url != null) {
                return GenericResult.success(url);
            } else {
                return GenericResult.error(ResultCode.FILE_UPLOAD_IMAGE_ERROR);
            }
        } catch (IOException e) {
            return GenericResult.error(ResultCode.FILE_SAVE_IMAGE_ERROR);
        }
    }
}
