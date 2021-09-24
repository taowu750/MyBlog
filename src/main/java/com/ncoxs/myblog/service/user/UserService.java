package com.ncoxs.myblog.service.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncoxs.myblog.constant.EmailTemplate;
import com.ncoxs.myblog.constant.ResultCode;
import com.ncoxs.myblog.constant.user.*;
import com.ncoxs.myblog.controller.user.UserController;
import com.ncoxs.myblog.dao.mysql.UserBasicInfoDao;
import com.ncoxs.myblog.dao.mysql.UserDao;
import com.ncoxs.myblog.dao.mysql.UserIdentityDao;
import com.ncoxs.myblog.dao.mysql.UserLogDao;
import com.ncoxs.myblog.dao.redis.RedisUserDao;
import com.ncoxs.myblog.exception.ImpossibleError;
import com.ncoxs.myblog.exception.UserLogException;
import com.ncoxs.myblog.model.bo.*;
import com.ncoxs.myblog.model.dto.IpLocInfo;
import com.ncoxs.myblog.model.dto.UserLoginResp;
import com.ncoxs.myblog.model.pojo.User;
import com.ncoxs.myblog.model.pojo.UserBasicInfo;
import com.ncoxs.myblog.model.pojo.UserIdentity;
import com.ncoxs.myblog.model.pojo.UserLog;
import com.ncoxs.myblog.service.app.MailService;
import com.ncoxs.myblog.service.app.VerificationCodeService;
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
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


// TODO: 考虑和官方用户账号的兼容问题
// TODO: 密码强度验证
// TODO: 要有定时清理数据库中过期数据的机制

@Service
public class UserService {

    @Value("${spring.mail.username}")
    private String mailSender;

    @Value("${myapp.user.activate.url}")
    private String activateUrl;

    @Value("${myapp.user.activate.expire-time}")
    private int activateExpireTime;

    @Value("${myapp.user.cancel.url}")
    private String cancelUrl;

    @Value("${myapp.user.cancel.expire-time}")
    private int cancelExpireTime;

    @Value("${myapp.user.cancel.aes-key}")
    private String cancelAesKey;

    @Value("${myapp.user.password-error.max-count}")
    private int passwordErrorMaxCount;

    @Value("${myapp.user.password-error.limit-minutes}")
    private int passwordErrorLimitMinutes;

    @Value("${myapp.user.forget-password.aes-key}")
    private String forgetPasswordAesKey;

    @Value("${myapp.user.forget-password.url}")
    private String forgetPasswordUrl;

    @Value("${myapp.user.forget-password.url-expire}")
    private int forgetPasswordExpire;

    @Value("${myapp.user.default-profile-picture-path}")
    private String defaultProfilePicturePath;

    @Value("${myapp.user.default-description}")
    private String defaultDescription;

    private UserDao userDao;
    private UserIdentityDao userIdentityDao;

    private RedisUserDao redisUserDao;

    private MailService mailService;

    private VerificationCodeService verificationCodeService;

    private UserLogDao userLogDao;

    private UserBasicInfoDao userBasicInfoDao;

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
    public void setVerificationCodeService(VerificationCodeService verificationCodeService) {
        this.verificationCodeService = verificationCodeService;
    }

    @Autowired
    public void setUserLogDao(UserLogDao userLogDao) {
        this.userLogDao = userLogDao;
    }

    @Autowired
    public void setUserBasicInfoDao(UserBasicInfoDao userBasicInfoDao) {
        this.userBasicInfoDao = userBasicInfoDao;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    /**
     * 用户每次登录，会生成一个 session 级别的 token，调用其他接口时需要使用这个 token 进行访问。
     */
    private static final String USER_LOGIN_SESSION_TOKEN = "userToken";

    private class PasswordErrorCounter {

        private final AtomicBoolean lock = new AtomicBoolean(false);

        private int count;
        private volatile long limitAt;

        void tryReset() {
            if (lock.compareAndSet(false, true)) {
                if (limitAt != 0 && limitAt < System.currentTimeMillis()) {
                    count = 0;
                    limitAt = 0;
                }
                lock.compareAndSet(true, false);
            }
        }

        void addCount() {
            while (!lock.compareAndSet(false, true)) {
            }

            try {
                if (count < passwordErrorMaxCount) {
                    count++;
                } else if (limitAt == 0) {
                    limitAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(passwordErrorLimitMinutes);
                }
            } finally {
                lock.compareAndSet(true, false);
            }
        }
    }

    /**
     * 用来记录某个用户密码错误次数和禁用时间的缓存 map。
     */
    private final ConcurrentHashMap<Integer, PasswordErrorCounter> passwordErrorMap = new ConcurrentHashMap<>();


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
    public ResultCode registerUser(UserController.RegisterParams params)
            throws MessagingException, JsonProcessingException {
        if (!verificationCodeService.verify(VerificationCodeService.SESSION_KEY_PLAIN_REGISTER, params.verificationCode)) {
            return ResultCode.PARAMS_VERIFICATION_CODE_ERROR;
        }

        // 先对客户端传递过来的用户对象初始化
        User user = initialUser(params.user);
        // 尝试插入数据库，如果用户名或邮箱不重复则插入成功
        boolean isSuccess = userDao.insertSelective(user);
        if (!isSuccess)
            return ResultCode.USER_HAS_EXISTED;

        // 生成用户激活标识（用户激活邮件中要用）
        String activateId = UUIDUtil.generate();
        // 将用户激活标识插入到数据库中
        UserIdentity userIdentity = newUserActivateIdentity(user, activateId);
        userIdentityDao.insert(userIdentity);

        // 插入用户注册日志
        userLogDao.insert(new UserLog(user.getId(), UserLogType.REGISTER, userIdentity.getIdentity(),
                objectMapper.writeValueAsString(new UserRegisterLog(user.getStatus(), DeviceUtil.fillIpLocInfo(params.ipLocInfo)))));

        // 将用户信息插入到 Redis 中
        redisUserDao.setNonActivateUser(activateId, user);

        // 发送用户激活模板邮件
        Context context = new Context();
        context.setVariable("username", user.getName());
        context.setVariable("activateUrl", activateUrl + activateId);
        mailService.sendTemplateEmail(EmailTemplate.USER_ACTIVATE_NAME, mailSender,
                user.getEmail(), EmailTemplate.USER_ACTIVATE_SUBJECT, context);

        return ResultCode.SUCCESS;
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

        // 插入用户基本信息
        UserBasicInfo userBasicInfo = new UserBasicInfo();
        userBasicInfo.setUserId(user.getId());
        userBasicInfo.setProfilePicturePath(defaultProfilePicturePath);
        userBasicInfo.setDescription(defaultDescription);
        userBasicInfoDao.insertSelective(userBasicInfo);

        // 将已激活的用户缓存到 redis 中
        user.setStatus(UserStatus.NORMAL);
        user.setLimitTime(TimeUtil.EMPTY_DATE);
        redisUserDao.setUser(user);

        return user;
    }

    /**
     * 判断用户密码重试次数是否超过最大次数。
     */
    private boolean canPasswordRetry(User user) {
        PasswordErrorCounter counter = passwordErrorMap.get(user.getId());
        boolean result = counter == null || counter.limitAt < System.currentTimeMillis();

        // 如果用户禁止访问时间已过期，则重置重试次数判断
        if (result && counter != null) {
            counter.tryReset();
        }

        return result;
    }

    /**
     * 累计用户密码重试次数
     */
    private void countPasswordError(User user) {
        PasswordErrorCounter counter;
        if (passwordErrorMap.containsKey(user.getId())) {
            counter = passwordErrorMap.get(user.getId());
        } else {
            counter = new PasswordErrorCounter();
            passwordErrorMap.put(user.getId(), counter);
        }

        counter.addCount();
    }

    private void removePasswordError(User user) {
        passwordErrorMap.remove(user.getId());
    }

    public static final UserLoginResp PASSWORD_RETRY_ERROR = new UserLoginResp();
    public static final UserLoginResp VERIFICATION_CODE_ERROR = new UserLoginResp();

    /**
     * 根据用户名和密码进行登录。如果选择了“记住我”，则还会返回新的登录标识。
     *
     * @return User 对象和登录标识。如果返回值为 null，表示用户名不存在；如果返回值 user 属性为空，表示用户密码错误；
     * 否则登录成功
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserLoginResp loginUserByName(UserController.LoginByNameParams params) throws JsonProcessingException {
        User user = redisUserDao.getUserByName(params.name);
        if (user == null) {
            user = userDao.selectByName(params.name);
            if (user == null) {
                return null;
            }
        }

        return loginUser("name", user, params.password, params.rememberDays, params.source,
                params.ipLocInfo, params.verificationCode);
    }

    /**
     * 根据用户邮箱和密码进行登录。如果选择了“记住我”，则还会返回新的登录标识。
     *
     * @return User 对象和登录标识。如果返回值为 null，表示用户邮箱不存在；如果返回值 user 属性为空，表示用户密码错误；
     * 否则登录成功
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserLoginResp loginUserByEmail(UserController.LoginByEmailParams params) throws JsonProcessingException {
        User user = redisUserDao.getUserByEmail(params.email);
        if (user == null) {
            user = userDao.selectByEmail(params.email);
            if (user == null) {
                return null;
            }
        }

        return loginUser("email", user, params.password, params.rememberDays, params.source,
                params.ipLocInfo, params.verificationCode);
    }

    // 封禁了还能登录，只是不能做除浏览外的其他操作
    UserLoginResp loginUser(String loginType, User user, String password,
                            int rememberDays, String source, IpLocInfo ipLocInfo,
                            String verificationCode)
            throws JsonProcessingException {
        if (!canPasswordRetry(user)) {
            return PASSWORD_RETRY_ERROR;
        }

        if (!verificationCodeService.verify(VerificationCodeService.SESSION_KEY_PLAIN_LOGIN, verificationCode)) {
            return VERIFICATION_CODE_ERROR;
        }

        UserLoginResp result = new UserLoginResp();
        // 比对密码是否相同
        if (passwordEquals(user, password)) {
            // 不需要检查用户是否已注销，因为已注销的用户不会被查询到

            // 将数据库中的用户数据再次缓存到 Redis 中
            redisUserDao.setUser(user);

            // 插入登录成功日志
            UserLog userLog = new UserLog(user.getId(), UserLogType.LOGIN, objectMapper.writeValueAsString(
                    new UserLoginLog("success", loginType, DeviceUtil.fillIpLocInfo(ipLocInfo))));
            userLogDao.insert(userLog);

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

            // 清除用户密码错误的重试次数
            removePasswordError(user);

            result.setUser(user);
            result.setToken(saveUserWithToken(user, userLog.getId()));
        } else {
            // 插入登录密码错误日志
            userLogDao.insert(new UserLog(user.getId(), UserLogType.LOGIN, objectMapper.writeValueAsString(
                    new UserLoginLog("password-fail", loginType, DeviceUtil.fillIpLocInfo(ipLocInfo)))));
            // 记录用户密码重试次数
            countPasswordError(user);
        }

        return result;
    }

    /**
     * 用户每次登录，会生成一个 session 级别的 token，调用其他接口时需要使用这个 token 进行访问。
     */
    private String saveUserWithToken(User user, int loginId) {
        return saveUserWithToken(user, UUIDUtil.generate(), loginId);
    }

    /**
     * 生成用户此次登录的 token，和用户对象相关联，并保存到 session 中。
     */
    private String saveUserWithToken(User user, String token, Integer loginId) {
        token = USER_LOGIN_SESSION_TOKEN + token;
        HttpSession session = SpringUtil.currentSession();
        // 注意，返回的 user 对象会被 FilterBlank 将密码设置为空，所以这里需要克隆一个
        session.setAttribute(token, new UserLoginHolder(user.clone(), loginId));

        return token;
    }

    /**
     * 根据用户标识和来源进行登录。如果不存在、标识或来源不对、已过期则返回 null。
     *
     * @param loginIdentity 用户登录标识
     * @param source        来源，用于唯一标识一个客户端
     * @return 登录成功返回 User 对象，否则返回 null
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserLoginResp loginByIdentity(@NonNull String loginIdentity, @NonNull String source, @Nullable IpLocInfo ipLocInfo)
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
        UserLog userLog = new UserLog(user.getId(), UserLogType.LOGIN, objectMapper.writeValueAsString(
                new UserLoginLog("success", "identity", DeviceUtil.fillIpLocInfo(ipLocInfo))));
        userLogDao.insert(userLog);

        UserLoginResp result = new UserLoginResp();
        result.setUser(user);
        result.setIdentity(loginIdentity);
        result.setToken(saveUserWithToken(user, userLog.getId()));

        return result;
    }

    /**
     * 通过 token 访问用户信息
     */
    public User accessByToken(String token) {
        Object obj = SpringUtil.currentSession().getAttribute(token);
        if (obj instanceof UserLoginHolder) {
            return ((UserLoginHolder) obj).getUser();
        }

        return null;
    }

    /**
     * 当用户关闭所有网页、或主动退出登录、或 session 过期时，需要删除登录 token
     */
    public boolean quitByToken(HttpSession session, String token, Integer logoutType) throws JsonProcessingException {
        Object obj = session.getAttribute(token);
        if (obj instanceof UserLoginHolder) {
            session.removeAttribute(token);
            // 记录登出日志
            if (logoutType != null) {
                UserLoginHolder holder = (UserLoginHolder) obj;
                userLogDao.insert(new UserLog(holder.getUser().getId(), UserLogType.LOGOUT, objectMapper.writeValueAsString(
                        new UserLogoutLog(logoutType, holder.getLoginLogId()))));
            }
        }

        return obj instanceof UserLoginHolder;
    }

    public void quitByToken(HttpSession session) throws JsonProcessingException {
        Enumeration<String> attrs = session.getAttributeNames();
        while (attrs.hasMoreElements()) {
            String attrName = attrs.nextElement();
            if (attrName.startsWith(USER_LOGIN_SESSION_TOKEN)) {
                quitByToken(session, attrName, UserLogoutType.INTERRUPTED);
                break;
            }
        }
    }

    public boolean quitByToken(String token, Integer logoutType) throws JsonProcessingException {
        return quitByToken(SpringUtil.currentSession(), token, logoutType);
    }

    public boolean quitByToken(String token) {
        try {
            return quitByToken(token, null);
        } catch (JsonProcessingException e) {
            throw new ImpossibleError(e);
        }
    }

    /**
     * 通过邮箱和密码获取用户信息
     *
     * @param email    邮箱
     * @param password 密码
     * @return 用户不存在或密码错误返回 null
     */
    public User accessByEmail(String email, String password) {
        User user = redisUserDao.getUserByEmail(email);
        if (user == null) {
            user = userDao.selectByEmail(email);
            if (user != null) {
                redisUserDao.setUser(user);
            }
        }
        if (user == null || !passwordEquals(user, password)) {
            return null;
        }

        return user;
    }

    /**
     * 发送“忘记密码”邮件，用户点击邮件中的链接后即可使用设置的新密码。
     *
     * @param email       需要重置密码的用户邮箱
     * @param newPassword 新密码
     * @throws GeneralSecurityException
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public ResultCode sendForgetPasswordMail(@NonNull String email, @NonNull String newPassword)
            throws GeneralSecurityException, MessagingException, UnsupportedEncodingException {
        if (!redisUserDao.existsEmail(email) && !userDao.existsEmail(email)) {
            return ResultCode.USER_NON_EXISTS;
        }

        User user = redisUserDao.getUserByEmail(email);
        if (user == null) {
            user = userDao.selectByEmail(email);
        }

        // 新密码和老密码相同
        if (passwordEquals(user, newPassword)) {
            return ResultCode.PARAM_MODIFY_SAME;
        }

        // 注意，返回的 user 对象会被 FilterBlank 将密码设置为空，所以这里需要克隆一个
        saveUserWithToken(user.clone(), email, null);
        // 拼接参数并加密
        String params = email + " " + newPassword + " " + TimeUtil.changeDateTime(forgetPasswordExpire, TimeUnit.HOURS).getTime();
        String encryptedParams = URLUtil.encryptParams(forgetPasswordAesKey, params);
        Context context = new Context();
        context.setVariable("username", user.getName());
        context.setVariable("resetPasswordUrl", forgetPasswordUrl + encryptedParams);
        // 发送重置密码邮件
        mailService.sendTemplateEmail(EmailTemplate.FORGET_PASSWORD_NAME, mailSender, email,
                EmailTemplate.FORGET_PASSWORD_SUBJECT, context);

        return ResultCode.SUCCESS;
    }

    /**
     * 解密忘记密码 URL 的参数
     */
    public String decryptForgetPasswordParams(@NonNull String encryptedParams) throws GeneralSecurityException, UnsupportedEncodingException {
        return URLUtil.decryptParams(forgetPasswordAesKey, encryptedParams);
    }

    /**
     * 通过用户登录 token，为用户设置新的密码。先需要校验旧密码。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ResultCode setNewPassword(int userLogType, String token, String oldPassword, String newPassword)
            throws JsonProcessingException {
        // 用户未登录
        User user = accessByToken(token);
        if (user == null) {
            return ResultCode.USER_ACCESS_ERROR;
        }

        // 旧密码错误
        if (oldPassword != null && !passwordEquals(user, oldPassword)) {
            return ResultCode.USER_PASSWORD_ERROR;
        }

        // 新旧密码相同
        if (passwordEquals(user, newPassword)) {
            return ResultCode.PARAM_MODIFY_SAME;
        }

        // 在修改密码时，用户不是正常状态
        if (userLogType == UserLogType.MODIFY_PASSWORD && user.getStatus() != UserStatus.NORMAL) {
            return ResultCode.USER_STATUS_INVALID;
        }

        // 更新数据库中的用户密码
        User updated = new User();
        updated.setId(user.getId());
        setPassword(updated, newPassword);
        userDao.updateByIdSelective(updated);

        // 写入用户更新密码日志
        userLogDao.insert(new UserLog(user.getId(), userLogType, objectMapper.writeValueAsString(
                new UserUpdateLog(user.getPassword(), updated.getPassword()))));

        // 清除缓存
        redisUserDao.deleteUserById(user.getId());

        // 最后在修改 map 中的用户密码
        user.setSalt(updated.getSalt());
        user.setPassword(updated.getPassword());

        return ResultCode.SUCCESS;
    }

    /**
     * 通过用户登录 token，为用户设置新的密码。
     */
    public ResultCode setNewPassword(int userLogType, String token, String newPassword) throws JsonProcessingException {
        return setNewPassword(userLogType, token, null, newPassword);
    }

    /**
     * 验证用户的密码，以便确认用户状态。
     */
    public ResultCode verifyPassword(String token, String password) {
        // 用户未登录
        User user = accessByToken(token);
        if (user == null) {
            return ResultCode.USER_ACCESS_ERROR;
        }

        // 密码错误
        if (!passwordEquals(user, password)) {
            return ResultCode.USER_PASSWORD_ERROR;
        }

        // 用户不是正常状态
        if (user.getStatus() != UserStatus.NORMAL) {
            return ResultCode.USER_STATUS_INVALID;
        }

        return ResultCode.SUCCESS;
    }

    /**
     * 修改用户名称。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ResultCode modifyName(UserController.ModifyNameParams params) throws JsonProcessingException {
        ResultCode result = verifyPassword(params.token, params.password);
        if (result != ResultCode.SUCCESS) {
            return result;
        }

        User user = accessByToken(params.token);

        // 如果新旧名称相同
        if (params.newName.equals(user.getName())) {
            return ResultCode.PARAM_MODIFY_SAME;
        }

        // 更新数据库中的用户名
        User update = new User();
        update.setId(user.getId());
        update.setName(params.newName);
        userDao.updateByIdSelective(update);

        // 写入用户更新名称日志
        userLogDao.insert(new UserLog(user.getId(), UserLogType.MODIFY_NAME, objectMapper.writeValueAsString(
                new UserUpdateLog(user.getName(), params.newName))));

        // 删除缓存
        redisUserDao.deleteUserById(user.getId());

        // 最后在修改 map 中的用户名称
        user.setName(params.newName);

        return ResultCode.SUCCESS;
    }

    /**
     * 发送注销账号邮件。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ResultCode sendCancelAccountEmail(String token, String password)
            throws MessagingException, GeneralSecurityException, UnsupportedEncodingException {
        ResultCode result = verifyPassword(token, password);
        if (result != ResultCode.SUCCESS) {
            return result;
        }

        User user = accessByToken(token);

        // 发送用户注销模板邮件
        String encryptedParams = URLUtil.encryptParams(cancelAesKey, user.getId() + " " + token + " "
                + TimeUtil.changeDateTime(cancelExpireTime, TimeUnit.HOURS).getTime());
        Context context = new Context();
        context.setVariable("username", user.getName());
        context.setVariable("cancelUrl", cancelUrl + encryptedParams);
        mailService.sendTemplateEmail(EmailTemplate.USER_ACCOUNT_CANCEL_NAME, mailSender,
                user.getEmail(), EmailTemplate.USER_ACCOUNT_CANCEL_SUBJECT, context);

        return ResultCode.SUCCESS;
    }

    /**
     * 注销账号。
     */
    public ResultCode cancelAccount(String encryptedParams)
            throws GeneralSecurityException, UnsupportedEncodingException, JsonProcessingException {
        String[] params = URLUtil.decryptParams(cancelAesKey, encryptedParams).split(" ");
        if (params.length != 3) {
            return ResultCode.PARAM_IS_INVALID;
        }

        int userId = Integer.parseInt(params[0]);
        String token = params[1];
        long expireAt = Long.parseLong(params[2]);

        User user = redisUserDao.getUserById(userId);
        if (user == null) {
            user = userDao.selectById(userId);
        }

        if (user == null) {
            return ResultCode.USER_NON_EXISTS;
        }
        if (user.getStatus() != UserStatus.NORMAL) {
            return ResultCode.USER_STATUS_INVALID;
        }
        if (expireAt < System.currentTimeMillis()) {
            return ResultCode.PARAMS_EXPIRED;
        }

        // 删除用户的所有标识
        userIdentityDao.deleteByUserId(userId);

        // 更新用户的状态
        User update = new User();
        update.setId(userId);
        update.setStatus(UserStatus.CANCELLED);
        userDao.updateByIdSelective(update);

        // 删除 redis 中已注销账号的数据
        redisUserDao.deleteUserById(userId);

        // 最后在删除用户 token
        quitByToken(token, UserLogoutType.CANCEL);

        return ResultCode.SUCCESS;
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
