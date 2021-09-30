package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.BlogDraft;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogDraftDao {

    BlogDraft selectById(Integer id);

    int insert(BlogDraft record);

    int updateById(BlogDraft record);

    int deleteById(Integer id);
}