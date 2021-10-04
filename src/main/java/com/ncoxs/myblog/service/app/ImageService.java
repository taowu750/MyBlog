package com.ncoxs.myblog.service.app;

import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.dao.mysql.SavedImageTokenDao;
import com.ncoxs.myblog.dao.mysql.UploadImageDao;
import com.ncoxs.myblog.model.pojo.SavedImageToken;
import com.ncoxs.myblog.model.pojo.UploadImage;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.user.UserService;
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
 * 4. 当更新编辑对象时，需要先将其数据库中保存的图片数据加载到 session 中。
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

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private MarkdownService markdownService;

    @Autowired
    public void setMarkdownService(MarkdownService markdownService) {
        this.markdownService = markdownService;
    }

    private UploadImageDao uploadImageDao;

    @Autowired
    public void setUploadImgDao(UploadImageDao uploadImageDao) {
        this.uploadImageDao = uploadImageDao;
    }

    private SavedImageTokenDao savedImageTokenDao;

    @Autowired
    public void setSavedImageTokenDao(SavedImageTokenDao savedImageTokenDao) {
        this.savedImageTokenDao = savedImageTokenDao;
    }


    private static final String SESSION_KEY_UPLOAD_IMAGE_TOKEN = "uploadImageToken";


    /**
     * 保存图片到服务器上，并返回图片 url。如果返回 url 为 null，表示图片文件为空或格式有问题。
     *
     * @param userLoginToken 用户登录 token
     * @param imgFile 客户端传递的图片文件
     * @param imageToken 一组图片的唯一标识
     * @param targetType 包含图片的目标类型
     * @return 图片 url
     * @throws IOException 保存文件失败抛出此异常
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public String saveImage(String userLoginToken, MultipartFile imgFile, String imageToken, int targetType) throws IOException {
        User user = userService.accessByToken(userLoginToken);
        // 图片不存在或格式有问题
        if (imgFile.isEmpty() || !FileUtil.isImageFileName(imgFile.getOriginalFilename())) {
            return null;
        }

        String originalFilename = imgFile.getOriginalFilename();
        //noinspection ConstantConditions
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String type = UploadImageTargetType.toStr(targetType);
        String dir = FileUtil.dateHourDirName();
        String fileName = FileUtil.randomFilename(extension);

        // 插入图片上传记录
        UploadImage uploadImage = new UploadImage();
        uploadImage.setUserId(user.getId());
        uploadImage.setToken(imageToken);
        uploadImage.setFilepath(type + "/" + dir + "/" + fileName);
        uploadImage.setOriginFileName(FileUtil.truncateFilename(originalFilename, originFilenameMaxLength));
        uploadImageDao.insert(uploadImage);

        // 将图片写入文件
        Path filePath = Paths.get(ResourceUtil.classpath("static"), "img", type, dir, fileName);
        imgFile.transferTo(filePath.toFile());

        // 将图片数据记录在 session 中
        loadImagesToSession(imageToken, Collections.singletonList(uploadImage));

        return webSiteUrl + "img/" + uploadImage.getFilepath();
    }

    /**
     * 当用户上传博客、评论等可能包含图片的对象时，保存图片 token 和这些对象的关系，并且删除没有用到的图片。
     */
    public void saveImageTokenWithTarget(String imageToken, int targetType, int targetId, String markdown) {
        HttpSession session = SpringUtil.currentSession();
        //noinspection unchecked
        Set<String> tokens = (Set<String>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN);
        if (tokens == null || !tokens.contains(imageToken)) {
            return;
        }

        // 插入新纪录，已存在则不会插入
        SavedImageToken savedImageToken = new SavedImageToken();
        savedImageToken.setToken(imageToken);
        savedImageToken.setTargetId(targetType);
        savedImageToken.setTargetType(targetId);
        savedImageTokenDao.insert(savedImageToken);

        // 删除没有用到的图片
        deleteSessionDiscardedImage(imageToken, markdown);
    }

    /**
     * 当需要修改博客、博客草稿等可能包含图片的对象时，需要先把已保存的图片数据加载到 session 中，
     * 然后删除其中没有用到的图像。注意，博客等对象必须之前已经提交了。
     */
    public void loadAndDeleteSessionImages(int targetType, int targetId, String markdown) {
        String imageToken = savedImageTokenDao.selectTokenByTarget(targetType, targetId);
        if (imageToken == null) {
            return;
        }

        List<UploadImage> savedImages = uploadImageDao.selectByToken(imageToken);
        if (savedImages != null && !savedImages.isEmpty()) {
            loadImagesToSession(imageToken, savedImages);
            deleteSessionDiscardedImage(imageToken, markdown);
        }
    }

    private void loadImagesToSession(String imageToken, List<UploadImage> images) {
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

        for (UploadImage uploadImage : images) {
            fp2id.put(uploadImage.getFilepath(), uploadImage.getId());
        }
    }

    /**
     * 当用户主动关闭网页、退出登录、或 session 被动销毁时，需要删除已上传的图片。
     */
    public void deleteSessionImages(HttpSession session) {
        //noinspection unchecked
        Set<String> tokens = (Set<String>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN);
        if (tokens == null) {
            return;
        }

        for (String token : tokens) {
            deleteSessionImage(session, token);
        }

        session.removeAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN);
    }

    /**
     * 当用户上传博客或评论等时，删除没有包含在内的图片。
     *
     * @param token 图片 token
     * @param markdown 可能包含有图片的 markdown 文本
     */
    public void deleteSessionDiscardedImage(String token, String markdown) {
        if (markdown == null) {
            return;
        }

        HttpSession session = SpringUtil.currentSession();
        //noinspection unchecked
        Map<String, Integer> fp2id = (Map<String, Integer>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN + token);
        if (fp2id == null || fp2id.isEmpty()) {
            return;
        }

        Set<String> usedImages = markdownService.parseUsedImages(markdown);
        fp2id.keySet().removeIf(usedImages::contains);
        deleteSessionImage(session, token);

        //noinspection unchecked
        Set<String> tokens = (Set<String>) session.getAttribute(SESSION_KEY_UPLOAD_IMAGE_TOKEN);
        tokens.remove(token);
    }

    /**
     * 当用户放弃编辑博客或评论等时，需要删除对应的图片
     */
    public void deleteSessionImage(String token) {
        deleteSessionImage(SpringUtil.currentSession(), token);
    }

    private void deleteSessionImage(HttpSession session, String token) {
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
