package com.ncoxs.myblog.service.app;

import com.ncoxs.myblog.util.general.FileUtil;
import com.ncoxs.myblog.util.general.ResourceUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理客户端传过来的图片。
 */
@Service
public class ImageService {

    public static final String IMAGE_TYPE_BLOG = "blog";

    @Value("${myapp.website.url}")
    private String webSiteUrl;

    /**
     * 保存图片到服务器上，并返回图片 url。如果返回 url 为 null，表示图片文件为空或格式有问题。
     *
     * @param imgFile 客户端传递的图片文件
     * @param type 图片文件所属类别（博客、评论等）
     * @return 图片 url
     * @throws IOException 保存文件失败抛出此异常
     */
    public String saveImage(MultipartFile imgFile, String type) throws IOException {
        if (imgFile.isEmpty() || !FileUtil.isImageFileName(imgFile.getOriginalFilename())) {
            return null;
        }

        String trueFileName = imgFile.getOriginalFilename();
        //noinspection ConstantConditions
        String extension = trueFileName.substring(trueFileName.lastIndexOf("."));
        String dir = FileUtil.dateHourDirName();
        String fileName = FileUtil.randomFileName(extension);
        Path filePath = Paths.get(ResourceUtil.classpath("static"), "img", type, dir, fileName);

        imgFile.transferTo(filePath.toFile());

        return webSiteUrl + "img/" + type + "/" + dir + "/" + fileName;
    }

    public List<Boolean> deleteUnusedImages(List<String> images) {
        return null;
    }

    public List<Boolean> deleteImages(List<String> unusedImgs) {
        List<Boolean> result = new ArrayList<>(unusedImgs.size());
        for (int i = 0; i < unusedImgs.size(); i++) {

        }

        return null;
    }

    public boolean isUnusedImage(String imagePath) {
        return false;
    }

    public boolean deleteImage(String imagePath) {
        return false;
    }
}
