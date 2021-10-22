package com.ncoxs.myblog.service.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.dao.mysql.UserLogDao;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarkdownService implements InitializingBean {

    @Value("${myapp.website.url}")
    private String webSiteUrl;

    @Value("${myapp.website.image-dir}")
    private String imageDir;

    private Pattern mdImagePattern;

    private Pattern imageUrlPattern;

    @Override
    public void afterPropertiesSet() {
        mdImagePattern = Pattern.compile("!\\[.+?]\\(" + webSiteUrl + imageDir + "/(.+?)\\)");
        imageUrlPattern = Pattern.compile(webSiteUrl + imageDir + "/(.+)");
    }


    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private ImageService imageService;

    @Autowired
    public void setNewImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    private UserLogDao userLogDao;

    @Autowired
    public void setUserLogDao(UserLogDao userLogDao) {
        this.userLogDao = userLogDao;
    }

    private ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    /**
     * 从 markdown 文档中解析出使用到的图片的相对路径。
     */
    public Set<String> parseImagePathsFromMarkdown(String markdown) {
        if (markdown == null) {
            return new HashSet<>();
        }

        Set<String> usedImages = new HashSet<>();
        Matcher matcher = mdImagePattern.matcher(markdown);
        while (matcher.find()) {
            String imageFilepath = matcher.group(1);
            usedImages.add(imageFilepath);
        }

        return usedImages;
    }

    /**
     * 从图片 url 中解析出使用到的图片的相对路径。
     */
    public String parseImagePathFromUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        Matcher matcher = imageUrlPattern.matcher(imageUrl);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        return null;
    }
}
