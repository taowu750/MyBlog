package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.BlogDraft;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogDraftDao {

    boolean isMatchIdAndUserId(int id, int userId);

    BlogDraft selectById(Integer id);

    int selectCountByUserId(int userId);

    String selectCoverPathById(int id);

    int insert(BlogDraft record);

    int updateById(BlogDraft record);

    int deleteById(Integer id);
}