package com.ncoxs.myblog.service.app;

import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.dao.mysql.UploadImageDao;
import com.ncoxs.myblog.model.pojo.UploadImage;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.util.general.FileUtil;
import com.ncoxs.myblog.util.general.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 处理客户端传过来的图片。
 */
@Service
public class ImageService {

    public static final String IMAGE_TYPE_BLOG = "blog";

    @Value("${myapp.website.url}")
    private String webSiteUrl;

    @Value("${myapp.image.origin-filename-max-length}")
    private int originFilenameMaxLength;

    private UploadImageDao uploadImageDao;

    @Autowired
    public void setUploadImgDao(UploadImageDao uploadImageDao) {
        this.uploadImageDao = uploadImageDao;
    }


    /**
     * 保存图片到服务器上，并返回图片 url。如果返回 url 为 null，表示图片文件为空或格式有问题。
     *
     * @param user 上传图片的用户
     * @param imgFile 客户端传递的图片文件
     * @param targetType 包含图片的目标类型
     * @param token 一组图片的唯一标识，可以是 UUID 或者 "targetType_targetId" 的形式
     * @return 图片 url
     * @throws IOException 保存文件失败抛出此异常
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public String saveImage(User user, MultipartFile imgFile, int targetType, String token) throws IOException {
        if (imgFile.isEmpty() || !FileUtil.isImageFileName(imgFile.getOriginalFilename())) {
            return null;
        }

        String originalFilename = imgFile.getOriginalFilename();
        //noinspection ConstantConditions
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String type = UploadImageTargetType.toStr(targetType);
        String dir = FileUtil.dateHourDirName();
        String fileName = FileUtil.randomFileName(extension);

        // 插入图片上传记录
        UploadImage uploadImage = new UploadImage();
        uploadImage.setTargetType(targetType);
        uploadImage.setFileName(fileName);
        uploadImage.setOriginFileName(FileUtil.truncatefilename(originalFilename, originFilenameMaxLength));
        uploadImageDao.insert(uploadImage);

        Path filePath = Paths.get(ResourceUtil.classpath("static"), "img", type, dir, fileName);
        imgFile.transferTo(filePath.toFile());

        return webSiteUrl + "img/" + type + "/" + dir + "/" + fileName;
    }
}
