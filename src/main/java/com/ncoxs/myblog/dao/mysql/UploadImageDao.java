package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.UploadImage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UploadImageDao {

    List<UploadImage> selectByToken(String token);

    UploadImage selectSingle(String token, int targetType);

    int insert(UploadImage record);

    boolean deleteById(int id);

    int deleteByToken(String token);
}