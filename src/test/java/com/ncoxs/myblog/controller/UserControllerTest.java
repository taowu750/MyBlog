package com.ncoxs.myblog.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.UserIdentityType;
import com.ncoxs.myblog.constant.UserState;
import com.ncoxs.myblog.dao.mysql.UserDao;
import com.ncoxs.myblog.dao.mysql.UserIdentityDao;
import com.ncoxs.myblog.dao.redis.RedisUserDao;
import com.ncoxs.myblog.model.dto.GenericResult;
import com.ncoxs.myblog.model.dto.UserAndIdentity;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.model.pojo.UserIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Autowired
    ObjectMapper objectMapper;


    @Test
    public void testRegister() throws Exception {
        mockMvc.perform(post("/user/register/{name}", "test")
                .param("email", "wutaoyx163@163.com")
                .param("password", "12345")
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(UserController.class))
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andDo(print());

        mockMvc.perform(post("/user/register/{name}", "test")
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

    private User registerTestUser() throws Exception {
        User user = new User();
        user.setName("test");
        user.setEmail("wutaoyx163@163.com");
        user.setPassword("12345");

        mockMvc.perform(post("/user/register/{name}", "test")
                .param("email", "wutaoyx163@163.com")
                .param("password", "12345"))
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()));

        return user;
    }

    @Test
    public void testAccountActivate() throws Exception {
        User user = registerTestUser();

        List<UserIdentity> identities = userIdentityDao.selectByUserName(user.getName());
        assertEquals(1, identities.size());
        UserIdentity identity = identities.get(0);
        assertEquals(UserIdentityType.ACTIVATE_IDENTITY.getType(), identity.getType());

        MvcResult mvcResult = mockMvc.perform(get("/user/account-activate/{identity}", identity.getIdentity()))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();
        ModelAndView mv = mvcResult.getModelAndView();
        assert mv != null;
        assertEquals("success", mv.getModel().get("result"));

        mvcResult = mockMvc.perform(get("/user/account-activate/{identity}", identity.getIdentity()))
                .andExpect(status().isOk())
                .andReturn();
        mv = mvcResult.getModelAndView();
        assert mv != null;
        assertEquals("non-exist", mv.getModel().get("result"));

        User savedUser = userDao.selectByName(user.getName());
        assertEquals(0, userIdentityDao.selectByUserName(user.getName()).size());
        assertEquals(user.getEmail(), savedUser.getEmail());
        assertEquals(UserState.NORMAL.getState(), savedUser.getState());
        assertTrue(savedUser.getId() != null && savedUser.getId() > 0);

        User cachedUser = redisUserDao.getUserByName(user.getName());
        assertEquals(user.getEmail(), cachedUser.getEmail());
        assertEquals(UserState.NORMAL.getState(), cachedUser.getState());
        assertEquals(savedUser.getId(), cachedUser.getId());

        userDao.deleteById(savedUser.getId());
        redisUserDao.deleteUserById(savedUser.getId());
    }

    private void activateTestUser() throws Exception {
        List<UserIdentity> identities = userIdentityDao.selectByUserName("test");
        assertEquals(1, identities.size());
        UserIdentity identity = identities.get(0);
        assertEquals(UserIdentityType.ACTIVATE_IDENTITY.getType(), identity.getType());

        MvcResult mvcResult = mockMvc.perform(get("/user/account-activate/{identity}", identity.getIdentity()))
                .andExpect(status().isOk())
                .andReturn();
        ModelAndView mv = mvcResult.getModelAndView();
        assert mv != null;
        assertEquals("success", mv.getModel().get("result"));
    }

    @Test
    public void testLogin() throws Exception {
        try {
            registerTestUser();
            activateTestUser();

            MvcResult mvcResult = mockMvc.perform(post("/user/login/name/{name}", "test")
                    .param("password", "12345")
                    .accept(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                    .andDo(print())
                    .andReturn();
            GenericResult<UserAndIdentity> result = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                    new TypeReference<GenericResult<UserAndIdentity>>(){});
            assertNotNull(result);
            assertNotNull(result.getData());

            UserAndIdentity userAndIdentity = result.getData();
            assertNotNull(userAndIdentity.getUser());
            assertNull(userAndIdentity.getIdentity());
            assertEquals("test", userAndIdentity.getUser().getName());
            assertNull(userAndIdentity.getUser().getPassword());

            mvcResult = mockMvc.perform(post("/user/login/email/{email}", "wutaoyx163@163.com")
                    .param("password", "12345")
                    .param("rememberDays", String.valueOf(10))
                    .param("source", "source")
                    .accept(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                    .andDo(print())
                    .andReturn();
            result = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                    new TypeReference<GenericResult<UserAndIdentity>>(){});
            assertNotNull(result);
            assertNotNull(result.getData());

            userAndIdentity = result.getData();
            assertNotNull(userAndIdentity.getUser());
            assertNotNull(userAndIdentity.getIdentity());
            assertEquals("test", userAndIdentity.getUser().getName());
            assertNull(userAndIdentity.getUser().getPassword());

            User savedUser = userDao.selectByIdentity(userAndIdentity.getIdentity(), "source");
            assertEquals(userAndIdentity.getUser().getId(), savedUser.getId());
        } finally {
            userIdentityDao.deleteByUsername("test");
            userDao.deleteByName("test");
            redisUserDao.deleteUserByName("test");
        }
    }
}
