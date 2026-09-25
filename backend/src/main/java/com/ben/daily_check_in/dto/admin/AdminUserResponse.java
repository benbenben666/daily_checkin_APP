package com.ben.daily_check_in.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员视角的用户列表项。
 */
@Data
public class AdminUserResponse {

    private Long id;
    private String phone;
    private String nickname;
    private String avatarKey;
    /** USER / ADMIN */
    private String systemRole;
    /** 1-正常 0-禁用 */
    private Integer status;
    private LocalDateTime createdAt;
}
