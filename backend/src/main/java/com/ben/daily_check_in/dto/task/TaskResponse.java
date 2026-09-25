package com.ben.daily_check_in.dto.task;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务详情 / 列表项。
 */
@Data
public class TaskResponse {

    private Long id;
    private Long companyId;
    private Long publisherId;
    private String publisherNickname;
    /** GLOBAL / ASSIGNED */
    private String taskType;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime deadlineAt;
    private LocalDateTime lateDeadlineAt;
    /** PENDING / EXPIRED / COMPLETED / CANCELLED */
    private String status;
    private Long completedBy;
    private String completedByNickname;
    private LocalDateTime completedAt;
    private String submitContent;
    private Long cancelledBy;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    /** PUBLIC / RESTRICTED */
    private String visibility;
    private Integer allowLateSubmit;
    /** 0-按时 1-超时完成 */
    private Integer isLate;
    /** 详情配图 */
    private List<ImageItem> detailImages;
    /** 完成提交图 */
    private List<ImageItem> submitImages;
    /** 被指派人（ASSIGNED 时） */
    private List<AssigneeItem> assignees;
    /** 可见人白名单（RESTRICTED 时） */
    private List<ViewerItem> viewers;

    @Data
    public static class ImageItem {
        private Long id;
        private String objectKey;
        private String fileName;
        private Integer fileSize;
        private Integer sortOrder;
    }

    @Data
    public static class AssigneeItem {
        private Long userId;
        private String nickname;
        private String phone;
    }

    @Data
    public static class ViewerItem {
        private Long userId;
        private String nickname;
    }
}
