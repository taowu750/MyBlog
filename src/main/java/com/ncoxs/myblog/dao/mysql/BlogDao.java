package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.Blog;

public interface BlogDao {

    Blog selectByPrimaryKey(Integer id);

    int insert(Blog record);

    int updateByIdSelective(Blog record);
}