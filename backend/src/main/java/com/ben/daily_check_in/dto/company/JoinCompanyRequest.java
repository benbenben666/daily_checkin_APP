package com.ben.daily_check_in.dto.company;

import lombok.Data;

/**
 * 申请加入公司请求。
 */
@Data
public class JoinCompanyRequest {

    /** 邀请码，加入公司的唯一凭据 */
    private String inviteCode;
    /** 申请身份：MANAGER / EMPLOYEE（不可申请 FOUNDER） */
    private String applyRole;
}
