package com.ncoxs.myblog.service.blog;

import com.ncoxs.myblog.constant.user.UserLogType;
import com.ncoxs.myblog.dao.mysql.BlogDao;
import com.ncoxs.myblog.dao.mysql.BlogDraftDao;
import com.ncoxs.myblog.dao.mysql.UserLogDao;
import com.ncoxs.myblog.model.dto.UserAccessParams;
import com.ncoxs.myblog.model.pojo.BlogDraft;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.model.pojo.UserLog;
import com.ncoxs.myblog.service.user.UserService;
import com.ncoxs.myblog.util.general.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 用户写博客，先保存为草稿。用户首次键入内容时，就在服务器生成这个草稿的标识。
 *
 * 草稿上传机制是怎么样的？
 * - 每隔一段时间，发现内容改变了，就保存本地和上传一次。
 *
 * 草稿中的图片怎么处理？
 * - 每次传输图片时，还需要传递草稿标识。
 * - 当用户发表博客时，将博客中没有出现的图片删除掉。
 * - 用户不保存草稿了，就将草稿和已经保存的图片删除掉。
 *
 * 过期的草稿怎么处理？
 * - 定时任务，每隔一天清理一次
 */
@Service
public class BlogUploadService {

    @Value("${myapp.blog.draft.expire-days}")
    private int blogDraftExpireDays;

    private UserService userService;

    private UserLogDao userLogDao;

    private BlogDao blogDao;

    private BlogDraftDao blogDraftDao;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setUserLogDao(UserLogDao userLogDao) {
        this.userLogDao = userLogDao;
    }

    @Autowired
    public void setBlogDao(BlogDao blogDao) {
        this.blogDao = blogDao;
    }

    @Autowired
    public void setBlogDraftDao(BlogDraftDao blogDraftDao) {
        this.blogDraftDao = blogDraftDao;
    }

    /**
     * 用户写博客，先保存为草稿。用户进入博客页面时，就在服务器生成这个草稿的标识。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Integer generateBlogDraftId(UserAccessParams params) {
        User user = userService.accessByEmail(params.getEmail(), params.getPassword());
        if (user == null) {
            return null;
        }

        // 插入空的草稿，生成草稿 id
        BlogDraft blogDraft = new BlogDraft();
        blogDraft.setUserId(user.getId());
        blogDraft.setExpire(TimeUtil.changeDateTime(blogDraftExpireDays, TimeUnit.DAYS));
        blogDraftDao.insert(blogDraft);

        // 插入用户日志
        userLogDao.insert(new UserLog(user.getId(), UserLogType.CREATE_BLOG_DRAFT, ""));

        return blogDraft.getId();
    }
}
