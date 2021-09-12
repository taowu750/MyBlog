create table if not exists `user`
(
    `id`          int unsigned primary key auto_increment,

    # 身份信息
    `name`        varchar(255) not null,
    `type`        int          not null comment '用户类别：1 普通用户，2 官方用户',
    `password`    varchar(40)  not null,
    `email`       varchar(40)  not null,
    `salt`        varchar(10)  not null,

    # 账号状态信息
    `status`      int          not null comment '状态：1 正常，2 未激活，3 被封禁，4 已注销',
    `limit_time`  timestamp             default null,

    # 时间信息
    `create_time` timestamp    not null default current_timestamp,
    `modify_time` timestamp    not null default current_timestamp on update current_timestamp,

    key (name),
    key (email)
);

# 用户标识表。存放注册用户的激活凭证、登录凭证等
create table if not exists `user_identity`
(
    `id`          int primary key auto_increment,
    `user_id`     int          not null,
    `identity`    varchar(40)  not null,
    `type`        int          not null,
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
    `token`       varchar(100)   not null default '' comment '用来唯一标识某条日志的 token，方便日后查找、修改',
    `description` varchar(10000) not null comment '描述，json 文本',
    `create_time` timestamp      not null default current_timestamp,
    `modify_time` timestamp      not null default current_timestamp on update current_timestamp,

    key (user_id, type),
    key (token)
);

# 用户基本身份信息
create table if not exists `user_basic_info`
(
    `id`                   int unsigned primary key auto_increment,
    `user_id`              int           not null,

    `profile_picture_path` varchar(150)  not null comment '头像图片路径',
    `description`          varchar(3000) not null comment '用户自我介绍',
    `sex`                  int           not null default 0 comment '性别：1 男，2 女',
    `age`                  int           not null default 0,
    `birthday`             timestamp              default '1970-01-01 08:00:01',
    `profession`           int           not null default 0,

    `create_time`          timestamp     not null default current_timestamp,
    `modify_time`          timestamp     not null default current_timestamp on update current_timestamp,

    key (user_id)
);

create table if not exists `user_relation`
(
    `id`          int primary key auto_increment,

    follower_id   int       not null,
    followed_id   int       not null,

    `create_time` timestamp not null default current_timestamp,

    key (follower_id),
    key (followed_id)
);

create table if not exists `blog`
(
    `id`                  int primary key auto_increment,
    `user_id`             int         not null,

    # 内容信息
    `title`               varchar(50) not null,
    `html_body`           text        not null,
    `markdown_body`       text        not null,

    # 元数据
    `word_count`          int         not null,
    `reading_count`       int         not null default 0,
    `like_count`          int         not null default 0,
    `dislike_count`       int         not null default 0,
    `collect_count`       int         not null default 0,
    `comment_count`       int         not null default 0,

    # 控制信息
    `status`              int         not null comment '状态：1 已发表，2 审核中，3 审核失败，4 被封禁，5 被删除',
    `is_allow_reprint`    bool        not null default false comment '是否允许转载',

    `create_time`         timestamp   not null default current_timestamp,
    `modify_time`         timestamp   not null default current_timestamp on update current_timestamp,
    `content_modify_time` timestamp   not null default current_timestamp comment '文章本身（标题、内容）的修改时间',

    key (user_id)
);

# 博客相关：标签、专栏、评论
create table if not exists `tag`
(
    `id`          int primary key auto_increment,
    `text`        varchar(20) not null,
    `user_id`     int         not null,

    `create_time` timestamp   not null default current_timestamp,
    `modify_time` timestamp   not null default current_timestamp on update current_timestamp,

    key (text),
    key (user_id)
);

# 博客所拥有的标签
create table if not exists `blog_tag`
(
    `id`            int primary key auto_increment,
    `blog_id`       int       not null,
    `tag_id`        int       not null,
    `creation_time` timestamp not null default current_timestamp,

    key (`blog_id`),
    key (`tag_id`)
);

# 专栏
create table if not exists `special_column`
(
    `id`           int primary key auto_increment,
    `user_id`      int          not null,

    `title`        varchar(50)  not null,
    `description`  varchar(300) not null,
    `icon_path`    varchar(150) not null,

    `blog_count`   int          not null,
    `follow_count` int          not null,

    key (user_id)
)