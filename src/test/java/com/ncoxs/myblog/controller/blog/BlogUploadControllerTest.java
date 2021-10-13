package com.ncoxs.myblog.controller.blog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.dao.mysql.BlogDraftDao;
import com.ncoxs.myblog.dao.mysql.SavedImageTokenDao;
import com.ncoxs.myblog.dao.mysql.UploadImageDao;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.ImageHolderParams;
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
        ImageHolderParams<BlogUploadController.BlogDraftParams> params = new ImageHolderParams<>();
        BlogUploadController.BlogDraftParams blogDraftParams = new BlogUploadController.BlogDraftParams();
        blogDraftParams.title = "标题1";
        blogDraftParams.markdownBody = blogBody;
        blogDraftParams.isAllowReprint = true;
        params.setImageHolder(blogDraftParams);
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
        assertEquals(blogDraftParams.title, blogDraft.getTitle());
        assertEquals(blogDraftParams.isAllowReprint, blogDraft.getIsAllowReprint());

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
        params = new ImageHolderParams<>();
        blogDraftParams = new BlogUploadController.BlogDraftParams();
        // 修改博客草稿，上传 id
        blogDraftParams.id = blogDraftId;
        blogDraftParams.title = "标题2";
        blogDraftParams.markdownBody = blogBody;
        blogDraftParams.isAllowReprint = false;
        params.setImageHolder(blogDraftParams);
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
        assertEquals(blogDraftParams.title, blogDraft.getTitle());
        assertEquals(blogDraftParams.isAllowReprint, blogDraft.getIsAllowReprint());

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
}
