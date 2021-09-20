package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.UploadImage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UploadImageDao {

    List<UploadImage> selectByToken(String token);

    int insert(UploadImage record);

    int deleteByToken(String token);
}