package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.UserBasicInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBasicInfoDao {

    UserBasicInfo selectByUserId(int userId);

    int insertSelective(UserBasicInfo record);

    int updateByUserIdSelective(UserBasicInfo record);
}