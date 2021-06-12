package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserDaoTest {

    @Autowired
    UserDao userDao;

    @Autowired
    UserService userService;


    @Test
    public void testInsertSelective() {
        User user = new User();
        user.setName("test");
        user.setPassword("12345");
        user.setEmail("test@test");
        user = userService.initialUser(user);

        System.out.println(user);
        assertTrue(userDao.insertSelective(user));

        User u = userDao.selectById(user.getId());
        assertEquals(u.getId(), user.getId());
        assertEquals(u.getName(), user.getName());
        assertEquals(u.getPassword(), user.getPassword());
        assertEquals(u.getEmail(), user.getEmail());
        assertEquals(u.getSalt(), user.getSalt());
        assertEquals(u.getState(), user.getState());
        assertEquals(u.getStateNote(), user.getStateNote());

        System.out.println(u);
        userDao.deleteById(u.getId());
    }

    @Test
    public void testRepeatInsert() {
        User user = new User();
        user.setName("test");
        user.setPassword("12345");
        user.setEmail("test@test");
        user = userService.initialUser(user);

        assertTrue(userDao.insertSelective(user));
        assertFalse(userDao.insertSelective(user));

        userDao.deleteById(user.getId());
    }
}
