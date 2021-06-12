package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDao {

    /**
     * 必须有 name、password、email、salt、state、stateNote 属性，其他可选。
     */
    boolean insertSelective(User user);

    User selectById(int id);

    User selectByName(String name);

    User selectByEmail(String email);

    User selectByIdentity(String identity, String source);

    boolean existsName(String name);

    boolean existsEmail(String email);

    boolean existsByNameEmail(String name, String email);

    boolean updateByIdSelective(User user);

    boolean deleteById(int id);

    boolean deleteByName(String name);
}