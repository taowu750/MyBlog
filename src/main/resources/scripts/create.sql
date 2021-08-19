create table if not exists `user`
(
    `id`          int unsigned primary key auto_increment,

    # 个人信息
    `name`        varchar(255)  not null,
    `note`        varchar(3000) not null,
    `age`         tinyint       not null default 0,
    `sex`         tinyint       not null default 0,

    # 身份信息
    `password`    varchar(40)   not null,
    `email`       varchar(40)   not null,
    `salt`        varchar(10)   not null,

    # 账号状态信息
    `state`       int           not null default 1,
    `state_note`  varchar(1000) not null default '',
    `limit_time`  timestamp              default null,

    # 时间信息
    `create_time` timestamp     not null default current_timestamp,
    `modify_time` timestamp     not null default current_timestamp on update current_timestamp,

    key (name),
    key (email)
);

# 用户标识表。存放注入用户的激活凭证、登录凭证等
create table if not exists `user_identity`
(
    `id`          int primary key auto_increment,
    `user_id`     int          not null,
    `identity`    varchar(40)  not null,
    `type`        tinyint      not null,
    `source`      varchar(100) not null default '',
    `expire`      timestamp    not null,

    `create_time` timestamp    not null default current_timestamp,

    key (user_id, identity)
);

# 用户行为状态日志表，记录诸如登录、忘记密码、修改姓名等操作
create table if not exists `user_log`
(
    `id`          int primary key auto_increment,
    `user_id`     int            not null,
    `type`        int            not null,
    # 用来唯一标识某条日志的 token，方便日后查找、修改
    `token`       varchar(100)   not null default '',
    # 描述，json 文本
    `description` varchar(10000) not null,
    `create_time` timestamp      not null default current_timestamp,
    `modify_time` timestamp      not null default current_timestamp on update current_timestamp,

    key (user_id, type),
    key (token)
);