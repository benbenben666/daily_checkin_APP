package com.ben.daily_check_in.dto.company;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成员列表项。
 */
@Data
public class MemberResponse {

    private Long userId;
    private String nickname;
    private String phone;
    private String avatarKey;
    /** FOUNDER / MANAGER / EMPLOYEE */
    private String companyRole;
    private LocalDateTime joinedAt;
}
