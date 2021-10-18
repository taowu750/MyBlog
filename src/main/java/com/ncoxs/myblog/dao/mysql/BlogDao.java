package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.dto.BlogThumbnail;
import com.ncoxs.myblog.model.pojo.Blog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogDao {

    boolean isMatchIdAndUserId(int id, int userId);

    Blog selectById(Integer id);

    /**
     * 获取博客缩略内容。
     *
     * @param abbrLength 缩略 markdown 长度
     */
    BlogThumbnail selectThumbnail(int id, int abbrLength);

    /**
     * 检测博客的状态是否可以展示
     */
    boolean canExhibit(int id);

    int insert(Blog record);

    int updateByIdSelective(Blog record);
}