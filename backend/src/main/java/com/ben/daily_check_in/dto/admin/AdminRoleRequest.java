package com.ben.daily_check_in.dto.admin;

import lombok.Data;

/**
 * 设置/取消管理员请求。
 */
@Data
public class AdminRoleRequest {

    /** 目标角色：USER / ADMIN */
    private String systemRole;
}
