package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.UploadImage;
import com.ncoxs.myblog.model.pojo.UploadImageBind;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UploadImageBindDao {

    List<UploadImage> selectUploadImages(int targetType, int targetId);

    /**
     * 如果 imageId 不存在返回 true；而当 imageId 已经存在，看看 imageId 是否匹配 target。
     */
    boolean isImageIdMatchTarget(int imageId, int targetType, int targetId);

    /**
     * 只有当 imageId 不存在时才会插入
     */
    int insert(UploadImageBind record);

    int updateTarget(int oldTargetType, int oldTargetId, int newTargetType, int newTargetId);

    int deleteByTarget(int targetType, int targetId);

    int deleteByImageId(int imageId);
}