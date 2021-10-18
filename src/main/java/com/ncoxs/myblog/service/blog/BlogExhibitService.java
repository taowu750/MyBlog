package com.ncoxs.myblog.service.blog;

import com.ncoxs.myblog.dao.mysql.BlogDao;
import com.ncoxs.myblog.dao.mysql.UserBasicInfoDao;
import com.ncoxs.myblog.model.dto.BlogThumbnail;
import com.ncoxs.myblog.model.dto.UserAbbrExhibitInfo;
import com.ncoxs.myblog.service.app.ImageService;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlogExhibitService {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    private ImageService imageService;

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    private BlogDao blogDao;

    @Autowired
    public void setBlogDao(BlogDao blogDao) {
        this.blogDao = blogDao;
    }

    private UserBasicInfoDao userBasicInfoDao;

    @Autowired
    public void setUserBasicInfoDao(UserBasicInfoDao userBasicInfoDao) {
        this.userBasicInfoDao = userBasicInfoDao;
    }


    /**
     * 获取博客简略信息，用于列表展示。
     */
    public BlogThumbnail getBlogThumbnail(int blogId) {
        if (blogDao.canExhibit(blogId)) {
            return null;
        }

        BlogThumbnail result = blogDao.selectThumbnail(blogId, 100);
        result.setCoverUrl(result.getCoverUrl() != null ? imageService.toImageUrl(result.getCoverUrl()) : null);

        UserAbbrExhibitInfo exhibitInfo = userBasicInfoDao.selectUserAbbrExhibitInfo(result.getUserId());
        result.setUsername(exhibitInfo.getName());
        result.setUserProfileImageUrl(imageService.toImageUrl(exhibitInfo.getProfilePicturePath()));

        return result;
    }
}
