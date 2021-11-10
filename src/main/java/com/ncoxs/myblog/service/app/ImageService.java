package com.ncoxs.myblog.service.app;

import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.dao.mysql.UploadImageBindDao;
import com.ncoxs.myblog.dao.mysql.UploadImageDao;
import com.ncoxs.myblog.exception.ResultCodeException;
import com.ncoxs.myblog.model.pojo.UploadImage;
import com.ncoxs.myblog.model.pojo.UploadImageBind;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.user.UserService;
import com.ncoxs.myblog.util.data.FileUtil;
import com.ncoxs.myblog.util.data.ResourceUtil;
import com.ncoxs.myblog.util.general.SpringUtil;
import com.ncoxs.myblog.util.general.URLUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 图片上传逻辑：
 * 1. 客户端上传图片、所属类型 targetType（参见 {@link com.ncoxs.myblog.constant.UploadImageTargetType}）
 * 2. 上传图片后，将图片保存到文件，并记录在数据库中。然后将 (图片路径, id) 缓存到 session 的一个 map 中。
 * 3. 当上传博客等包含图片的对象时，从中解析出使用的图片，然后从 session 中删除用到的图片，
 * 最后在数据库中记录每张用到的图片所属的对象 id（targetId）（用一个新的表）。
 * 4. session 被销毁时删除仍然记录的图片数据。
 * <p>
 * 当修改博客等包含图片的对象时：
 * 1. 客户端上传修改后的内容时，从中解析出使用的图片
 * 2. 将包含图片的对象之前使用的图片数据加载到 session 中，然后从 session 中删除用到的图片，
 * 最后在数据库中记录每张用到的图片的 targetId（用一个新的表）。
 * 3. session 被销毁时删除仍然记录的图片数据。
 * <p>
 * 注意，博客封面、专栏图标之类的图片需要单独成为一种类型，不过它们的 targetId 还是博客或专栏。
 * <p>
 * 此外用户头像不保存在 session 中，不走上面的逻辑（为了防止恶意上传头像，可以限制修改头像的频率）。
 */
@Service
@Slf4j
public class ImageService {

    @Value("${myapp.website.url}")
    private String webSiteUrl;

    @Value("${myapp.website.image-dir}")
    private String imageDir;

    @Value("${myapp.image.origin-filename-max-length}")
    private int originFilenameMaxLength;


    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private UploadImageDao uploadImageDao;

    @Autowired
    public void setUploadImgDao(UploadImageDao uploadImageDao) {
        this.uploadImageDao = uploadImageDao;
    }

    private UploadImageBindDao uploadImageBindDao;

    @Autowired
    public void setUploadImageBindDao(UploadImageBindDao uploadImageBindDao) {
        this.uploadImageBindDao = uploadImageBindDao;
    }


    private static final String SESSION_KEY_UPLOAD_IMAGES = "uploadImages";


    /**
     * 将图片路径转化成图片 url。如果是外链则原样返回
     */
    public String toImageUrl(String imagePath) {
        return StringUtils.hasText(imagePath)
                ? (URLUtil.isImageURL(imagePath) ? imagePath : webSiteUrl + imageDir + "/" + imagePath)
                : null;
    }

    /**
     * coverUrl 可能是外链，此时数据库中的 coverPath 需要存这条外链
     *
     * @param coverPath 之前解析出来的图片路径
     * @param coverUrl  上传图片的 url
     * @return 最终的图片路径
     */
    public String parseImagePath(String coverPath, String coverUrl) {
        return StringUtils.hasText(coverPath) ? coverPath : (URLUtil.isImageURL(coverUrl) ? coverUrl : null);
    }

    /**
     * 保存图片到服务器上，并返回图片 url。如果返回 url 为 null，表示图片文件为空或格式有问题。
     *
     * @param userLoginToken 用户登录 token
     * @param imgFile        客户端传递的图片文件
     * @param targetType     包含图片的目标类型
     * @return 图片 url
     * @throws IOException 保存文件失败抛出此异常
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public String saveImage(String userLoginToken, MultipartFile imgFile, int targetType) throws IOException {
        User user = userService.accessByToken(userLoginToken);
        // 图片不存在或格式有问题
        if (imgFile.isEmpty() || !FileUtil.isImageFileName(imgFile.getOriginalFilename())) {
            return null;
        }

        String originalFilename = imgFile.getOriginalFilename();
        //noinspection ConstantConditions
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String type = UploadImageTargetType.toStr(targetType);
        String dateDir = FileUtil.dateHourDirName();
        String fileName = FileUtil.randomFilename(extension);

        // 插入图片上传记录
        UploadImage uploadImage = new UploadImage();
        uploadImage.setUserId(user.getId());
        uploadImage.setTargetType(targetType);
        uploadImage.setFilepath(type + "/" + user.getId() + "/" + dateDir + "/" + fileName);
        uploadImage.setOriginFileName(FileUtil.truncateFilename(originalFilename, originFilenameMaxLength));
        uploadImageDao.insert(uploadImage);

        // 将图片写入文件
        Path filePath = Paths.get(ResourceUtil.classpath("static"), imageDir, uploadImage.getFilepath());
        // 注意要先创建文件夹
        Files.createDirectories(filePath.getParent());
        imgFile.transferTo(filePath.toFile());

        // 将图片数据记录在 session 中
        loadImagesToSession(Collections.singletonList(uploadImage));

        return toImageUrl(uploadImage.getFilepath());
    }

    private void loadImagesToSession(List<UploadImage> images) {
        if (images.isEmpty()) {
            return;
        }

        HttpSession session = SpringUtil.currentSession();

        //noinspection unchecked
        Map<String, Integer> cache = (Map<String, Integer>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGES);
        if (cache == null) {
            cache = new HashMap<>();
            session.setAttribute(SESSION_KEY_UPLOAD_IMAGES, cache);
        }

        for (UploadImage uploadImage : images) {
            // 用户头像不保存在 session 中
            if (uploadImage.getTargetType() != UploadImageTargetType.USER_PROFILE_PICTURE) {
                cache.put(uploadImage.getFilepath(), uploadImage.getId());
            }
        }
    }

    /**
     * 当上传或者修改包含图片的对象时（如博客、评论、博客封面等），保存图片和包含对象的映射关系，并更新 session。
     *
     * @param usedImagePaths 包含图片的对象中所使用的图片相对路径
     * @param targetType     包含图片的对象的类型，参见 {@link UploadImageTargetType}
     * @param targetId       包含图片的对象的 id
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void bindImageTarget(Set<String> usedImagePaths, int targetType, int targetId, boolean updateTargetType) {
        HttpSession session = SpringUtil.currentSession();
        //noinspection unchecked
        Map<String, Integer> cache = (Map<String, Integer>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGES);
        if (cache == null) {
            cache = new HashMap<>();
            session.setAttribute(SESSION_KEY_UPLOAD_IMAGES, cache);
        }

        // 将 targetId、targetType 包含的图片加载到 session 中
        loadImagesToSession(uploadImageBindDao.selectUploadImages(targetType, targetId));

        // 保存新图片和 target 的映射关系
        for (String usedImagePath : usedImagePaths) {
            int imageId = cache.getOrDefault(usedImagePath, -1);
            // 当图片根本不存在时，抛出异常
            if (imageId == -1) {
                throw new ResultCodeException(ResultCode.DATA_ACCESS_DENIED);
            }
            uploadImageBindDao.insert(new UploadImageBind(imageId, targetType, targetId, usedImagePath));
            // 某些情况下（例如博客草稿->博客），还需要修改 upload_image 的 targetType
            if (updateTargetType) {
                uploadImageDao.updateTargetTypeById(imageId, targetType);
            }
        }

        // 从 session 中删除使用到的图片
        cache.keySet().removeIf(usedImagePaths::contains);
    }

    /**
     * 删除 Session 中的所有未保存图片和记录，用在 session 销毁时。
     */
    public void deleteSessionImages(HttpSession session) {
        //noinspection unchecked
        Map<String, Integer> cache = (Map<String, Integer>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGES);
        if (cache == null) {
            return;
        }

        for (Map.Entry<String, Integer> en : cache.entrySet()) {
            String filepath = en.getKey();
            int imageId = en.getValue();

            uploadImageDao.deleteById(imageId);
            uploadImageBindDao.deleteByImageId(imageId);

            Path realPath = Paths.get(ResourceUtil.classpath("static"), imageDir, filepath);
            try {
                Files.delete(realPath);
            } catch (IOException e) {
                log.error("删除图片失败：" + realPath, e);
            }
        }
    }

    /**
     * 删除指定对象在数据库中的所有图片记录。
     *
     * @param targetType 对象类型，参见 {@link UploadImageTargetType}
     * @param targetId   对象 id
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void deleteImages(int targetType, int targetId) {
        List<UploadImage> uploadImages = uploadImageBindDao.selectUploadImages(targetType, targetId);
        uploadImageBindDao.deleteByTarget(targetType, targetId);
        for (UploadImage uploadImage : uploadImages) {
            uploadImageDao.deleteById(uploadImage.getId());

            Path realPath = Paths.get(ResourceUtil.classpath("static"), imageDir, uploadImage.getFilepath());
            try {
                Files.delete(realPath);
            } catch (IOException e) {
                log.error("删除图片失败：" + realPath, e);
            }
        }
    }

    /**
     * 更改图片的 target 类型和 targetId。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void updateImageTarget(int oldTargetType, int oldTargetId, int newTargetType, int newTargetId) {
        List<UploadImage> uploadImages = uploadImageBindDao.selectUploadImages(oldTargetType, oldTargetId);
        for (UploadImage uploadImage : uploadImages) {
            uploadImageDao.updateTargetTypeById(uploadImage.getId(), newTargetType);
        }
        uploadImageBindDao.updateTarget(oldTargetType, oldTargetId, newTargetType, newTargetId);
    }
}
