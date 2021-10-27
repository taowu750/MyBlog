package com.ncoxs.myblog.service.app;

import com.ncoxs.myblog.testutil.BaseTester;
import com.ncoxs.myblog.util.general.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@Slf4j
public class MarkdownServiceTest extends BaseTester {

    private MarkdownService markdownService;

    @Autowired
    public void setMarkdownService(MarkdownService markdownService) {
        this.markdownService = markdownService;
    }


    @Test
    public void testParseUsedImages() {
        String testMd = ResourceUtil.loadString("markdown/test.md");
        Set<String> images = markdownService.parseImagePathsFromMarkdown(testMd);
        log.info(images.toString());
    }
}
