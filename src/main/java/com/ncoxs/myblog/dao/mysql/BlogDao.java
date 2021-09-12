package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.Blog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogDao {

    Blog selectByPrimaryKey(Integer id);

    int insert(Blog record);

    int updateByIdSelective(Blog record);
}