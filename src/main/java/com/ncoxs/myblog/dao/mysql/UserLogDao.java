package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.UserLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserLogDao {

    int insert(UserLog record);

    UserLog selectById(Integer id);

    List<UserLog> selectByUserIdType(int userId, int type);

    UserLog selectByUserIdTypeLatest(int userId, int type);

    UserLog selectByToken(String token);

    String selectDescriptionByToken(String token);

    boolean updateByIdSelective(UserLog record);

    boolean updateDescriptionByToken(String token, String description);

    boolean deleteById(Integer id);

    int deleteByUserId(int userId);

    int deleteAll();
}