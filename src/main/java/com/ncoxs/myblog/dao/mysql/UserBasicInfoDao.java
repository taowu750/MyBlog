package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.UserBasicInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBasicInfoDao {

    UserBasicInfo selectById(Integer id);

    UserBasicInfo selectByUserId(int userId);

    /**
     * 参数中，用户 id 必须存在
     */
    int insertSelective(UserBasicInfo record);

    int updateByIdSelective(UserBasicInfo record);

    int deleteById(Integer id);
}