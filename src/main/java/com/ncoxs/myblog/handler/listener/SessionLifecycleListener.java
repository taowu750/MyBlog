package com.ncoxs.myblog.handler.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ncoxs.myblog.exception.ImpossibleError;
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

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }


    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        try {
            userService.quitByToken(session);
        } catch (JsonProcessingException e) {
            throw new ImpossibleError(e);
        }
    }
}
