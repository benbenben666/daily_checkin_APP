package com.ben.daily_check_in.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户，对应表 user。
 */
@Data
public class User {

    private Long id;
    /** 手机号，登录账号，全局唯一 */
    private String phone;
    /** 密码哈希（BCrypt） */
    private String password;
    private String nickname;
    /** 头像 OSS object key */
    private String avatarKey;
    /** USER / ADMIN */
    private String systemRole;
    /** 1-正常 0-禁用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
