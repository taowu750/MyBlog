package com.ncoxs.myblog.controller.blog;

import com.ncoxs.myblog.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static com.ncoxs.myblog.util.general.MapUtil.kv;
import static com.ncoxs.myblog.util.general.MapUtil.mp;

/**
 * 博客上传控制器
 */
@RestController
@RequestMapping("/blog")
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
    @PostMapping("/editor-md-blog-img/upload")
    public Map<String, Object> uploadEditorMdImage(@RequestParam("editormd-image-file") MultipartFile imgFile) {
        try {
            String url = imageService.saveImage(imgFile, ImageService.IMAGE_TYPE_BLOG);
            if (url != null) {
                return mp(kv("success", 0), kv("message", "成功"), kv("url", url));
            } else {
                return mp(kv("success", 1), kv("message", "文件为空或格式错误"));
            }
        } catch (IOException e) {
            return mp(kv("success", 1), kv("message", "保存文件出错"));
        }
    }
}
