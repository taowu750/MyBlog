package com.ncoxs.myblog.dao.mysql;

import com.ncoxs.myblog.model.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserDao {

    /**
     * 必须有 name、password、email、salt、state、stateNote 属性，其他可选。
     */
    boolean insertSelective(User user);

    User selectById(int id);

    User selectByName(String name);

    User selectByEmail(String email);

    /**
     * 根据 identity 和 source 选择 user。
     * 如果参数 source 为 null，则不使用此参数。
     */
    User selectByIdentity(String identity, String source);

    boolean existsName(String name);

    boolean existsEmail(String email);

    boolean existsByNameEmail(String name, String email);

    /**
     * 判断邮箱账号是否存在。注意 password 需要是原始密码。
     */
    boolean existsByEmailPassword(String email, String password);

    /**
     * 判断名称账号是否存在。注意 password 需要是原始密码。
     */
    boolean existsByNamePassword(String name, String password);

    boolean updateByIdSelective(User user);

    boolean deleteById(int id);

    boolean deleteByName(String name);
}