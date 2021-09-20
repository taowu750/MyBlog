package com.ncoxs.myblog.handler.listener;

import com.ncoxs.myblog.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
@Component
public class SessionLifeCycleListener implements HttpSessionListener {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // 在 session 销毁时，删除用户 token
        HttpSession session = se.getSession();
        String token = (String) session.getAttribute(UserService.USER_LOGIN_SESSION_KEY);
        if (token != null) {
            userService.quitByToken(token);
        }
    }
}
