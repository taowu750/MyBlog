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
    `user_id`              int primary key,

    `profile_picture_path` varchar(150)  not null comment '头像图片路径',
    `description`          varchar(3000) not null comment '用户自我介绍',
    `sex`                  int           not null default 0 comment '性别：1 男，2 女',
    `age`                  int           not null default 0,
    `birthday`             timestamp              default '1970-01-01 08:00:01',
    `profession`           int           not null default 0,

    `create_time`          timestamp     not null default current_timestamp,
    `modify_time`          timestamp     not null default current_timestamp on update current_timestamp
);

# 用户统计信息
create table if not exists `user_statistic`
(
    user_id        int       not null primary key,

    follower_count int       not null comment '关注了多少用户',
    followed_count int       not null comment '被多少用户关注',

    `create_time`  timestamp not null default current_timestamp,
    `modify_time`  timestamp not null default current_timestamp on update current_timestamp
);

create table if not exists `blog`
(
    `id`               int primary key auto_increment,
    `user_id`          int          not null,

    # 内容信息
    `title`            varchar(50)  not null,
    `html_body`        text         not null,
    `markdown_body`    text         not null,
    `cover_path`       varchar(150) not null comment '封面图片路径',

    # 元数据
    `word_count`       int          not null,
    `reading_count`    int          not null default 0,
    `like_count`       int          not null default 0,
    `dislike_count`    int          not null default 0,
    `collect_count`    int          not null default 0,
    `comment_count`    int          not null default 0,

    # 控制信息
    `status`           int          not null comment '状态：1 已发表，2 审核中，3 被封禁，4 被删除',
    `is_allow_reprint` bool         not null default false comment '是否允许转载',

    `create_time`      timestamp    not null default current_timestamp,
    `modify_time`      timestamp    not null default current_timestamp comment '文章本身（标题、内容、封面、是否允许转载）的修改时间',

    key (user_id)
);

# 标签
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

# 博客、用户等所拥有的标签
create table if not exists `target_tag`
(
    `id`            int primary key auto_increment,
    `tag_id`        int       not null,
    `target_type`   int       not null comment '具有标签的对象类型：1 用户，2 博客，3 专栏，4 收藏夹',
    `target_id`     int       not null,
    `creation_time` timestamp not null default current_timestamp,

    key (`target_id`, `target_type`),
    key (`tag_id`)
);

# 专栏
create table if not exists `special_column`
(
    `id`                   int primary key auto_increment,
    `user_id`              int           not null,

    `title`                varchar(50)   not null,
    `html_description`     varchar(4000) not null,
    `markdown_description` varchar(4000) not null,
    `icon_path`            varchar(150)  not null,
    `blog_count`           int           not null,

    `comment_count`        int           not null,
    `like_count`           int           not null default 0,
    `dislike_count`        int           not null default 0,
    `followed_count`       int           not null,

    `creation_time`        timestamp     not null default current_timestamp,
    `modify_time`          timestamp     not null default current_timestamp comment '专栏本身（标题、描述、图标、博客数量）的修改时间',

    key (user_id)
);

create table if not exists `special_column_blog`
(
    `id`                int primary key auto_increment,
    `special_column_id` int       not null,
    `blog_id`           int       not null,
    `creation_time`     timestamp not null default current_timestamp,

    key (special_column_id),
    key (blog_id)
);

# 评论
create table if not exists `comment`
(
    `id`               int primary key auto_increment,
    `user_id`          int           not null,
    `target_type`      int           not null comment '所评论对象的类型：1 博客，2 专栏，3 收藏夹，4 其他评论',
    `target_id`        int           not null comment '所评论对象 id',

    `html_content`     varchar(3000) not null,
    `markdown_content` varchar(3000) not null,
    `like_count`       int           not null default 0,
    `dislike_count`    int           not null default 0,

    `status`           int           not null comment '评论状态：1 已发表，2 审核中，3 被封禁，4 被删除',
    `order`            int           not null,

    `creation_time`    timestamp     not null default current_timestamp,
    `modify_time`      timestamp     not null default current_timestamp on update current_timestamp,

    key (user_id),
    key (target_id, target_type)
);

# 浏览表，记录用户浏览博客等的行为
create table if not exists `browse`
(
    `id`            int primary key auto_increment,
    `user_id`       int       not null,
    `target_type`   int       not null comment '被浏览对象类型：1 用户，2 博客，3 专栏，4 收藏夹',
    `target_id`     int       not null,
    `interval`      int       not null comment '浏览时间长度，单位秒',

    `creation_time` timestamp not null default current_timestamp,

    key (user_id),
    key (target_id, target_type)
);

# 评价，包括点赞/踩、评分
create table if not exists `evaluation`
(
    `id`            int primary key auto_increment,
    `user_id`       int       not null,
    `target_type`   int       not null comment '被评价对象类型：1 博客，2 专栏，3 评论，4 收藏夹',
    `target_id`     int       not null,
    `score`         int       not null comment '评价值。对于点赞/踩是 1、2；对于评分是 1-10',

    `creation_time` timestamp not null default current_timestamp,

    key (user_id),
    key (target_id, target_type)
);

# 关注表，记录用户关注其他用户、专栏等记录
create table if not exists `follow`
(
    `id`            int primary key auto_increment,

    follower_id     int       not null,
    `followed_type` int       not null comment '被关注对象类型：1 用户，2 专栏，3 收藏夹',
    followed_id     int       not null,

    `create_time`   timestamp not null default current_timestamp,

    key (follower_id),
    key (followed_id, followed_type)
);

# 收藏夹表
create table if not exists `collect_folder`
(
    `id`             int primary key auto_increment,
    `user_id`        int          not null,

    `title`          varchar(50)  not null,
    `description`    varchar(300) not null,
    `collect_count`  int          not null,
    `is_public`      bool                  default true comment '收藏夹是否公开',

    `comment_count`  int          not null,
    `like_count`     int          not null default 0,
    `dislike_count`  int          not null default 0,
    `followed_count` int          not null,

    `creation_time`  timestamp    not null default current_timestamp,
    `modify_time`    timestamp    not null default current_timestamp comment '收藏夹本身（标题、描述、收藏数量、是否公开）的修改时间',

    key (user_id)
);

# 收藏表，记录用户收藏博客等记录
create table if not exists `collect`
(
    `id`                int primary key auto_increment,
    `user_id`           int       not null,
    `target_type`       int       not null comment '被收藏对象类型：1 博客',
    `target_id`         int       not null,
    `collect_folder_id` int       not null,

    `creation_time`     timestamp not null default current_timestamp,

    key (user_id),
    key (target_id, target_type)
);