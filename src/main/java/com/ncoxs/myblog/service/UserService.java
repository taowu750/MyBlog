package com.ncoxs.myblog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.EmailTemplate;
import com.ncoxs.myblog.constant.user.UserIdentityType;
import com.ncoxs.myblog.constant.user.UserLogType;
import com.ncoxs.myblog.constant.user.UserStatus;
import com.ncoxs.myblog.constant.user.UserType;
import com.ncoxs.myblog.controller.user.UserController;
import com.ncoxs.myblog.dao.mysql.UserDao;
import com.ncoxs.myblog.dao.mysql.UserIdentityDao;
import com.ncoxs.myblog.dao.mysql.UserLogDao;
import com.ncoxs.myblog.dao.redis.RedisUserDao;
import com.ncoxs.myblog.exception.UserLogException;
import com.ncoxs.myblog.model.bo.UserLoginLog;
import com.ncoxs.myblog.model.bo.UserRegisterLog;
import com.ncoxs.myblog.model.bo.UserUpdateLog;
import com.ncoxs.myblog.model.dto.IpLocInfo;
import com.ncoxs.myblog.model.dto.UserAndIdentity;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.model.pojo.UserIdentity;
import com.ncoxs.myblog.model.pojo.UserLog;
import com.ncoxs.myblog.util.general.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.concurrent.TimeUnit;


// TODO: 考虑和官方用户账号的兼容问题
// TODO: 密码强度验证
// TODO: 要有定时清理数据库中过期数据的机制

// TODO: 增加 user_basic_info，注册时需要插入数据
@Service
public class UserService {

    @Value("${spring.mail.username}")
    private String mailSender;

    @Value("${myapp.user.activate-url}")
    private String activateUrl;

    @Value("${myapp.user.activate-expire-time}")
    private int activateExpireTime;

    @Value("${myapp.user.forget-password.aes-key}")
    private String forgetPasswordAesKey;

    @Value("${myapp.user.forget-password.url}")
    private String forgetPasswordUrl;

    @Value("${myapp.user.forget-password.url-expire}")
    private int forgetPasswordExpire;

    private UserDao userDao;
    private UserIdentityDao userIdentityDao;

    private RedisUserDao redisUserDao;

    private MailService mailService;

    private UserLogDao userLogDao;

    private ObjectMapper objectMapper;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Autowired
    public void setUserIdentityDao(UserIdentityDao userIdentityDao) {
        this.userIdentityDao = userIdentityDao;
    }

    @Autowired
    public void setRedisUserDao(RedisUserDao redisUserDao) {
        this.redisUserDao = redisUserDao;
    }

    @Autowired
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    @Autowired
    public void setUserLogDao(UserLogDao userLogDao) {
        this.userLogDao = userLogDao;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    /**
     * 判断用户信息是否存在。
     * - 返回 0 表示用户名和邮箱都不存在
     * - 返回 1 表示用户名和邮箱匹配
     * - 返回 2 表示用户名和邮箱都存在，但是不匹配
     * - 返回 3 表示用户名存在，但邮箱不存在
     * - 返回 4 表示邮箱存在，但用户名不存在
     */
    public int existsUser(@NonNull String name, @NonNull String email) {
        boolean existsName = redisUserDao.existsName(name) || userDao.existsName(name);
        boolean existsEmail = redisUserDao.existsEmail(email) || userDao.existsEmail(email);
        boolean existsUser = redisUserDao.existsNameAndEmail(name, email)
                || userDao.existsByNameEmail(name, email);

        if (existsUser)
            return 1;
        else if (existsName && existsEmail)
            return 2;
        else if (existsName)
            return 3;
        else if (existsEmail)
            return 4;
        else
            return 0;
    }

    /**
     * 发送邮箱激活邮件（一定时间内有效）生成标识 ID，将标识 ID 和用户信息保存到数据库中。
     * <p>
     * 调用此方法时，user 参数的 name、email、password 属性必须提供。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public boolean registerUser(@NonNull User user, @Nullable IpLocInfo ipLocInfo)
            throws MessagingException, JsonProcessingException {
        // 先对客户端传递过来的用户对象初始化
        user = initialUser(user);
        // 尝试插入数据库，如果用户名或邮箱不重复则插入成功
        boolean isSuccess = userDao.insertSelective(user);
        if (!isSuccess)
            return false;

        // 生成用户激活标识（用户激活邮件中要用）
        String activateId = UUIDUtil.generate();
        // 将用户激活标识插入到数据库中
        UserIdentity userIdentity = newUserActivateIdentity(user, activateId);
        userIdentityDao.insert(userIdentity);

        // 插入用户注册日志
        userLogDao.insert(new UserLog(user.getId(), UserLogType.REGISTER, userIdentity.getIdentity(),
                objectMapper.writeValueAsString(new UserRegisterLog(user.getStatus(), DeviceUtil.fillIpLocInfo(ipLocInfo)))));

        // 将用户信息插入到 Redis 中
        redisUserDao.setNonActivateUser(activateId, user);

        // 发送用户激活模板邮件
        Context context = new Context();
        context.setVariable("username", user.getName());
        context.setVariable("activateUrl", activateUrl + activateId);
        mailService.sendTemplateEmail(EmailTemplate.USER_ACTIVATE_NAME, mailSender,
                user.getEmail(), EmailTemplate.USER_ACTIVATE_SUBJECT, context);

        return true;
    }

    public static final User USER_ACTIVATED = new User();
    public static final User USER_EXPIRED = new User();

    /**
     * 激活具有指定标识的用户。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public User activateUser(@NonNull String identity) throws JsonProcessingException {
        // 先从缓存中获取未激活用户数据
        User user = redisUserDao.getAndDeleteNonActivateUser(identity);
        if (user == null) {
            // 没有再到数据库中找
            user = userDao.selectByIdentity(identity, "");
        }
        if (user == null)
            return null;

        // 如果用户已经激活，返回已激活
        if (UserStatus.NOT_ACTIVATED != user.getStatus())
            return USER_ACTIVATED;

        // 如果已经过期，返回已过期
        if (user.getLimitTime().getTime() < System.currentTimeMillis()) {
            // 删除数据库中的记录
            userDao.deleteById(user.getId());
            userIdentityDao.deleteByUserId(user.getId());
            // 删除缓存中的记录
            redisUserDao.deleteUserById(user.getId());
            return USER_EXPIRED;
        }

        User update = new User();
        update.setId(user.getId());
        update.setStatus(UserStatus.NORMAL);
        update.setLimitTime(TimeUtil.EMPTY_DATE);
        // 更新数据库中的用户状态
        userDao.updateByIdSelective(update);

        // 删除激活凭证
        userIdentityDao.deleteByIdentity(identity);

        // 修改用户注册日志
        String userRegisterLogStr = userLogDao.selectDescriptionByToken(identity);
        if (userRegisterLogStr == null) {
            throw new UserLogException("没有 token 为 " + identity + " 的用户注册日志");
        }
        UserRegisterLog userRegisterLog = objectMapper.readValue(userRegisterLogStr, UserRegisterLog.class);
        userRegisterLog.setStatus(update.getStatus());
        userLogDao.updateDescriptionByToken(identity, objectMapper.writeValueAsString(userRegisterLog));

        // 将已激活的用户缓存到 redis 中
        user.setStatus(UserStatus.NORMAL);
        user.setLimitTime(TimeUtil.EMPTY_DATE);
        redisUserDao.setUser(user);

        return user;
    }

    /**
     * 根据用户名和密码进行登录。如果选择了“记住我”，则还会返回新的登录标识。
     *
     * @return User 对象和登录标识。如果返回值为 null，表示用户名不存在；如果返回值 user 属性为空，表示用户密码错误；
     * 如果 user 属性等于 {@link #USER_CANCELED}，表示用户账号已注销；否则登录成功
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserAndIdentity loginUserByName(UserController.LoginByNameParams params) throws JsonProcessingException {
        User user = redisUserDao.getUserByName(params.name);
        if (user == null) {
            user = userDao.selectByName(params.name);
            if (user == null) {
                return null;
            }
        }

        return loginUser("name", user, params.password, params.rememberDays, params.source,
                params.ipLocInfo);
    }

    /**
     * 根据用户邮箱和密码进行登录。如果选择了“记住我”，则还会返回新的登录标识。
     *
     * @return User 对象和登录标识。如果返回值为 null，表示用户邮箱不存在；如果返回值 user 属性为空，表示用户密码错误；
     * 如果 user 属性等于 {@link #USER_CANCELED}，表示用户账号已注销；否则登录成功
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserAndIdentity loginUserByEmail(UserController.LoginByEmailParams params) throws JsonProcessingException {
        User user = redisUserDao.getUserByEmail(params.email);
        if (user == null) {
            user = userDao.selectByEmail(params.email);
            if (user == null) {
                return null;
            }
        }

        return loginUser("email", user, params.password, params.rememberDays, params.source,
                params.ipLocInfo);
    }

    // 登录还需要检查用户状态，是否为已注销。封禁了还能登录，只是不能做除浏览外的其他操作
    UserAndIdentity loginUser(String loginType, User user, String password,
                              int rememberDays, String source, IpLocInfo ipLocInfo)
            throws JsonProcessingException {
        UserAndIdentity result = new UserAndIdentity();
        // 比对密码是否相同
        if (passwordEquals(user, password)) {
            // 不需要检查用户是否已注销，因为已注销的用户不会被查询到

            // 将数据库中的用户数据再次缓存到 Redis 中
            redisUserDao.setUser(user);

            result.setUser(user);
            // 插入登录成功日志
            userLogDao.insert(new UserLog(user.getId(), UserLogType.LOGIN, objectMapper.writeValueAsString(
                    new UserLoginLog("success", loginType, DeviceUtil.fillIpLocInfo(ipLocInfo)))));

            // 如果登录时选择了“记住我”的选项，则删除上一个登录标识，插入新的登录标识
            if (rememberDays > 0 && StringUtils.hasText(source)) {
                UserIdentity userIdentity = newLoginIdentity(user, rememberDays, source);
                String identity = userIdentity.getIdentity();
                result.setIdentity(identity);
                userIdentityDao.deleteByUserIdAndSource(user.getId(), source);
                userIdentityDao.insert(userIdentity);

                // 新的登录标识缓存到 redis 中
                redisUserDao.setIdentity2Id(identity, source, user.getId());
            }
        } else {
            // 插入登录密码错误日志
            userLogDao.insert(new UserLog(user.getId(), UserLogType.LOGIN, objectMapper.writeValueAsString(
                    new UserLoginLog("password-fail", loginType, DeviceUtil.fillIpLocInfo(ipLocInfo)))));
        }

        return result;
    }

    /**
     * 根据用户标识和来源进行登录。如果不存在、标识或来源不对、已过期则返回 null。
     *
     * @param loginIdentity 用户登录标识
     * @param source        来源，用于唯一标识一个客户端
     * @return 登录成功返回 User 对象，否则返回 null
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public User loginByIdentity(@NonNull String loginIdentity, @NonNull String source, @Nullable IpLocInfo ipLocInfo)
            throws JsonProcessingException {
        User user = redisUserDao.getUserByIdentity(loginIdentity, source);
        if (user == null) {
            UserIdentity userIdentity = userIdentityDao.selectByIdentity(loginIdentity, source);
            if (userIdentity == null) {
                return null;
            }
            // 过期则删除并返回 null
            if (!userIdentity.getExpire().equals(TimeUtil.EMPTY_DATE)
                    && userIdentity.getExpire().getTime() < System.currentTimeMillis()) {
                userIdentityDao.deleteByIdentity(loginIdentity);

                // 插入登录过期日志
                userLogDao.insert(new UserLog(userIdentity.getUserId(), UserLogType.LOGIN, objectMapper.writeValueAsString(
                        new UserLoginLog("identity-expire", "identity", DeviceUtil.fillIpLocInfo(ipLocInfo)))));
                return null;
            }

            // 将数据缓存到 redis 中
            user = userDao.selectByIdentity(loginIdentity, source);
            redisUserDao.setIdentity2Id(loginIdentity, source, user.getId());
            redisUserDao.setUser(user);
        }

        // 插入登录成功日志
        userLogDao.insert(new UserLog(user.getId(), UserLogType.LOGIN, objectMapper.writeValueAsString(
                new UserLoginLog("success", "identity", DeviceUtil.fillIpLocInfo(ipLocInfo)))));

        return user;
    }

    /**
     * 发送“忘记密码”邮件，用户点击邮件中的链接后即可使用设置的新密码。
     *
     * @param email       需要重置密码的用户邮箱
     * @param newPassword 新密码
     * @return 邮箱不存在，或新密码和旧密码相同，则返回 false；否则返回 true
     * @throws GeneralSecurityException
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public boolean sendForgetPasswordMail(@NonNull String email, @NonNull String newPassword)
            throws GeneralSecurityException, MessagingException, UnsupportedEncodingException {
        boolean exists = redisUserDao.existsEmail(email) || userDao.existsEmail(email);
        if (exists) {
            User user = redisUserDao.getUserByEmail(email);
            if (user == null) {
                user = userDao.selectByEmail(email);
            }

            // 新密码和老密码相同，返回 false
            if (passwordEquals(user, newPassword)) {
                return false;
            }

            // 拼接参数并加密
            String params = email + " " + newPassword + " " + TimeUtil.changeDateTime(forgetPasswordExpire, TimeUnit.HOURS).getTime();
            String encryptedParams = URLUtil.encryptParams(forgetPasswordAesKey, params);
            Context context = new Context();
            context.setVariable("username", user.getName());
            context.setVariable("resetPasswordUrl", forgetPasswordUrl + encryptedParams);
            // 发送重置密码邮件
            mailService.sendTemplateEmail(EmailTemplate.FORGET_PASSWORD_NAME, mailSender, email,
                    EmailTemplate.FORGET_PASSWORD_SUBJECT, context);
        }

        return exists;
    }

    /**
     * 解密忘记密码 URL 的参数
     */
    public String decryptForgetPasswordParams(@NonNull String encryptedParams) throws GeneralSecurityException, UnsupportedEncodingException {
        return URLUtil.decryptParams(forgetPasswordAesKey, encryptedParams);
    }

    /**
     * 判断是否存在 email
     */
    public boolean existsEmail(@NonNull String email) {
        return redisUserDao.existsEmail(email) || userDao.existsEmail(email);
    }

    /**
     * 为指定的邮箱账号，设置新的密码
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public boolean setNewPassword(int userLogType, @NonNull String email, @NonNull String newPassword)
            throws JsonProcessingException {
        User user = redisUserDao.getUserByEmail(email);
        if (user == null) {
            user = userDao.selectByEmail(email);
        }
        if (user == null) {
            return false;
        }

        // 更新数据库中的用户密码
        User updated = new User();
        updated.setId(user.getId());
        setPassword(updated, newPassword);
        userDao.updateByIdSelective(updated);

        // 写入用户更新密码日志
        userLogDao.insert(new UserLog(user.getId(), userLogType, objectMapper.writeValueAsString(
                new UserUpdateLog(user.getPassword(), updated.getPassword()))));

        // 更新 Redis 中的用户密码
        user.setSalt(updated.getSalt());
        user.setPassword(updated.getPassword());
        redisUserDao.setUser(user);

        return true;
    }

    /**
     * 修改用户密码。
     *
     * @return 如果邮箱和老密码存在，并且新旧密码不相同，返回 true；否则返回 false
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public boolean modifyPassword(UserController.ModifyPasswordParams params) throws JsonProcessingException {
        if (!params.oldPassword.equals(params.newPassword)
                && userDao.existsByEmailPassword(params.email, params.oldPassword)) {
            return setNewPassword(UserLogType.MODIFY_PASSWORD, params.email, params.newPassword);
        }

        return false;
    }

    /**
     * 修改用户名称。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public boolean modifyName(UserController.ModifyNameParams params) throws JsonProcessingException {
        // 如果新旧名称相同，返回 false
        if (params.oldName.equals(params.newName)) {
            return false;
        }

        User user = redisUserDao.getUserByName(params.oldName);
        if (user == null) {
            user = userDao.selectByName(params.oldName);
        }

        // 如果老用户名和密码匹配
        if (user != null && passwordEquals(user, params.password)) {
            // 更新数据库中的用户名
            User update = new User();
            update.setId(user.getId());
            update.setName(params.newName);
            userDao.updateByIdSelective(update);

            // 写入用户更新名称日志
            userLogDao.insert(new UserLog(user.getId(), UserLogType.MODIFY_NAME, objectMapper.writeValueAsString(
                    new UserUpdateLog(params.oldName, params.newName))));

            // 更新 redis 中的用户名
            user.setName(params.newName);
            redisUserDao.setUser(user);

            return true;
        }

        return false;
    }

    /**
     * 注销账号。
     */
    public boolean canceledAccount(@NonNull String email, @NonNull String password) {
        User user = redisUserDao.getUserByEmail(email);
        if (user == null) {
            user = userDao.selectByEmail(email);
            if (user == null) {
                return false;
            }
        }

        if (!passwordEquals(user, password)) {
            return false;
        }

        if (user.getStatus() != UserStatus.NORMAL) {
            return false;
        }

        // 删除 redis 中已注销账号的数据
        redisUserDao.deleteUserById(user.getId());

        // 更新用户的状态
        User update = new User();
        update.setId(user.getId());
        update.setStatus(UserStatus.CANCELLED);
        userDao.updateByIdSelective(update);

        return true;
    }

    public boolean passwordEquals(User user, String password) {
        return PasswordUtil.encrypt(password + user.getSalt()).equals(user.getPassword());
    }

    /**
     * 将客户端传来的用户信息初始化可以插入数据库的用户对象。
     */
    public User initialUser(User user) {
        // 设置用户类型为普通用户
        user.setType(UserType.PLAIN);
        // 设置用户密码
        setPassword(user, user.getPassword());
        // 设置用户状态为 NOT_ACTIVATED
        user.setStatus(UserStatus.NOT_ACTIVATED);
        // 设置用户创建时间
        user.setCreateTime(new Date());
        user.setModifyTime(new Date(user.getCreateTime().getTime()));
        // 设置用户激活过期时间
        user.setLimitTime(TimeUtil.changeDateTime(user.getCreateTime(), activateExpireTime,
                TimeUnit.HOURS));

        return user;
    }

    /**
     * 设置用户对象密码。
     */
    public void setPassword(User user, String password) {
        user.setSalt(PasswordUtil.generateSalt());
        user.setPassword(PasswordUtil.encrypt(password + user.getSalt()));
    }

    /**
     * 创建一个用户激活标识对象。
     */
    public UserIdentity newUserActivateIdentity(User user, String activateIdentity) {
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setUserId(user.getId());
        userIdentity.setIdentity(activateIdentity);
        userIdentity.setType(UserIdentityType.ACTIVATE_IDENTITY);
        userIdentity.setCreateTime(new Date());
        userIdentity.setExpire(user.getLimitTime());

        return userIdentity;
    }

    /**
     * 创建一个用户登录标识对象。
     *
     * @param user         用户对象
     * @param rememberDays 登录标识过期时间（天）
     * @param source       登录客户端来源
     * @return UserIdentity 对象
     */
    public UserIdentity newLoginIdentity(User user, int rememberDays, String source) {
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setUserId(user.getId());
        userIdentity.setIdentity(UUIDUtil.generate());
        userIdentity.setSource(source);
        userIdentity.setType(UserIdentityType.LOGIN_IDENTITY);
        userIdentity.setCreateTime(new Date());
        userIdentity.setExpire(TimeUtil.changeDateTime(userIdentity.getCreateTime(), rememberDays,
                TimeUnit.DAYS));

        return userIdentity;
    }
}
