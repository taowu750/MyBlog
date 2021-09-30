package com.ncoxs.myblog.handler.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ncoxs.myblog.exception.ImpossibleError;
import com.ncoxs.myblog.service.app.ImageService;
import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
@Component
public class SessionLifecycleListener implements HttpSessionListener {

    private UserService userService;

    private ImageService imageService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }


    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        // 删除用户登录验证数据
        try {
            userService.quitByToken(session);
        } catch (JsonProcessingException e) {
            throw new ImpossibleError(e);
        }
        // 删除用户上传的图片数据
        imageService.deleteImages(session);
    }
}
