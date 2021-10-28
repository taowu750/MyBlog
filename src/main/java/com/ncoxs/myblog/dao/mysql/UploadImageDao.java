package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.UploadImage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UploadImageDao {

    UploadImage selectById(int id);

    UploadImage selectByUserIdAndFilepath(int userId, String filepath);

    int insert(UploadImage record);

    int updateTargetTypeById(int id, int newTargetType);

    boolean deleteById(int id);
}