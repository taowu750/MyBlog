package com.ncoxs.myblog.model.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * upload_img
 * @author 
 */
@Data
public class UploadImage implements Serializable {

    private Integer id;

    private Integer userId;

    /**
     * 图片的类型，参见 {@link com.ncoxs.myblog.constant.UploadImageTargetType}
     */
    private Integer targetType;

    /**
     * 图片在服务器上的相对路径
     */
    private String filepath;

    /**
     * 图片上传时的名称
     */
    private String originFileName;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}