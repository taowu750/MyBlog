package com.ncoxs.myblog.controller.blog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.UploadImageTargetType;
import com.ncoxs.myblog.constant.blog.BlogStatus;
import com.ncoxs.myblog.dao.mysql.BlogDao;
import com.ncoxs.myblog.dao.mysql.BlogDraftDao;
import com.ncoxs.myblog.dao.mysql.UploadImageBindDao;
import com.ncoxs.myblog.dao.mysql.UploadImageDao;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserLoginResp;
import com.ncoxs.myblog.model.pojo.Blog;
import com.ncoxs.myblog.model.pojo.BlogDraft;
import com.ncoxs.myblog.model.pojo.UploadImage;
import com.ncoxs.myblog.testutil.BaseTester;
import com.ncoxs.myblog.testutil.EncryptionMockMvcBuilder;
import com.ncoxs.myblog.util.data.ResourceUtil;
import com.ncoxs.myblog.util.model.Tuple2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;

import static com.ncoxs.myblog.util.model.MapUtil.kv;
import static com.ncoxs.myblog.util.model.MapUtil.mp;
import static org.junit.jupiter.api.Assertions.*;

public class BlogEditControllerTest extends BaseTester implements InitializingBean {

    @Value("${myapp.website.url}")
    private String webSiteUrl;

    @Value("${myapp.website.image-dir}")
    private String imageDir;

    @Autowired
    BlogDraftDao blogDraftDao;

    @Autowired
    BlogDao blogDao;

    @Autowired
    UploadImageDao uploadImageDao;

    @Autowired
    UploadImageBindDao uploadImageBindDao;

    int imagePrefixLen;


    @Override
    public void afterPropertiesSet() throws Exception {
        imagePrefixLen = webSiteUrl.length() + imageDir.length() + 1;
    }


    /**
     * 测试上传博客草稿功能，针对上传和修改内容、图片这些要点进行了测试。
     */
    @Test
    @Transactional
    public void testUploadBlogDraft() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 上传图片 test1.gif
        String imageUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test1.gif");
        // 上传图片 test2.jpeg
        String imageUrl2 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test2.jpeg");

        // assert session
        //noinspection unchecked
        Map<String, Integer> imageCache = (Map<String, Integer>) tuple.t2.getAttribute("uploadImages");
        assertEquals(2, imageCache.size());
        assertEquals(new HashSet<>(Arrays.asList(imageUrl1.substring(imagePrefixLen), imageUrl2.substring(imagePrefixLen))),
                imageCache.keySet());

        // 读取博客草稿正文并替换图片 url
        String blogBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        blogBody = blogBody.replace("url-placeholder1", imageUrl1);
        blogBody = blogBody.replace("url-placeholder2", imageUrl2);

        // 上传新的博客草稿
        BlogEditController.BlogDraftParams params = new BlogEditController.BlogDraftParams();
        params.title = "标题1";
        params.setMarkdownBody(blogBody);
        params.isAllowReprint = true;
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

        // assert 图片记录
        List<UploadImage> uploadImages = uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG_DRAFT, result2.getData());
        assertEquals(2, uploadImages.size());
        // 记录的插入顺序是随机的，所以需要用 set 来判断
        assertEquals(new HashSet<>(Arrays.asList(imageUrl1.substring(imagePrefixLen), imageUrl2.substring(imagePrefixLen))),
                new HashSet<>(Arrays.asList(uploadImages.get(0).getFilepath(), uploadImages.get(1).getFilepath())));
        assertNotNull(uploadImageDao.selectById(uploadImages.get(0).getId()));
        assertNotNull(uploadImageDao.selectById(uploadImages.get(1).getId()));

        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(0).getFilepath())).isFile());
        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(1).getFilepath())).isFile());

        // assert session
        int cnt = 0;
        Enumeration<String> attributeNames = tuple.t2.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            attributeNames.nextElement();
            cnt++;
        }
        // 上传博客草稿后，session 里面的属性应该有用户登录 token 的两个，以及图片 token map
        assertEquals(3, cnt);
        // assert 此时图片记录都已经从 cache 中删除
        assertEquals(0, imageCache.size());


        // 上传图片 test3.jpeg
        String imageUrl3 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test3.jpeg");

        // 读取修改的博客草稿正文
        blogBody = ResourceUtil.loadString("markdown/test-blog-update.md");
        blogBody = blogBody.replace("url-placeholder1", imageUrl1);
        // 修改博客草稿中的一张图片
        blogBody = blogBody.replace("url-placeholder2", imageUrl3);

        // 修改博客草稿
        params = new BlogEditController.BlogDraftParams();
        // 修改博客草稿，上传 id
        params.setId(blogDraftId);
        params.title = "标题2";
        params.setMarkdownBody(blogBody);
        params.isAllowReprint = false;
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

        uploadImages = uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG_DRAFT, result2.getData());
        assertEquals(3, uploadImages.size());
        // 修改的图片
        assertTrue(imageUrl3.endsWith(uploadImages.get(2).getFilepath()));

        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(0).getFilepath())).isFile());
        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(1).getFilepath())).isFile());

        // assert session 图片记录
        assertEquals(1, imageCache.size());
        assertEquals(new HashSet<>(Collections.singletonList(imageUrl2.substring(imagePrefixLen))),
                imageCache.keySet());

        // 由于 Mock Session 不能触发 SessionListener 的销毁方法，因此无法验证 session 销毁时销毁图片的逻辑
//        // 被删除的图片记录
//        assertNull(uploadImageDao.selectById(deleteId));
//        // 测试被删除的图片
//        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.classpath("static/img/" + deleteImagePath));
    }

    /**
     * 测试上传博客草稿封面。
     */
    @Test
    @Transactional
    public void testUploadBlogDraftCover() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 上传图片 test1.gif 作为博客封面
        String coverUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT_COVER, "test1.gif");

        // 上传新的博客草稿
        String blogBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        BlogEditController.BlogDraftParams params = new BlogEditController.BlogDraftParams();
        params.title = "标题1";
        params.setMarkdownBody(blogBody);
        params.isAllowReprint = true;
        // 将 coverUrl1 作为博客封面
        params.setCoverUrl(coverUrl1);
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
        String coverUrl2 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT_COVER, "test2.jpeg");
        // 修改博客封面
        params = new BlogEditController.BlogDraftParams();
        params.setId(result2.getData());
        // 设置新的封面
        params.setCoverUrl(coverUrl2);
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

        // assert 将被删除的封面
        //noinspection unchecked
        Map<String, Integer> imageCache = (Map<String, Integer>) tuple.t2.getAttribute("uploadImages");
        assertEquals(1, imageCache.size());
        assertEquals(new HashSet<>(Collections.singletonList(deleteCoverPath1)),
                imageCache.keySet());


        // 上传新的博客草稿，并使用之前的博客草稿的封面，测试是否会出现预期的错误
        blogBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        params = new BlogEditController.BlogDraftParams();
        params.title = "标题222";
        params.setMarkdownBody(blogBody);
        params.isAllowReprint = true;
        // 将 coverUrl1 作为博客封面
        params.setCoverUrl(coverUrl2);
        params.setUserLoginToken(tuple.t1.getToken());

        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/draft/upload")
                .jsonParams(params)
                .session(tuple.t2)
                .sendRequest()
                .print()
                .buildByte();
        result2 = objectMapper.readValue(data,
                new TypeReference<GenericResult<Integer>>() {
                });
        // assert 出现返回错误
        assertNull(result2.getData());
        assertEquals(ResultCode.DATA_ACCESS_DENIED.getCode(), result2.getCode());


        // 使用外链作为新的博客封面
        String outerUrl = "https://cn.bing.com/images/cat.png";
        // 修改博客封面
        params = new BlogEditController.BlogDraftParams();
        params.setId(result2.getData());
        params.setCoverUrl(outerUrl);
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
        assertEquals(outerUrl, blogDraft.getCoverPath());
    }

    /**
     * 测试发表/修改博客
     */
    @Test
    @Transactional
    public void testPublishBlog() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 一般是先上传博客草稿的图片
        // 上传图片 test1.gif
        String imageUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test1.gif");
        // 上传图片 test2.jpeg
        String imageUrl2 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test2.jpeg");

        // 读取博客正文并替换图片 url
        String markdownBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        markdownBody = markdownBody.replace("url-placeholder1", imageUrl1);
        markdownBody = markdownBody.replace("url-placeholder2", imageUrl2);

        // 上传新的博客
        BlogEditController.BlogParams params = new BlogEditController.BlogParams();
        params.title = "标题1";
        params.wordCount = 1000;
        params.setMarkdownBody(markdownBody);
        params.isAllowReprint = true;
        params.setUserLoginToken(tuple.t1.getToken());

        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/publish")
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
        int blogId = result2.getData();

        // assert 博客上传
        Blog blog = blogDao.selectById(blogId);
        assertNotNull(blog);
        assertEquals(markdownBody, blog.getMarkdownBody());
        assertEquals(params.wordCount, blog.getWordCount());
        assertEquals(params.title, blog.getTitle());
        assertEquals(params.isAllowReprint, blog.getIsAllowReprint());

        List<UploadImage> uploadImages = uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG, result2.getData());
        assertEquals(2, uploadImages.size());
        assertEquals(UploadImageTargetType.BLOG, uploadImages.get(0).getTargetType());
        assertEquals(UploadImageTargetType.BLOG, uploadImages.get(1).getTargetType());
        // 记录的插入顺序是随机的，所以需要用 set 来判断
        assertEquals(new HashSet<>(Arrays.asList(imageUrl1.substring(imagePrefixLen), imageUrl2.substring(imagePrefixLen))),
                new HashSet<>(Arrays.asList(uploadImages.get(0).getFilepath(), uploadImages.get(1).getFilepath())));
        assertNotNull(uploadImageDao.selectById(uploadImages.get(0).getId()));
        assertNotNull(uploadImageDao.selectById(uploadImages.get(1).getId()));

        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(0).getFilepath())).isFile());
        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(1).getFilepath())).isFile());

        int cnt = 0;
        Enumeration<String> attributeNames = tuple.t2.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            attributeNames.nextElement();
            cnt++;
        }
        // 上传博客后，session 里面的属性应该有用户登录 token 的两个，以及图片 token map
        assertEquals(3, cnt);



        // 上传图片 test3.jpeg
        String imageUrl3 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test3.jpeg");

        // 读取修改的博客正文
        markdownBody = ResourceUtil.loadString("markdown/test-blog-update.md");
        markdownBody = markdownBody.replace("url-placeholder1", imageUrl1);
        // 修改博客中的一张图片
        markdownBody = markdownBody.replace("url-placeholder2", imageUrl3);

        // 修改博客
        params = new BlogEditController.BlogParams();
        // 修改博客，上传 id
        params.setId(blogId);
        params.title = "标题2";
        params.wordCount = 900;
        params.setMarkdownBody(markdownBody);
        params.isAllowReprint = false;
        params.setUserLoginToken(tuple.t1.getToken());

        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/publish")
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

        // assert 博客的修改
        blog = blogDao.selectById(result2.getData());
        assertNotNull(blog);
        assertEquals(markdownBody, blog.getMarkdownBody());
        assertEquals(params.wordCount, blog.getWordCount());
        assertEquals(params.title, blog.getTitle());
        assertEquals(params.isAllowReprint, blog.getIsAllowReprint());

        uploadImages = uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG, result2.getData());
        assertEquals(3, uploadImages.size());
        // 修改的图片
        assertTrue(imageUrl3.endsWith(uploadImages.get(2).getFilepath()));

        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(0).getFilepath())).isFile());
        assertTrue(new File(ResourceUtil.classpath("static/img/" + uploadImages.get(1).getFilepath())).isFile());

        // assert session 图片记录
        //noinspection unchecked
        Map<String, Integer> imageCache = (Map<String, Integer>) tuple.t2.getAttribute("uploadImages");
        assertEquals(1, imageCache.size());
        assertEquals(new HashSet<>(Collections.singletonList(imageUrl2.substring(imagePrefixLen))),
                imageCache.keySet());
    }

    /**
     * 测试将博客草稿发表为博客
     */
    @Test
    @Transactional
    public void testPublishBlogByDraft() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 上传图片 test1.gif
        String imageUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test1.gif");
        // 上传图片 test2.jpeg
        String imageUrl2 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test2.jpeg");
        // 上传封面 test3.jpeg
        String coverUrl = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT_COVER, "test3.jpeg");

        // 读取博客草稿正文并替换图片 url
        String blogBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        blogBody = blogBody.replace("url-placeholder1", imageUrl1);
        blogBody = blogBody.replace("url-placeholder2", imageUrl2);

        // 上传新的博客草稿
        BlogEditController.BlogDraftParams params = new BlogEditController.BlogDraftParams();
        params.title = "标题1";
        params.setMarkdownBody(blogBody);
        params.setCoverUrl(coverUrl);
        params.isAllowReprint = true;
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



        // 将博客草稿发表为博客
        BlogEditController.PublishDraftParams publishDraftParams = new BlogEditController.PublishDraftParams();
        publishDraftParams.blogDraftId = blogDraftId;
        publishDraftParams.wordCount = 1000;
        publishDraftParams.setUserLoginToken(tuple.t1.getToken());

        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/draft/publish")
                .jsonParams(publishDraftParams)
                .session(tuple.t2)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        result2 = objectMapper.readValue(data,
                new TypeReference<GenericResult<Integer>>() {
                });
        assertNotNull(result2.getData());

        // assert 博客发表
        assertNull(blogDraftDao.selectById(blogDraftId));
        // assert 博客中的图片
        List<UploadImage> uploadImages = uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG, result2.getData());
        assertEquals(2, uploadImages.size());
        assertEquals(new HashSet<>(Arrays.asList(imageUrl1.substring(imagePrefixLen), imageUrl2.substring(imagePrefixLen))),
                new HashSet<>(Arrays.asList(uploadImages.get(0).getFilepath(), uploadImages.get(1).getFilepath())));
        // assert 博客自身
        Blog blog = blogDao.selectById(result2.getData());
        assertNotNull(blog);
        assertEquals(params.title, blog.getTitle());
        assertEquals(params.getMarkdownBody(), blog.getMarkdownBody());
        assertEquals(publishDraftParams.wordCount, blog.getWordCount());
        // assert 博客封面
        uploadImages = uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG_COVER, result2.getData());
        assertEquals(1, uploadImages.size());
        assertEquals(coverUrl.substring(imagePrefixLen), uploadImages.get(0).getFilepath());
        assertEquals(coverUrl.substring(imagePrefixLen), blog.getCoverPath());
    }

    /**
     * 测试获取编辑数据的接口。
     */
    @Test
    @Transactional
    public void testGetEditData() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 上传图片 test1.gif
        String imageUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test1.gif");
        // 上传图片 test2.jpeg
        String imageUrl2 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test2.jpeg");

        // 上传图片 test3.jpeg 作为博客封面
        String coverUrl = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT_COVER, "test3.jpeg");

        // 读取博客草稿正文并替换图片 url
        String blogBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        blogBody = blogBody.replace("url-placeholder1", imageUrl1);
        blogBody = blogBody.replace("url-placeholder2", imageUrl2);

        // 上传新的博客草稿
        BlogEditController.BlogDraftParams params = new BlogEditController.BlogDraftParams();
        params.title = "标题1";
        params.setMarkdownBody(blogBody);
        params.isAllowReprint = true;
        params.setCoverUrl(coverUrl);
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
        assertEquals(blogBody, blogDraft.getMarkdownBody());
        assertEquals(params.title, blogDraft.getTitle());
        assertEquals(params.isAllowReprint, blogDraft.getIsAllowReprint());


        // 获取博客草稿内容
        data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/draft/get-for-edit")
                .jsonParams(mp(kv("id", blogDraft.getId()), kv("userLoginToken", tuple.t1.getToken())))
                .session(tuple.t2)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildByte();
        GenericResult<BlogEditController.EditResp> result3 = objectMapper.readValue(data,
                new TypeReference<GenericResult<BlogEditController.EditResp>>() {
                });

        // assert 获取博客草稿内容
        BlogEditController.EditResp editResp = result3.getData();
        assertNotNull(editResp);
        assertEquals(blogDraft.getTitle(), editResp.getTitle());
        assertEquals(blogDraft.getMarkdownBody(), editResp.getMarkdownBody());
        assertEquals(blogDraft.getIsAllowReprint(), editResp.getIsAllowReprint());
        assertEquals(blogDraft.getCreateTime(), editResp.getCreateTime());
        assertEquals(blogDraft.getModifyTime(), editResp.getModifyTime());
        assertEquals(coverUrl, editResp.getCoverUrl());
    }

    /**
     * 测试删除博客草稿
     */
    @Test
    @Transactional
    public void testDeleteDraft() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 上传图片 test1.gif
        String imageUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test1.gif");
        // 上传图片 test2.jpeg
        String imageUrl2 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test2.jpeg");
        // 上传封面 test3.jpeg
        String coverUrl = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT_COVER, "test3.jpeg");

        // 读取博客草稿正文并替换图片 url
        String blogBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        blogBody = blogBody.replace("url-placeholder1", imageUrl1);
        blogBody = blogBody.replace("url-placeholder2", imageUrl2);

        // 上传新的博客草稿
        BlogEditController.BlogDraftParams params = new BlogEditController.BlogDraftParams();
        params.title = "标题1";
        params.setMarkdownBody(blogBody);
        params.setCoverUrl(coverUrl);
        params.isAllowReprint = true;
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


        // 发送删除博客草稿请求
        BlogEditController.IdParams idParams = new BlogEditController.IdParams();
        idParams.setId(blogDraftId);
        idParams.setUserLoginToken(tuple.t1.getToken());
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .delete("/blog/draft/delete")
                .jsonParams(idParams)
                .session(tuple.t2)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());

        // assert 博客草稿删除
        assertNull(blogDraftDao.selectById(blogDraftId));
        assertTrue(uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG_DRAFT, blogDraftId).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.classpath("static/img/" + imageUrl1.substring(imagePrefixLen)));
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.classpath("static/img/" + imageUrl2.substring(imagePrefixLen)));
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.classpath("static/img/" + coverUrl.substring(imagePrefixLen)));
    }

    @Test
    @Transactional
    public void testDeleteBlog() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 一般是先上传博客草稿的图片
        // 上传图片 test1.gif
        String imageUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test1.gif");
        // 上传图片 test2.jpeg
        String imageUrl2 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT, "test2.jpeg");

        // 读取博客正文并替换图片 url
        String markdownBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        markdownBody = markdownBody.replace("url-placeholder1", imageUrl1);
        markdownBody = markdownBody.replace("url-placeholder2", imageUrl2);

        // 上传新的博客
        BlogEditController.BlogParams params = new BlogEditController.BlogParams();
        params.title = "标题1";
        params.wordCount = 1000;
        params.setMarkdownBody(markdownBody);
        params.isAllowReprint = true;
        params.setUserLoginToken(tuple.t1.getToken());

        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/publish")
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
        int blogId = result2.getData();


        // 发送删除博客请求
        BlogEditController.IdParams idParams = new BlogEditController.IdParams();
        idParams.setId(blogId);
        idParams.setUserLoginToken(tuple.t1.getToken());
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .delete("/blog/delete")
                .jsonParams(idParams)
                .session(tuple.t2)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());

        // assert 博客草稿删除
        assertEquals(BlogStatus.DELETED, blogDao.selectById(blogId).getStatus());
    }

    /**
     * 测试删除博客封面
     */
    @Test
    @Transactional
    public void testDeleteBlogDraftCover() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 上传图片 test1.gif 作为博客封面
        String coverUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT_COVER, "test1.gif");

        // 上传新的博客草稿
        String blogBody = ResourceUtil.loadString("markdown/test-blog-new.md");
        BlogEditController.BlogDraftParams params = new BlogEditController.BlogDraftParams();
        params.title = "标题1";
        params.setMarkdownBody(blogBody);
        params.isAllowReprint = true;
        // 将 coverUrl1 作为博客封面
        params.setCoverUrl(coverUrl1);
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


        // 发送删除博客草稿封面请求
        BlogEditController.IdParams idParams = new BlogEditController.IdParams();
        idParams.setId(blogDraftId);
        idParams.setUserLoginToken(tuple.t1.getToken());
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .delete("/blog/draft/cover/delete")
                .jsonParams(idParams)
                .session(tuple.t2)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());

        // assert 删除博客封面
        assertEquals("", blogDraftDao.selectById(blogDraftId).getCoverPath());
        assertTrue(uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG_DRAFT_COVER, blogDraftId).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.classpath("static/img/" + coverUrl1.substring(imagePrefixLen)));
    }

    @Test
    @Transactional
    public void testDeleteBlogCover() throws Exception {
        Tuple2<UserLoginResp, MockHttpSession> tuple = prepareUser();

        // 一般是先上传博客草稿的图片
        // 上传封面图片 test2.jpeg
        String coverUrl1 = uploadImage(tuple, UploadImageTargetType.BLOG_DRAFT_COVER, "test2.jpeg");

        // 读取博客正文并替换图片 url
        String markdownBody = ResourceUtil.loadString("markdown/test-blog-new.md");

        // 上传新的博客
        BlogEditController.BlogParams params = new BlogEditController.BlogParams();
        params.title = "标题1";
        params.wordCount = 1000;
        params.setMarkdownBody(markdownBody);
        // 设置封面 url
        params.setCoverUrl(coverUrl1);
        params.isAllowReprint = true;
        params.setUserLoginToken(tuple.t1.getToken());

        byte[] data = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .post("/blog/publish")
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
        int blogId = result2.getData();


        // 发送删除博客封面请求
        BlogEditController.IdParams idParams = new BlogEditController.IdParams();
        idParams.setId(blogId);
        idParams.setUserLoginToken(tuple.t1.getToken());
        GenericResult<Map<String, Object>> result = new EncryptionMockMvcBuilder(mockMvc, objectMapper)
                .delete("/blog/cover/delete")
                .jsonParams(idParams)
                .session(tuple.t2)
                .sendRequest()
                .expectStatusOk()
                .print()
                .buildGR();
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());

        // assert 删除博客封面
        assertEquals("", blogDao.selectById(blogId).getCoverPath());
        assertTrue(uploadImageBindDao.selectUploadImages(UploadImageTargetType.BLOG_COVER, blogId).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> ResourceUtil.classpath("static/img/" + coverUrl1.substring(imagePrefixLen)));
    }
}
