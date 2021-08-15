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

    int updateByIdSelective(UserLog record);

    boolean deleteById(Integer id);
}