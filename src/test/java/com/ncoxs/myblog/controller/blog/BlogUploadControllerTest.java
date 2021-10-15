package com.ncoxs.myblog.controller.blog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.dao.mysql.BlogDraftDao;
import com.ncoxs.myblog.dao.mysql.SavedImageTokenDao;
import com.ncoxs.myblog.dao.mysql.UploadImageDao;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserLoginResp;
import com.ncoxs.myblog.model.pojo.BlogDraft;
import com.ncoxs.myblog.model.pojo.UploadImage;
import com.ncoxs.myblog.testutil.BaseTester;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import com.ncoxs.myblog.util.general.ResourceUtil;
import com.ncoxs.myblog.util.general.UUIDUtil;
import com.ncoxs.myblog.util.model.Tuple2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BlogUploadControllerTest extends BaseTester {

    @Autowired
    BlogDraftDao blogDraftDao;

    @Autowired
    SavedImageTokenDao savedImageTokenDao;

    @Autowired
    UploadImageDao uploadImageDao;


    /**
     * 测试上传博客草稿功能，针对上传和修改内容、图片这些要点进行了测试。
     */
    @Test
    @Transactional
    public void testUploadBlogDraft() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 上传图片 test1.gif
        String imageToken = UUIDUtil.generate();
        String imageUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, imageToken, "test1.gif");
        // 上传图片 test2.jpeg
        String imageUrl2 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, imageToken, "test2.jpeg");

        // 读取博客草稿正文并替换图片 url
        String blogBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        blogBody = blogBody.replace("url-placeholder1", imageUrl1);
        blogBody = blogBody.replace("url-placeholder2", imageUrl2);

        // 上传新的博客草稿
        BlogUploadController.BlogDraftParams params = new BlogUploadController.BlogDraftParams();
        params.title = "标题1";
        params.setMarkdownBody(blogBody);
        params.isAllowReprint = true;
        params.setImageToken(imageToken);
        params.setUserLoginToken(tuple.t1.getToken());

        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/draft/upload")
                .jsonParams(params)
                .session(tuple.t2)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<Integer> result2 = objectMapper.readValue(data,
                new TypeReference<GenericResult<Integer>>() {
                });
        assertNotNull(result2.getData());
        int blogDraftId = result2.getData();

        // assert 博客草稿上传
        BlogDraft blogDraft = blogDraftDao.selectById(result2.getData());
        assertNotNull(blogDraft);
        assertEquals(blogBody, blogDraft.getMarkdownBody());
        assertEquals(params.title, blogDraft.getTitle());
        assertEquals(params.isAllowReprint, blogDraft.getIsAllowReprint());

        String savedImageToken = savedImageTokenDao.selectTokenByTarget(UploadImageTargetType.BLOG_DRAFT, result2.getData());
        assertEquals(savedImageToken, imageToken);

        List<UploadImage> uploadImages = uploadImageDao.selectByToken(imageToken);
        assertEquals(2, uploadImages.size());
        assertTrue(imageUrl1.endsWith(uploadImages.get(0).getFilepath()));
        assertTrue(imageUrl2.endsWith(uploadImages.get(1).getFilepath()));

        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(0).getFilepath())).isFile());
        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(1).getFilepath())).isFile());

        // 这张图片在下面的修改博客草稿中将被删除
        String deleteImagePath = uploadImages.get(1).getFilepath();

        int cnt = 0;
        Enumeration<String> attributeNames = tuple.t2.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            attributeNames.nextElement();
            cnt++;
        }
        // 上传博客草稿后，session 里面的属性应该只剩下用户登录 token 的两个，以及图片 token set
        assertEquals(3, cnt);


        // 上传图片 test3.jpeg
        String imageUrl3 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, imageToken, "test3.jpeg");

        // 读取修改的博客草稿正文
        blogBody = ResourceUtil.loadString("markdown/test-blog-update.md");
        blogBody = blogBody.replace("url-placeholder1", imageUrl1);
        // 修改博客草稿中的一张图片
        blogBody = blogBody.replace("url-placeholder2", imageUrl3);

        // 修改博客草稿
        params = new BlogUploadController.BlogDraftParams();
        // 修改博客草稿，上传 id
        params.setId(blogDraftId);
        params.title = "标题2";
        params.setMarkdownBody(blogBody);
        params.isAllowReprint = false;
        params.setImageToken(imageToken);
        params.setUserLoginToken(tuple.t1.getToken());


        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/draft/upload")
                .jsonParams(params)
                .session(tuple.t2)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        result2 = objectMapper.readValue(data,
                new TypeReference<GenericResult<Integer>>() {
                });
        assertNotNull(result2.getData());

        // assert 博客草稿上修改
        blogDraft = blogDraftDao.selectById(result2.getData());
        assertNotNull(blogDraft);
        assertEquals(blogBody, blogDraft.getMarkdownBody());
        assertEquals(params.title, blogDraft.getTitle());
        assertEquals(params.isAllowReprint, blogDraft.getIsAllowReprint());

        uploadImages = uploadImageDao.selectByToken(imageToken);
        assertEquals(2, uploadImages.size());
        assertTrue(imageUrl1.endsWith(uploadImages.get(0).getFilepath()));
        // 修改的图片
        assertTrue(imageUrl3.endsWith(uploadImages.get(1).getFilepath()));

        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(0).getFilepath())).isFile());
        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(1).getFilepath())).isFile());
        // 测试被删除的图片
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.classpath("static/img/" + deleteImagePath));
    }

    @Test
    @Transactional
    public void testUploadBlogDraftCover() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 上传图片 test1.gif 作为博客封面
        String coverToken = UUIDUtil.generate();
        String coverUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT_COVER, coverToken, "test1.gif");

        // 上传新的博客草稿
        String blogBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        BlogUploadController.BlogDraftParams params = new BlogUploadController.BlogDraftParams();
        params.title = "标题1";
        params.setMarkdownBody(blogBody);
        params.isAllowReprint = true;
        // 将 coverUrl1 作为博客封面
        params.setCoverToken(coverToken);
        params.setUserLoginToken(tuple.t1.getToken());

        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/draft/upload")
                .jsonParams(params)
                .session(tuple.t2)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<Integer> result2 = objectMapper.readValue(data,
                new TypeReference<GenericResult<Integer>>() {
                });
        assertNotNull(result2.getData());

        // assert 博客草稿上传
        BlogDraft blogDraft = blogDraftDao.selectById(result2.getData());
        assertNotNull(blogDraft);
        assertTrue(coverUrl1.endsWith(blogDraft.getCoverPath()));
        // 之后这个博客封面会被替换
        String deleteCoverPath1 = blogDraft.getCoverPath();


        // 上传图片 test2.jpeg 作为新的博客封面
        String coverUrl2 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT_COVER, coverToken, "test2.jpeg");
        // 修改博客封面
        params = new BlogUploadController.BlogDraftParams();
        params.setId(result2.getData());
        // 删除原来的封面，其实这里的赋值有没有都可以
        params.setCoverToken(coverToken);
        params.setUserLoginToken(tuple.t1.getToken());

        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/draft/upload")
                .jsonParams(params)
                .session(tuple.t2)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        result2 = objectMapper.readValue(data,
                new TypeReference<GenericResult<Integer>>() {
                });
        assertNotNull(result2.getData());

        // assert 博客草稿修改
        blogDraft = blogDraftDao.selectById(result2.getData());
        assertNotNull(blogDraft);
        assertTrue(coverUrl2.endsWith(blogDraft.getCoverPath()));

        assertTrue(new File(ResourceUtil.classpath("static/img/" + blogDraft.getCoverPath())).isFile());
        // 测试被删除的图片
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.classpath("static/img/" + deleteCoverPath1));
    }
}
