package com.ncoxs.myblog.dao.redis;

import com.ncoxs.myblog.model.pojo.User;

public interface RedisUserDao {

    // RedisUserDao 所有键的顶级前缀
    String KEY_DOMAIN = "user";
    // RedisUserDao 键的二级前缀
    String KEY_ACTIVATE_IDENTITY2USER = "activate-identity2user";
    String KEY_ID2USER = "id2user";
    String KEY_NAME2ID = "name2id";
    String KEY_EMAIL2ID = "email2id";
    String KEY_IDENTITY2ID = "identity2id";
    String SKEY_ID2IDENTITIES = "id2identities";

    // 返回 User 的方法发生错误时返回此对象表示出错
    User USER_ERROR_RETURN = new User();

    /**
     * 在 redis 中缓存未激活用户，并设置超时时间
     */
    void setNonActivateUser(String activateIdentity, User user);

    /**
     * 返回缓存的未激活用户对象，并从缓存中移除。
     * 如果已经过期了就返回 null。
     */
    User getAndDeleteNonActivateUser(String activateIdentity);

    /**
     * 在 Redis 中缓存以下字符串键值对，并设置它们的超时时间
     * - user.id: user
     * - user.name: user.id
     * - user.email: user.id
     */
    void setUser(User user);

    boolean existsName(String name);

    boolean existsEmail(String email);

    /**
     * 判断 name 和 email 是否存在，并且是同一个用户的
     */
    boolean existsNameAndEmail(String name, String email);

    /**
     * 根据用户 id 获取缓存的用户对象，不存在返回 null。
     *
     * 如果成功获取到了，就更新与用户相关（id、name、email、identity）的缓存的过期时间
     */
    User getUserById(int id);

    /**
     * 根据用户 name 获取缓存的用户对象，不存在返回 null。
     *
     * 如果成功获取到了，就更新与用户相关（id、name、email、identity）的缓存的过期时间
     */
    User getUserByName(String name);

    /**
     * 根据用户 email 获取缓存的用户对象，不存在返回 null。
     *
     * 如果成功获取到了，就更新与用户相关（id、name、email、identity）的缓存的过期时间
     */
    User getUserByEmail(String email);

    /**
     * 根据用户 identity 和对应的 source 获取缓存的用户对象，不存在返回 null。
     *
     * 如果成功获取到了，就更新与用户相关（id、name、email、identity）的缓存的过期时间
     */
    User getUserByIdentity(String identity, String source);

    /**
     * 设置用户 identity 到 id 和 source 的键值对，并设置过期时间
     */
    void setIdentity2Id(String identity, String source, int id);

    /**
     * 根据用户 id 删除此用户的所有缓存数据，包括 id、name、email、identity 键值对。
     * 存在的话返回用户对象，否则返回 null
     */
    User deleteUserById(int id);

    /**
     * 根据用户 identity 和对应的 source 删除此用户的所有缓存数据，包括 id、name、email、identity 键值对。
     * 存在的话返回用户对象，否则返回 null
     */
    User deleteUserByIdentity(String identity, String source);
}
