package com.ncoxs.myblog.controller;

import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.UserIdentityType;
import com.ncoxs.myblog.constant.UserState;
import com.ncoxs.myblog.dao.mysql.UserDao;
import com.ncoxs.myblog.dao.mysql.UserIdentityDao;
import com.ncoxs.myblog.dao.redis.RedisUserDao;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.model.pojo.UserIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserController userController;

    @Autowired
    UserDao userDao;

    @Autowired
    UserIdentityDao userIdentityDao;

    @Autowired
    RedisUserDao redisUserDao;


    @Test
    public void testRegister() throws Exception {
        mockMvc.perform(put("/user/{name}", "test")
                .param("email", "wutaoyx163@163.com")
                .param("password", "12345")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(UserController.class))
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andDo(print());

        mockMvc.perform(put("/user/{name}", "test")
                .param("email", "wutaoyx163@163.com")
                .param("password", "12345")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(UserController.class))
                .andExpect(jsonPath("$.code").value(ResultCode.USER_HAS_EXISTED.getCode()))
                .andDo(print());

        User user = userDao.selectByName("test");
        List<UserIdentity> userIdentities = userIdentityDao.selectByUserId(user.getId());
        try {
            assertEquals(1, userIdentities.size());
            assertEquals((byte) userIdentities.get(0).getType(), UserIdentityType.ACTIVATE_IDENTITY.getType());
            User redisUser = redisUserDao.getAndDeleteNonActivateUser(userIdentities.get(0).getIdentity());
            assertTrue(UserState.NOT_ACTIVATED.is(user.getState()));
            assertEquals(user.getId(), redisUser.getId());
            assertEquals(user.getName(), redisUser.getName());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            userDao.deleteById(user.getId());
            userIdentityDao.deleteByUserId(user.getId());
            if (userIdentities.size() > 0)
                redisUserDao.getAndDeleteNonActivateUser(userIdentities.get(0).getIdentity());
        }
    }

    @Test
    public void testAccountActivate() throws Exception {
        User user = new User();
        user.setName("test");
        user.setEmail("wutaoyx163@163.com");
        user.setPassword("12345");

        GenericResult<Object> result = userController.register(1, user);
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());

        List<UserIdentity> identities = userIdentityDao.selectByUserName(user.getName());
        assertEquals(1, identities.size());
        UserIdentity identity = identities.get(0);
        assertEquals(UserIdentityType.ACTIVATE_IDENTITY.getType(), identity.getType());

        mockMvc.perform(get("/user/account-activate/{identity}", identity.getIdentity()));
    }

    /**
     * 此方法用来注册一个测试用户，方便之后的测试。
     */
    @Test
    public void registerUser() throws Exception {
        mockMvc.perform(put("/user/{name}", "test")
                .param("email", "wutaoyx163@163.com")
                .param("password", "12345")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(UserController.class))
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andDo(print());
    }

    /**
     * 清理所有测试用户数据。
     */
    @Test
    public void clearUser() {
        User user = userDao.selectByName("test");
        if (user != null) {
            userDao.deleteById(user.getId());
            List<UserIdentity> userIdentities = userIdentityDao.selectByUserId(user.getId());
            for (UserIdentity userIdentity : userIdentities) {
                if (UserIdentityType.ACTIVATE_IDENTITY.is(userIdentity.getType())) {
                    redisUserDao.getAndDeleteNonActivateUser(userIdentity.getIdentity());
                    break;
                } else {
                    redisUserDao.deleteUserByIdentity(userIdentity.getIdentity(), userIdentity.getSource());
                    break;
                }
            }
            userIdentityDao.deleteByUserId(user.getId());
            redisUserDao.deleteUserById(user.getId());
        }
    }
}
