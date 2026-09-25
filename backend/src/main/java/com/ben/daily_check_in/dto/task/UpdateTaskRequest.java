package com.ben.daily_check_in.dto.task;

import lombok.Data;

import java.util.List;

/**
 * 编辑任务请求。所有字段都可改（含任务类型与可见范围）。
 * 传 null 的字段不修改。
 */
@Data
public class UpdateTaskRequest {

    private String taskType;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private String visibility;
    private Integer allowLateSubmit;
    /** 详情配图：传则整体替换 */
    private List<String> detailImages;
    /** ASSIGNED 时的被指派人：传则整体替换 */
    private List<Long> assigneeIds;
    /** RESTRICTED 时的可见人：传则整体替换 */
    private List<Long> viewerIds;
}
