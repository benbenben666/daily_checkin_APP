package com.ben.daily_check_in.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 签到记录实体，对应数据库里的签到记录表
 * 字段先按最常见的签到场景放着，等你定了表结构再改
 */
@Data
public class CheckInRecord {

    /** 主键 */
    private Long id;

    /** 签到的用户 ID */
    private Long userId;

    /** 签到日期 */
    private LocalDate checkInDate;

    /** 创建时间 */
    private LocalDateTime createTime;
}