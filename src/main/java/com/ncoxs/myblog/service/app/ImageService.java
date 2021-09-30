package com.ncoxs.myblog.service.app;

import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.dao.mysql.SavedImageTokenDao;
import com.ncoxs.myblog.dao.mysql.UploadImageDao;
import com.ncoxs.myblog.model.pojo.SavedImageToken;
import com.ncoxs.myblog.model.pojo.UploadImage;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.util.general.FileUtil;
import com.ncoxs.myblog.util.general.ResourceUtil;
import com.ncoxs.myblog.util.general.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 处理客户端传过来的图片。
 *
 * 上传图片逻辑：
 * 0. 每个编辑对象（可以是博客、评论等可以包含图片的对象）都会生成一个唯一的图片 token
 * 1. 上传图片带上图片 token，用这个 token 标识属于编辑对象的一组图片。将图片存入文件，并记录在数据库中
 * 2. 在 session 中记录下用户的图片 token 列表，以及每个 token 所对应的 (图片路径名, 图片 id) 映射关系（方便以后删除）。
 *    当用户主动关闭网页，放弃编辑时，或 session 被动销毁，则需要将这些图片和数据库记录删除
 * 3. 当上传编辑对象时，带上图片 token，然后从 session 读取数据，并删除不存在于编辑对象的图片和数据库记录。
 *    session 中的图片 token 数据随后也被清理。最后记录图片 token 和编辑对象 id 的对应关系。
 *
 * 当系统启动时，需要删除那些没有 targetId 的图片文件和数据库记录
 */
@Service
@Slf4j
public class ImageService {

    @Value("${myapp.website.url}")
    private String webSiteUrl;

    @Value("${myapp.image.origin-filename-max-length}")
    private int originFilenameMaxLength;

    private UploadImageDao uploadImageDao;

    private SavedImageTokenDao savedImageTokenDao;

    @Autowired
    public void setUploadImgDao(UploadImageDao uploadImageDao) {
        this.uploadImageDao = uploadImageDao;
    }

    @Autowired
    public void setSavedImageTokenDao(SavedImageTokenDao savedImageTokenDao) {
        this.savedImageTokenDao = savedImageTokenDao;
    }


    private static final String SESSION_KEY_UPLOAD_IMAGE_TOKEN = "uploadImageToken";


    /**
     * 保存图片到服务器上，并返回图片 url。如果返回 url 为 null，表示图片文件为空或格式有问题。
     *
     * @param user 上传图片的用户
     * @param imgFile 客户端传递的图片文件
     * @param imageToken 一组图片的唯一标识
     * @param targetType 包含图片的目标类型
     * @return 图片 url
     * @throws IOException 保存文件失败抛出此异常
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public String saveImage(User user, MultipartFile imgFile, String imageToken, int targetType) throws IOException {
        // 图片不存在或格式有问题
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
        uploadImage.setUserId(user.getId());
        uploadImage.setToken(imageToken);
        uploadImage.setFilepath(type + "/" + dir + "/" + fileName);
        uploadImage.setOriginFileName(FileUtil.truncatefilename(originalFilename, originFilenameMaxLength));
        uploadImageDao.insert(uploadImage);

        // 将图片写入文件
        Path filePath = Paths.get(ResourceUtil.classpath("static"), "img", type, dir, fileName);
        imgFile.transferTo(filePath.toFile());

        // 将图片数据记录在 session 中
        HttpSession session = SpringUtil.currentSession();
        //noinspection unchecked
        Set<String> tokens = (Set<String>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN);
        if (tokens == null) {
            tokens = new HashSet<>();
            session.setAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN, tokens);
        }
        tokens.add(imageToken);
        //noinspection unchecked
        Map<String, Integer> fp2id = (Map<String, Integer>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN + imageToken);
        if (fp2id == null) {
            fp2id = new HashMap<>();
            session.setAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN + imageToken, fp2id);
        }
        fp2id.put(uploadImage.getFilepath(), uploadImage.getId());

        return webSiteUrl + "img/" + uploadImage.getFilepath();
    }

    /**
     * 当用户上传博客、评论等可能包含图片的对象时，保存图片 token 和这些对象的关系。
     */
    public boolean saveImageTokenWithTarget(String imageToken, int targetType, int targetId) {
        HttpSession session = SpringUtil.currentSession();
        //noinspection unchecked
        Set<String> tokens = (Set<String>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN);
        if (tokens == null || !tokens.contains(imageToken)) {
            return false;
        }

        SavedImageToken savedImageToken = new SavedImageToken();
        savedImageToken.setToken(imageToken);
        savedImageToken.setTargetId(targetType);
        savedImageToken.setTargetType(targetId);
        boolean result = savedImageTokenDao.insert(savedImageToken);

        tokens.remove(imageToken);
        session.removeAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN + imageToken);

        return result;
    }

    /**
     * 当用户主动关闭网页、退出登录、或 session 被动销毁时，需要删除已上传的图片。
     */
    public void deleteImages(HttpSession session) {
        //noinspection unchecked
        Set<String> tokens = (Set<String>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN);
        if (tokens == null) {
            return;
        }

        for (String token : tokens) {
            deleteImage(session, token);
        }

        session.removeAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN);
    }

    /**
     * 当用户上传博客或评论等时，删除没有包含在内的图片。
     *
     * @param usedImages 被使用的图片相对路径
     */
    public void deleteDiscardedImage(String token, Set<String> usedImages) {
        HttpSession session = SpringUtil.currentSession();
        //noinspection unchecked
        Map<String, Integer> fp2id = (Map<String, Integer>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN + token);
        if (fp2id == null) {
            return;
        }

        fp2id.keySet().removeIf(usedImages::contains);
        deleteImage(session, token);
    }

    /**
     * 当用户放弃编辑博客或评论等时，需要删除对应的图片
     */
    public void deleteImage(String token) {
        deleteImage(SpringUtil.currentSession(), token);
    }

    private void deleteImage(HttpSession session, String token) {
        //noinspection unchecked
        Map<String, Integer> fp2id = (Map<String, Integer>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN + token);
        if (fp2id == null) {
            return;
        }
        for (Map.Entry<String, Integer> en : fp2id.entrySet()) {
            String filepath = en.getKey();
            int imageId = en.getValue();

            Path realPath = Paths.get(ResourceUtil.classpath("static"), "img", filepath);
            try {
                Files.delete(realPath);
            } catch (IOException e) {
                log.error("删除图片失败：" + realPath, e);
            }

            uploadImageDao.deleteById(imageId);
        }
        session.removeAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN + token);
    }
}
