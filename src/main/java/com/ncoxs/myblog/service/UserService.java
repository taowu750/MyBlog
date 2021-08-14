package com.ncoxs.myblog.service;

import com.ncoxs.myblog.constant.EmailTemplate;
import com.ncoxs.myblog.constant.UserIdentityType;
import com.ncoxs.myblog.constant.UserState;
import com.ncoxs.myblog.dao.mysql.UserDao;
import com.ncoxs.myblog.dao.mysql.UserIdentityDao;
import com.ncoxs.myblog.dao.redis.RedisUserDao;
import com.ncoxs.myblog.model.dto.UserAndIdentity;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.model.pojo.UserIdentity;
import com.ncoxs.myblog.util.general.PasswordUtil;
import com.ncoxs.myblog.util.general.TimeUtil;
import com.ncoxs.myblog.util.general.URLUtil;
import com.ncoxs.myblog.util.general.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.lang.NonNull;
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

// TODO: 密码强度验证
// TODO: 要有定时清理数据库中过期数据的机制
// TODO: 用户登录等行为应该被记录
@Service
@PropertySource("classpath:app-props.properties")
public class UserService {

    @Value("${spring.mail.username}")
    private String mailSender;

    @Value("${user.activate-url}")
    private String activateUrl;

    @Value("${user.activate-expire-time}")
    private int activateExpireTime;

    @Value("${user.forget-password.aes-key}")
    private String forgetPasswordAesKey;

    @Value("${user.forget-password.url}")
    private String forgetPasswordUrl;

    @Value("${user.forget-password.url-expire}")
    private int forgetPasswordExpire;

    private UserDao userDao;
    private UserIdentityDao userIdentityDao;

    private RedisUserDao redisUserDao;

    private MailService mailService;

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
     *
     * 调用此方法时，user 参数的 name、email、password 属性必须提供。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public boolean registerUser(@NonNull User user) throws MessagingException {
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
    public User activateUser(@NonNull String identity) {
        // 先从缓存中获取未激活用户数据
        User user = redisUserDao.getAndDeleteNonActivateUser(identity);
        if (user == null) {
            // 没有再到数据库中找
            user = userDao.selectByIdentity(identity, "");
        }
        if (user == null)
            return null;

        // 如果用户已经激活，返回已激活
        if (!UserState.NOT_ACTIVATED.is(user.getState()))
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
        update.setState(UserState.NORMAL.getState());
        update.setStateNote(UserState.NORMAL.getStateNote());
        update.setLimitTime(TimeUtil.EMPTY_DATE);
        // 更新数据库中的用户状态
        userDao.updateByIdSelective(update);

        // 删除激活凭证
        userIdentityDao.deleteByIdentity(identity);

        // 将已激活的用户缓存到 redis 中
        user.setState(UserState.NORMAL.getState());
        user.setStateNote(UserState.NORMAL.getStateNote());
        user.setLimitTime(TimeUtil.EMPTY_DATE);
        redisUserDao.setUser(user);

        return user;
    }

    /**
     * 根据用户名和密码进行登录。如果选择了“记住我”，则还会返回新的登录标识。
     *
     * @param name         用户名
     * @param password     密码
     * @param rememberDays 记住多少天
     * @param source       客户端标识
     * @return User 对象和登录标识
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserAndIdentity loginUserByName(@NonNull String name, @NonNull String password,
                                           int rememberDays, @NonNull String source) {
        User user = redisUserDao.getUserByName(name);
        if (user == null) {
            user = userDao.selectByName(name);
            if (user == null) {
                return null;
            } else {
                // 将数据库中的用户数据再次缓存到 Redis 中
                redisUserDao.setUser(user);
            }
        }

        return loginUser(user, password, rememberDays, source);
    }

    /**
     * 根据用户邮箱和密码进行登录。如果选择了“记住我”，则还会返回新的登录标识。
     *
     * @param email        邮箱
     * @param password     密码
     * @param rememberDays 记住多少天
     * @param source       客户端标识
     * @return User 对象和登录标识
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserAndIdentity loginUserByEmail(@NonNull String email, @NonNull String password,
                                            int rememberDays, @NonNull String source) {
        User user = redisUserDao.getUserByEmail(email);
        if (user == null) {
            user = userDao.selectByEmail(email);
            if (user == null) {
                return null;
            }
            // 将数据库中的用户数据再次缓存到 Redis 中
            redisUserDao.setUser(user);
        }

        return loginUser(user, password, rememberDays, source);
    }

    UserAndIdentity loginUser(User user, String password, int rememberDays, String source) {
        UserAndIdentity result = new UserAndIdentity();
        // 比对密码是否相同
        String actualPassword = PasswordUtil.encrypt(password + user.getSalt());
        if (actualPassword.equals(user.getPassword())) {
            result.setUser(user);
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
        }

        return result;
    }

    /**
     * 根据用户标识和来源进行登录。如果不存在、标识或来源不对、已过期则返回 null。
     *
     * @param loginIdentity 用户登录标识
     * @param source 来源，用于唯一标识一个客户端
     * @return 登录成功返回 User 对象，否则返回 null
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public User loginByIdentity(@NonNull String loginIdentity, @NonNull String source) {
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
                return null;
            }
            // 将数据缓存到 redis 中
            user = userDao.selectByIdentity(loginIdentity, source);
            redisUserDao.setIdentity2Id(loginIdentity, source, user.getId());
            redisUserDao.setUser(user);
        }

        return user;
    }

    /**
     * 发送“忘记密码”邮件，用户点击邮件中的链接后即可使用设置的新密码。
     *
     * @param email 需要重置密码的用户邮箱
     * @param newPassword 新密码
     * @return 邮箱是否存在
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

    public String decryptForgetPasswordParams(@NonNull String encryptedParams) throws GeneralSecurityException, UnsupportedEncodingException {
        return URLUtil.decryptParams(forgetPasswordAesKey, encryptedParams);
    }

    public boolean existsEmail(@NonNull String email) {
        return redisUserDao.existsEmail(email) || userDao.existsEmail(email);
    }

    public boolean setNewPassword(@NonNull String email, @NonNull String newPassword) {
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
        updated.setPassword(PasswordUtil.encrypt(newPassword));
        userDao.updateByIdSelective(updated);

        // 更新 Redis 中的用户密码
        user.setPassword(newPassword);
        redisUserDao.setUser(user);

        return true;
    }

    /**
     * 将客户端传来的用户信息初始化可以插入数据库的用户对象。
     */
    public User initialUser(User user) {
        // 如果用户 note 是空字符串，将其设为 null
        if (!StringUtils.hasText(user.getNote()))
            user.setNote(null);
        // 为用户随机生成“密码盐”
        user.setSalt(PasswordUtil.generateSalt());
        // 将密码和“密码盐”组合，然后进行加密变成最终的密码
        user.setPassword(PasswordUtil.encrypt(user.getPassword() + user.getSalt()));
        // 设置用户状态为 NOT_ACTIVATED
        user.setState(UserState.NOT_ACTIVATED.getState());
        user.setStateNote(UserState.NOT_ACTIVATED.getStateNote());
        // 设置用户创建时间
        user.setCreateTime(new Date());
        user.setModifyTime(new Date(user.getCreateTime().getTime()));
        // 设置用户激活过期时间
        user.setLimitTime(TimeUtil.changeDateTime(user.getCreateTime(), activateExpireTime,
                TimeUnit.HOURS));

        return user;
    }

    /**
     * 创建一个用户激活标识对象。
     */
    public UserIdentity newUserActivateIdentity(User user, String activateIdentity) {
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setUserId(user.getId());
        userIdentity.setIdentity(activateIdentity);
        userIdentity.setType(UserIdentityType.ACTIVATE_IDENTITY.getType());
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
        userIdentity.setType(UserIdentityType.LOGIN_IDENTITY.getType());
        userIdentity.setCreateTime(new Date());
        userIdentity.setExpire(TimeUtil.changeDateTime(userIdentity.getCreateTime(), rememberDays,
                TimeUnit.DAYS));

        return userIdentity;
    }
}
