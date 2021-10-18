package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.dto.UserAbbrExhibitInfo;
import com.ncoxs.myblog.model.pojo.UserBasicInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBasicInfoDao {

    UserBasicInfo selectByUserId(int userId);

    /**
     * 获取用户简要展示信息
     */
    UserAbbrExhibitInfo selectUserAbbrExhibitInfo(int userId);

    int insert(UserBasicInfo record);

    int updateByUserId(UserBasicInfo record);
}