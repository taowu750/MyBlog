package com.ncoxs.myblog.service.app;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用来处理上传的原始 Markdown 文档，其中可能包含 base64 格式的图片。
 */
@Service
public class MarkdownService implements InitializingBean {

    @Value("${myapp.website.url}")
    private String webSiteUrl;

    private Pattern mdImagePattern;


    @Override
    public void afterPropertiesSet() throws Exception {
        mdImagePattern = Pattern.compile("!\\[.+?\\]\\((.+?)\\)");
    }


    /**
     * 从 markdown 文档中解析出使用到的图片的相对路径。
     */
    public Set<String> parseUsedImages(String markdown) {
        Set<String> usedImages = new HashSet<>();
        Matcher matcher = mdImagePattern.matcher(markdown);
        while (matcher.find()) {
            String imageFilepath = matcher.group(1);
            if (imageFilepath.startsWith(webSiteUrl)) {
                usedImages.add(imageFilepath.substring(webSiteUrl.length() + 4));
            }
        }

        return usedImages;
    }
}
