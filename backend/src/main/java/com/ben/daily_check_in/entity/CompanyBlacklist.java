package com.ben.daily_check_in.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公司黑名单，对应表 company_blacklist。有记录 = 永久拉黑。
 */
@Data
public class CompanyBlacklist {

    private Long id;
    private Long companyId;
    private Long userId;
    private Long operatorId;
    private String reason;
    private LocalDateTime createdAt;
}
