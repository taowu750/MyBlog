package com.ncoxs.myblog.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户新增/修改/删除 markdown 文档的记录日志
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEditMarkdownLog {

    public static final String EDIT_TYPE_CREATE = "create";
    public static final String EDIT_TYPE_UPDATE = "update";
    public static final String EDIT_TYPE_DELETE = "delete";

    /**
     * 参见 {@link com.ncoxs.myblog.constant.UploadImageTargetType}
     */
    private int targetType;

    /**
     * markdown 文档 id
     */
    private int targetId;

    /**
     * 编辑类型，参见上面的常量
     */
    private String editType;
}
