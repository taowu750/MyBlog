package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.UploadImage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UploadImageDao {

    List<UploadImage> selectByTypeId(int targetType, int targetId);

    int insert(UploadImage record);

    boolean deleteById(Integer id);

    int deleteByTypeId(int targetType, int targetId);
}