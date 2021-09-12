package com.ncoxs.myblog.controller.blog;

import com.ncoxs.myblog.constant.ParamValidateMsg;
import com.ncoxs.myblog.constant.ParamValidateRule;
import com.ncoxs.myblog.model.pojo.Blog;
import com.ncoxs.myblog.service.app.ImageService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.IOException;
import java.util.Map;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;

/**
 * 博客上传控制器
 */
@RestController
@RequestMapping("/blog")
@Validated
public class BlogUploadController {

    private ImageService imageService;

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }


    // TODO: 如何防止恶意图片上传
    /**
     * Editor.md Markdown 编辑器博客图片上传，参见：
     * https://www.cnblogs.com/softidea/p/7808214.html#23%E4%B8%8A%E4%BC%A0%E5%9B%BE%E7%89%87
     */
    @PostMapping("/editor-md-img/upload")
    public Map<String, Object> uploadEditorMdImage(@RequestParam("editormd-image-file") MultipartFile imgFile) {
        try {
            String url = imageService.saveImage(imgFile, ImageService.IMAGE_TYPE_BLOG);
            if (url != null) {
                return mp(kv("success", 0), kv("message", "成功"), kv("url", url));
            } else {
                return mp(kv("success", 1), kv("message", "图片为空或格式错误"));
            }
        } catch (IOException e) {
            return mp(kv("success", 1), kv("message", "保存图片出错"));
        }
    }

    @Data
    public static class UploadBlogParams {

        @NotBlank(message = ParamValidateMsg.USER_EMAIL_BLANK)
        @Pattern(regexp = ParamValidateRule.EMAIL_REGEX, message = ParamValidateMsg.USER_EMAIL_FORMAT)
        public String email;

        @NotBlank(message = ParamValidateMsg.USER_PASSWORD_BLANK)
        @Pattern(regexp = ParamValidateRule.PASSWORD_REGEX, message = ParamValidateMsg.USER_PASSWORD_FORMAT)
        public String password;

        public Blog blog;
    }

    @PostMapping("/upload")
    public void uploadBlog(@RequestBody UploadBlogParams params) {

    }
}
