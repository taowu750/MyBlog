package com.ncoxs.myblog.controller.app;

import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.service.app.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/app/img")
public class ImageController {

    private ImageService imageService;

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }


    /**
     * 删除已上传的未使用图片。当用户不提交博客、评论时，就需要删除其中已经上传的图片。
     *
     * @param unusedImgs 需要删除的未使用图片的相对路径名，也就是图片 url 去掉域名剩余的部分
     * @return 和图片列表长度相同的 bool 列表，表示对应的每个图片是否删除成功；图片不存在或使用中都会导致返回 false
     */
    @DeleteMapping("/unused-image")
    public GenericResult<List<Boolean>> deleteUnusedImage(@RequestBody @NotNull List<String> unusedImgs) {
        return GenericResult.success(imageService.deleteImages(unusedImgs));
    }
}
