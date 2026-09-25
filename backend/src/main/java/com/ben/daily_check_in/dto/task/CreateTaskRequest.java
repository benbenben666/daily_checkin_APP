package com.ben.daily_check_in.dto.task;

import lombok.Data;

import java.util.List;

/**
 * 发布任务请求。
 */
@Data
public class CreateTaskRequest {

    /** GLOBAL / ASSIGNED */
    private String taskType;
    private String title;
    /** 详情文字，可空 */
    private String description;
    /** 限时时长（分钟），5 ~ 43200 */
    private Integer timeLimitMinutes;
    /** PUBLIC / RESTRICTED，默认 PUBLIC */
    private String visibility;
    /** 是否允许超时补交，默认 1 */
    private Integer allowLateSubmit;
    /** 详情配图的 OSS object key 列表，可空 */
    private List<String> detailImages;
    /** ASSIGNED 时必填：被指派用户 ID 列表 */
    private List<Long> assigneeIds;
    /** RESTRICTED 时必填：可见人用户 ID 列表 */
    private List<Long> viewerIds;
}
