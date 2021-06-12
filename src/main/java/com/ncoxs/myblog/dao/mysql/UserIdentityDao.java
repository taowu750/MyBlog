package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.UserIdentity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserIdentityDao {

    boolean insert(UserIdentity record);

    UserIdentity selectById(Integer id);

    List<UserIdentity> selectByUserId(int userId);

    List<UserIdentity> selectByUserName(String username);

    UserIdentity selectByIdentity(String identity, String source);

    boolean updateByIdSelective(UserIdentity record);

    boolean deleteById(Integer id);

    int deleteByUserId(int userId);

    boolean deleteByIdentity(String identity);

    boolean deleteByUserIdAndSource(int userId, String source);
}