package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.SavedImageToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SavedImageTokenDao {

    SavedImageToken selectByToken(String token);

    String selectTokenByTarget(int targetType, int targetId);

    boolean insert(SavedImageToken record);

    int deleteByToken(String token);
}