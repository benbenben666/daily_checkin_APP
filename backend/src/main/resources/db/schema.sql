-- ============================================================
--  daily_checkin_APP  数据库建表脚本
--  MySQL 8.0.16+（CHECK 约束需要 8.0.16；生成列需要 5.7+）
--  InnoDB ／ utf8mb4 ／ utf8mb4_0900_ai_ci
--
--  ⚠️ 开头会 DROP 全部表并清空数据，上生产前请删掉「清理」那一段
--  ⚠️ 建表顺序有外键依赖，请勿调整
--
--  执行方式（二选一）：
--    1. IDEA 里右键本文件 → Run
--    2. 命令行：mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `daily_check_in`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `daily_check_in`;

-- ------------------------------------------------------------
-- 清理（按外键依赖倒序）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `task_viewer`;
DROP TABLE IF EXISTS `task_image`;
DROP TABLE IF EXISTS `task_assignee`;
DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `company_blacklist`;
DROP TABLE IF EXISTS `join_application`;
DROP TABLE IF EXISTS `user_company`;
DROP TABLE IF EXISTS `company`;
DROP TABLE IF EXISTS `user`;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '用户ID',
    `phone`       VARCHAR(20)  NOT NULL                             COMMENT '手机号，登录账号，全局唯一',
    `password`    VARCHAR(255) NOT NULL                             COMMENT '密码哈希（BCrypt/Argon2），绝不存明文',
    `nickname`    VARCHAR(50)           DEFAULT NULL                COMMENT '昵称',
    `avatar_key`  VARCHAR(500)          DEFAULT NULL                COMMENT '头像 OSS object key（可选）',
    `system_role` VARCHAR(20)  NOT NULL DEFAULT 'USER'              COMMENT '系统角色：USER-普通用户 ADMIN-系统管理员（开发者权限，只能手工改库）',
    `status`      TINYINT      NOT NULL DEFAULT 1                   COMMENT '1-正常 0-禁用（"删除用户"=置 0，不物理删除）',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '注册时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_system_role` (`system_role`),
    CONSTRAINT `ck_user_system_role` CHECK (`system_role` IN ('USER', 'ADMIN')),
    CONSTRAINT `ck_user_status`      CHECK (`status` IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 公司表
-- ------------------------------------------------------------
CREATE TABLE `company` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '公司ID，全局唯一',
    `name`         VARCHAR(100) NOT NULL                             COMMENT '公司名称，允许重名',
    `invite_code`  CHAR(8)      NOT NULL                             COMMENT '公司邀请码，全局唯一，加入公司的唯一凭据',
    `status`       TINYINT      NOT NULL DEFAULT 1                   COMMENT '1-正常 0-已解散',
    `dissolved_by` BIGINT                DEFAULT NULL                COMMENT '解散操作人用户ID（创始人本人或系统管理员）',
    `dissolved_at` DATETIME              DEFAULT NULL                COMMENT '解散时间',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invite_code` (`invite_code`),
    KEY `idx_name`         (`name`),
    KEY `idx_dissolved_by` (`dissolved_by`),
    CONSTRAINT `fk_company_dissolver` FOREIGN KEY (`dissolved_by`) REFERENCES `user` (`id`),
    CONSTRAINT `ck_company_status` CHECK (`status` IN (0, 1))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公司表';

-- ------------------------------------------------------------
-- 3. 用户-公司关系表（成员关系）
-- ------------------------------------------------------------
CREATE TABLE `user_company` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT              COMMENT '主键',
    `user_id`      BIGINT      NOT NULL                             COMMENT '用户ID',
    `company_id`   BIGINT      NOT NULL                             COMMENT '公司ID',
    `company_role` VARCHAR(20) NOT NULL                             COMMENT '公司内角色：FOUNDER-创始人 MANAGER-管理者 EMPLOYEE-员工',
    `status`       TINYINT     NOT NULL DEFAULT 1                   COMMENT '1-在职 0-已离职',
    `leave_type`   TINYINT              DEFAULT NULL                COMMENT '离职方式：1-主动退出 2-被移除 3-被拉黑；在职时为 NULL',
    `joined_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '加入时间',
    `left_at`      DATETIME             DEFAULT NULL                COMMENT '离职时间，在职时为 NULL',
    `updated_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `founder_only` TINYINT GENERATED ALWAYS AS (IF(`company_role` = 'FOUNDER', 1, NULL)) STORED
                   COMMENT '内部列请勿写入：非创始人恒为 NULL，用于约束每家公司最多一个创始人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_company`         (`user_id`, `company_id`),
    UNIQUE KEY `uk_one_founder`          (`company_id`, `founder_only`),
    KEY        `idx_company_status_role` (`company_id`, `status`, `company_role`),
    KEY        `idx_user_status`         (`user_id`, `status`),
    CONSTRAINT `fk_uc_user`    FOREIGN KEY (`user_id`)    REFERENCES `user` (`id`),
    CONSTRAINT `fk_uc_company` FOREIGN KEY (`company_id`) REFERENCES `company` (`id`),
    CONSTRAINT `ck_uc_company_role` CHECK (`company_role` IN ('FOUNDER', 'MANAGER', 'EMPLOYEE')),
    CONSTRAINT `ck_uc_status`       CHECK (`status` IN (0, 1)),
    CONSTRAINT `ck_uc_leave_type`   CHECK (`leave_type` IS NULL OR `leave_type` IN (1, 2, 3)),
    CONSTRAINT `ck_uc_leave_consistency` CHECK (
           (`status` = 1 AND `leave_type` IS NULL     AND `left_at` IS NULL)
        OR (`status` = 0 AND `leave_type` IS NOT NULL AND `left_at` IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与公司关系表（成员关系）';

-- ------------------------------------------------------------
-- 4. 公司加入申请表
-- ------------------------------------------------------------
CREATE TABLE `join_application` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT            COMMENT '主键',
    `user_id`       BIGINT       NOT NULL                           COMMENT '申请人用户ID',
    `company_id`    BIGINT       NOT NULL                           COMMENT '目标公司ID',
    `apply_role`    VARCHAR(20)  NOT NULL                           COMMENT '申请身份：MANAGER-管理者 EMPLOYEE-员工（不可申请 FOUNDER）',
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING'         COMMENT 'PENDING-待审批 APPROVED-已通过 REJECTED-已拒绝 CANCELLED-申请人撤回',
    `reviewer_id`   BIGINT                DEFAULT NULL              COMMENT '审批人用户ID，未审批时为 NULL',
    `reviewed_at`   DATETIME              DEFAULT NULL              COMMENT '审批时间，未审批时为 NULL',
    `reject_reason` VARCHAR(200)          DEFAULT NULL              COMMENT '拒绝理由',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间（每日限额的计数依据）',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `pending_only`  TINYINT GENERATED ALWAYS AS (IF(`status` = 'PENDING', 1, NULL)) STORED
                    COMMENT '内部列请勿写入：非待审批恒为 NULL，用于限制同公司同用户只能有一条待审批申请',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_one_pending`        (`user_id`, `company_id`, `pending_only`),
    KEY        `idx_user_company_time` (`user_id`, `company_id`, `created_at`),
    KEY        `idx_company_status`    (`company_id`, `status`),
    KEY        `idx_reviewer_id`       (`reviewer_id`),
    CONSTRAINT `fk_ja_user`     FOREIGN KEY (`user_id`)     REFERENCES `user` (`id`),
    CONSTRAINT `fk_ja_company`  FOREIGN KEY (`company_id`)  REFERENCES `company` (`id`),
    CONSTRAINT `fk_ja_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`),
    CONSTRAINT `ck_ja_apply_role` CHECK (`apply_role` IN ('MANAGER', 'EMPLOYEE')),
    CONSTRAINT `ck_ja_status`     CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公司加入申请表';

-- ------------------------------------------------------------
-- 5. 公司黑名单表（存在记录 = 永久拉黑）
-- ------------------------------------------------------------
CREATE TABLE `company_blacklist` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
    `company_id`  BIGINT       NOT NULL                             COMMENT '公司ID',
    `user_id`     BIGINT       NOT NULL                             COMMENT '被拉黑的用户ID',
    `operator_id` BIGINT       NOT NULL                             COMMENT '执行拉黑的人（创始人/管理者/系统管理员）',
    `reason`      VARCHAR(200)          DEFAULT NULL                COMMENT '拉黑原因',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '拉黑时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_company_user` (`company_id`, `user_id`),
    KEY `idx_user_id`     (`user_id`),
    KEY `idx_operator_id` (`operator_id`),
    CONSTRAINT `fk_bl_company`  FOREIGN KEY (`company_id`)  REFERENCES `company` (`id`),
    CONSTRAINT `fk_bl_user`     FOREIGN KEY (`user_id`)     REFERENCES `user` (`id`),
    CONSTRAINT `fk_bl_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公司黑名单表';

-- ------------------------------------------------------------
-- 6. 任务表
-- ------------------------------------------------------------
CREATE TABLE `task` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '任务ID',
    `company_id`         BIGINT       NOT NULL                             COMMENT '所属公司ID',
    `publisher_id`       BIGINT       NOT NULL                             COMMENT '发布人用户ID（创始人或管理者）',
    `task_type`          VARCHAR(20)  NOT NULL                             COMMENT '任务类型：GLOBAL-全局任务（全公司可抢，先到先得） ASSIGNED-指定任务',
    `title`              VARCHAR(200) NOT NULL                             COMMENT '任务标题',
    `description`        TEXT                  DEFAULT NULL                COMMENT '任务详情（文字），配图见 task_image',
    `time_limit_minutes` INT          NOT NULL                             COMMENT '限时时长（分钟），5 ~ 43200（30天）',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '发布时间',

    -- ↓ 截止时间由「发布时间 + 限时时长」自动推导，不需要也不允许手工写入
    `deadline_at`        DATETIME GENERATED ALWAYS AS (
                             `created_at` + INTERVAL `time_limit_minutes` MINUTE
                         ) STORED COMMENT '完成截止 = 发布时间 + 限时时长',

    -- ↓ 补交窗口长度等于限时时长本身（限时30分钟的任务，超时后仍有30分钟可补交）
    `late_deadline_at`   DATETIME GENERATED ALWAYS AS (
                             `deadline_at` + INTERVAL `time_limit_minutes` MINUTE
                         ) STORED COMMENT '补交截止 = 完成截止 + 限时时长；allow_late_submit=0 时该值无意义',

    `status`             VARCHAR(20)  NOT NULL DEFAULT 'PENDING'           COMMENT 'PENDING-未完成 EXPIRED-已超时(等待补交) COMPLETED-已完成 CANCELLED-已取消',
    `completed_by`       BIGINT                DEFAULT NULL                COMMENT '完成人用户ID（全局任务即抢到的人）',
    `completed_at`       DATETIME              DEFAULT NULL                COMMENT '完成时间（超时补交时为补交时刻）',
    `submit_content`     TEXT                  DEFAULT NULL                COMMENT '完成时提交的文字描述（允许为空）',
    `cancelled_by`       BIGINT                DEFAULT NULL                COMMENT '取消操作人用户ID（创始人/管理者）',
    `cancelled_at`       DATETIME              DEFAULT NULL                COMMENT '取消时间',
    `cancel_reason`      VARCHAR(200)          DEFAULT NULL                COMMENT '取消原因',
    `visibility`         VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC'            COMMENT '可见范围：PUBLIC-全公司可见 RESTRICTED-仅白名单可见（白名单见 task_viewer）',
    `allow_late_submit`  TINYINT      NOT NULL DEFAULT 1                   COMMENT '是否允许超时补交：1-允许（默认） 0-不允许',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- ↓ 是否超时完成：完全由 completed_at 与 deadline_at 推导，请勿手工维护
    `is_late`            TINYINT GENERATED ALWAYS AS (
                             IF(`completed_at` IS NOT NULL AND `completed_at` > `deadline_at`, 1, 0)
                         ) STORED COMMENT '0-按时完成(或未完成) 1-超时完成',

    PRIMARY KEY (`id`),
    KEY `idx_company_type_status` (`company_id`, `task_type`, `status`),
    KEY `idx_company_status`      (`company_id`, `status`),
    KEY `idx_status_deadline`     (`status`, `deadline_at`),
    KEY `idx_publisher_id`        (`publisher_id`),
    KEY `idx_completed_by`        (`completed_by`),
    KEY `idx_cancelled_by`        (`cancelled_by`),
    CONSTRAINT `fk_task_company`   FOREIGN KEY (`company_id`)   REFERENCES `company` (`id`),
    CONSTRAINT `fk_task_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_task_completer` FOREIGN KEY (`completed_by`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_task_canceller` FOREIGN KEY (`cancelled_by`) REFERENCES `user` (`id`),
    CONSTRAINT `ck_task_type`       CHECK (`task_type` IN ('GLOBAL', 'ASSIGNED')),
    CONSTRAINT `ck_task_status`     CHECK (`status` IN ('PENDING', 'EXPIRED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT `ck_task_visibility` CHECK (`visibility` IN ('PUBLIC', 'RESTRICTED')),
    CONSTRAINT `ck_task_late`       CHECK (`allow_late_submit` IN (0, 1)),
    CONSTRAINT `ck_task_time_limit` CHECK (`time_limit_minutes` BETWEEN 5 AND 43200),
    -- 完成态必须有完成人和完成时间；其他状态必须都没有
    CONSTRAINT `ck_task_completion` CHECK (
           (`status` = 'COMPLETED' AND `completed_by` IS NOT NULL AND `completed_at` IS NOT NULL)
        OR (`status` <> 'COMPLETED' AND `completed_by` IS NULL     AND `completed_at` IS NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务表';

-- ------------------------------------------------------------
-- 7. 任务指派关系表（指定型任务专用，支持指派多人）
-- ------------------------------------------------------------
CREATE TABLE `task_assignee` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT              COMMENT '主键',
    `task_id`     BIGINT   NOT NULL                             COMMENT '任务ID',
    `user_id`     BIGINT   NOT NULL                             COMMENT '被指派的用户ID',
    `assigned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '指派时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_user` (`task_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_ta_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`),
    CONSTRAINT `fk_ta_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务指派关系表';

-- ------------------------------------------------------------
-- 8. 任务图片表（阿里云 OSS）
-- ------------------------------------------------------------
CREATE TABLE `task_image` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '主键',
    `task_id`    BIGINT       NOT NULL                             COMMENT '任务ID',
    `image_type` VARCHAR(20)  NOT NULL                             COMMENT 'DETAIL-任务详情配图 SUBMIT-完成提交图',
    `object_key` VARCHAR(500) NOT NULL                             COMMENT 'OSS object key（不含域名，完整 URL 由后端拼接）',
    `file_name`  VARCHAR(255)          DEFAULT NULL                COMMENT '原始文件名',
    `file_size`  INT                   DEFAULT NULL                COMMENT '文件大小（字节）',
    `sort_order` INT          NOT NULL DEFAULT 0                   COMMENT '展示顺序',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_type` (`task_id`, `image_type`, `sort_order`),
    CONSTRAINT `fk_ti_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`),
    CONSTRAINT `ck_ti_type` CHECK (`image_type` IN ('DETAIL', 'SUBMIT'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务图片表';

-- ------------------------------------------------------------
-- 9. 任务可见人白名单（visibility = RESTRICTED 时生效）
-- ------------------------------------------------------------
CREATE TABLE `task_viewer` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT              COMMENT '主键',
    `task_id`    BIGINT   NOT NULL                             COMMENT '任务ID',
    `user_id`    BIGINT   NOT NULL                             COMMENT '可见用户ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '添加时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_user` (`task_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_tv_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`),
    CONSTRAINT `fk_tv_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '任务可见人白名单（visibility=RESTRICTED 时生效；被指派人无需重复写入，查询时自动可见）';

-- ============================================================
--  系统管理员的唯一产生途径：手工改库
--  （注册接口永远只能创建 USER，这是"管理员无法通过注册成为"的落地方式）
-- ============================================================
-- UPDATE `user` SET `system_role` = 'ADMIN' WHERE `phone` = '13800000000';
