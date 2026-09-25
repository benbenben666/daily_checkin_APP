package com.ben.daily_check_in.service;

import com.ben.daily_check_in.common.BizException;
import com.ben.daily_check_in.dto.task.*;
import com.ben.daily_check_in.entity.Company;
import com.ben.daily_check_in.entity.Task;
import com.ben.daily_check_in.entity.TaskImage;
import com.ben.daily_check_in.mapper.*;
import com.ben.daily_check_in.security.LoginUser;
import com.ben.daily_check_in.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务：发布 / 编辑 / 列表 / 详情 / 完成(抢单) / 取消。
 * 抢单原子性见开发文档 6.7 / 7.4 —— 条件 UPDATE + 行锁，不先查后写。
 */
@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskAssigneeMapper assigneeMapper;
    private final TaskImageMapper imageMapper;
    private final TaskViewerMapper viewerMapper;
    private final CompanyMapper companyMapper;
    private final UserCompanyMapper userCompanyMapper;
    private final UserMapper userMapper;

    public TaskService(TaskMapper taskMapper, TaskAssigneeMapper assigneeMapper,
                       TaskImageMapper imageMapper, TaskViewerMapper viewerMapper,
                       CompanyMapper companyMapper, UserCompanyMapper userCompanyMapper,
                       UserMapper userMapper) {
        this.taskMapper = taskMapper;
        this.assigneeMapper = assigneeMapper;
        this.imageMapper = imageMapper;
        this.viewerMapper = viewerMapper;
        this.companyMapper = companyMapper;
        this.userCompanyMapper = userCompanyMapper;
        this.userMapper = userMapper;
    }

    /** T-1 创始人/管理者可发布任务 */
    @Transactional
    public Long create(Long companyId, CreateTaskRequest req) {
        LoginUser me = UserContext.require();
        requireManagerOrFounder(companyId);
        validateTaskFields(req.getTaskType(), req.getTitle(), req.getTimeLimitMinutes(),
                req.getVisibility(), req.getAssigneeIds(), req.getViewerIds());

        Task task = new Task();
        task.setCompanyId(companyId);
        task.setPublisherId(me.userId());
        task.setTaskType(req.getTaskType());
        task.setTitle(req.getTitle().trim());
        task.setDescription(req.getDescription());
        task.setTimeLimitMinutes(req.getTimeLimitMinutes());
        task.setVisibility(req.getVisibility() == null ? "PUBLIC" : req.getVisibility());
        task.setAllowLateSubmit(req.getAllowLateSubmit() == null ? 1 : req.getAllowLateSubmit());
        taskMapper.insert(task);

        saveAssignees(task.getId(), task.getTaskType(), req.getAssigneeIds());
        saveViewers(task.getId(), task.getVisibility(), req.getViewerIds());
        saveImages(task.getId(), "DETAIL", req.getDetailImages());
        return task.getId();
    }

    /** T-12 发布者和有管理权限的人可改，所有字段都可改 */
    @Transactional
    public void update(Long taskId, UpdateTaskRequest req) {
        Long userId = UserContext.requireUserId();
        Task task = requireTask(taskId);
        if (!task.getStatus().equals("PENDING") && !task.getStatus().equals("EXPIRED")) {
            throw BizException.conflict("已完成或已取消的任务不能编辑");
        }
        // 权限：发布者本人 或 该公司创始人/管理者/系统管理员
        LoginUser me = UserContext.require();
        boolean isPublisher = task.getPublisherId().equals(userId);
        boolean isManager = me.isAdmin() || isManagerOrFounder(task.getCompanyId(), userId);
        if (!isPublisher && !isManager) {
            throw BizException.forbidden("只有发布者或创始人/管理者可以编辑任务");
        }

        validateTaskFields(req.getTaskType(), req.getTitle(), req.getTimeLimitMinutes(),
                req.getVisibility(), req.getAssigneeIds(), req.getViewerIds());

        Task patch = new Task();
        patch.setId(taskId);
        patch.setTaskType(req.getTaskType());
        patch.setTitle(req.getTitle() == null ? null : req.getTitle().trim());
        patch.setDescription(req.getDescription());
        patch.setTimeLimitMinutes(req.getTimeLimitMinutes());
        patch.setVisibility(req.getVisibility());
        patch.setAllowLateSubmit(req.getAllowLateSubmit());
        taskMapper.update(patch);

        // 编辑后以最新字段为准处理关联数据
        String finalType = req.getTaskType() != null ? req.getTaskType() : task.getTaskType();
        String finalVisibility = req.getVisibility() != null ? req.getVisibility() : task.getVisibility();
        if (req.getAssigneeIds() != null) {
            assigneeMapper.deleteByTask(taskId);
            saveAssignees(taskId, finalType, req.getAssigneeIds());
        } else if ("GLOBAL".equals(finalType)) {
            // 改成全局任务时清空历史指派
            assigneeMapper.deleteByTask(taskId);
        }
        if (req.getViewerIds() != null) {
            viewerMapper.deleteByTask(taskId);
            saveViewers(taskId, finalVisibility, req.getViewerIds());
        } else if ("PUBLIC".equals(finalVisibility)) {
            // 改成公开时清空白名单
            viewerMapper.deleteByTask(taskId);
        }
        if (req.getDetailImages() != null) {
            imageMapper.deleteByTaskAndType(taskId, "DETAIL");
            saveImages(taskId, "DETAIL", req.getDetailImages());
        }
    }

    /** 任务列表：按角色过滤可见范围（见开发文档 3.4 / 7.11） */
    public List<TaskResponse> list(Long companyId, String view) {
        Long userId = UserContext.requireUserId();
        requireCompany(companyId);
        requireMember(companyId);

        LoginUser me = UserContext.require();
        String role = userCompanyMapper.selectActiveRole(userId, companyId);
        boolean isManager = me.isAdmin() || "FOUNDER".equals(role) || "MANAGER".equals(role);

        List<Task> tasks;
        if (isManager) {
            // 创始人/管理者/系统管理员：本公司全部任务
            tasks = taskMapper.selectAllByCompany(companyId);
        } else if ("history".equals(view)) {
            tasks = taskMapper.selectHistoryList(companyId, userId);
        } else {
            tasks = taskMapper.selectGrabList(companyId, userId);
        }
        List<TaskResponse> result = new ArrayList<>();
        for (Task t : tasks) {
            result.add(toResponse(t, false));
        }
        return result;
    }

    public TaskResponse detail(Long taskId) {
        Long userId = UserContext.requireUserId();
        Task task = requireTask(taskId);
        requireMember(task.getCompanyId());
        // 单条任务可见性校验
        if (!taskMapper.canView(taskId, userId)) {
            throw BizException.forbidden("无权查看该任务");
        }
        return toResponse(task, true);
    }

    /** T-4/T-7/T-8 完成/超时补交：条件 UPDATE 保证抢单原子性 */
    @Transactional
    public void complete(Long taskId, CompleteTaskRequest req) {
        Long userId = UserContext.requireUserId();
        Task task = requireTask(taskId);
        requireMember(task.getCompanyId());

        int rows = taskMapper.complete(taskId, userId,
                req == null ? null : req.getSubmitContent());
        if (rows == 0) {
            throw BizException.conflict("提交失败：任务已被他人完成/已取消，或已超出补交时限，或无执行权限");
        }
        if (req != null && req.getSubmitImages() != null) {
            saveImages(taskId, "SUBMIT", req.getSubmitImages());
        }
    }

    /** T-9 本公司创始人和管理者都可以取消 */
    @Transactional
    public void cancel(Long taskId, String cancelReason) {
        Long userId = UserContext.requireUserId();
        Task task = requireTask(taskId);
        requireManagerOrFounder(task.getCompanyId());

        int rows = taskMapper.cancel(taskId, userId, cancelReason);
        if (rows == 0) {
            throw BizException.conflict("任务已完成或已取消，无法取消");
        }
    }

    // ---------- 内部工具 ----------

    private void validateTaskFields(String taskType, String title, Integer timeLimitMinutes,
                                    String visibility, List<Long> assigneeIds, List<Long> viewerIds) {
        if (taskType != null && !"GLOBAL".equals(taskType) && !"ASSIGNED".equals(taskType)) {
            throw BizException.badRequest("任务类型只能是 GLOBAL 或 ASSIGNED");
        }
        if (title != null && title.isBlank()) {
            throw BizException.badRequest("任务标题不能为空");
        }
        // T-3 限时 5 分钟 ~ 30 天
        if (timeLimitMinutes != null && (timeLimitMinutes < 5 || timeLimitMinutes > 43200)) {
            throw BizException.badRequest("限时时长必须在 5 分钟 ~ 30 天之间");
        }
        if (visibility != null && !"PUBLIC".equals(visibility) && !"RESTRICTED".equals(visibility)) {
            throw BizException.badRequest("可见范围只能是 PUBLIC 或 RESTRICTED");
        }
        if ("ASSIGNED".equals(taskType) && (assigneeIds == null || assigneeIds.isEmpty())) {
            throw BizException.badRequest("指定任务必须选择被指派人");
        }
        if ("RESTRICTED".equals(visibility) && (viewerIds == null || viewerIds.isEmpty())) {
            throw BizException.badRequest("指定人可见时必须选择可见人");
        }
    }

    private void saveAssignees(Long taskId, String taskType, List<Long> assigneeIds) {
        if (!"ASSIGNED".equals(taskType) || assigneeIds == null) {
            return;
        }
        for (Long uid : assigneeIds) {
            assigneeMapper.insert(taskId, uid);
        }
    }

    private void saveViewers(Long taskId, String visibility, List<Long> viewerIds) {
        if (!"RESTRICTED".equals(visibility) || viewerIds == null) {
            return;
        }
        for (Long uid : viewerIds) {
            viewerMapper.insert(taskId, uid);
        }
    }

    private void saveImages(Long taskId, String imageType, List<String> objectKeys) {
        if (objectKeys == null) {
            return;
        }
        for (int i = 0; i < objectKeys.size(); i++) {
            imageMapper.insert(taskId, imageType, objectKeys.get(i), i);
        }
    }

    private TaskResponse toResponse(Task t, boolean withDetails) {
        TaskResponse r = new TaskResponse();
        r.setId(t.getId());
        r.setCompanyId(t.getCompanyId());
        r.setPublisherId(t.getPublisherId());
        r.setPublisherNickname(nicknameOf(t.getPublisherId()));
        r.setTaskType(t.getTaskType());
        r.setTitle(t.getTitle());
        r.setDescription(t.getDescription());
        r.setTimeLimitMinutes(t.getTimeLimitMinutes());
        r.setCreatedAt(t.getCreatedAt());
        r.setDeadlineAt(t.getDeadlineAt());
        r.setLateDeadlineAt(t.getLateDeadlineAt());
        r.setStatus(t.getStatus());
        r.setCompletedBy(t.getCompletedBy());
        r.setCompletedByNickname(nicknameOf(t.getCompletedBy()));
        r.setCompletedAt(t.getCompletedAt());
        r.setSubmitContent(t.getSubmitContent());
        r.setCancelledBy(t.getCancelledBy());
        r.setCancelledAt(t.getCancelledAt());
        r.setCancelReason(t.getCancelReason());
        r.setVisibility(t.getVisibility());
        r.setAllowLateSubmit(t.getAllowLateSubmit());
        r.setIsLate(t.getIsLate());
        if (withDetails) {
            r.setDetailImages(toImageItems(imageMapper.selectByTaskAndType(t.getId(), "DETAIL")));
            r.setSubmitImages(toImageItems(imageMapper.selectByTaskAndType(t.getId(), "SUBMIT")));
            if ("ASSIGNED".equals(t.getTaskType())) {
                r.setAssignees(assigneeMapper.selectWithUser(t.getId()));
            }
            if ("RESTRICTED".equals(t.getVisibility())) {
                r.setViewers(viewerMapper.selectWithUser(t.getId()));
            }
        }
        return r;
    }

    private List<TaskResponse.ImageItem> toImageItems(List<TaskImage> images) {
        List<TaskResponse.ImageItem> items = new ArrayList<>();
        for (TaskImage img : images) {
            TaskResponse.ImageItem item = new TaskResponse.ImageItem();
            item.setId(img.getId());
            item.setObjectKey(img.getObjectKey());
            item.setFileName(img.getFileName());
            item.setFileSize(img.getFileSize());
            item.setSortOrder(img.getSortOrder());
            items.add(item);
        }
        return items;
    }

    /** 查用户昵称；用户不存在或 id 为空时返回 null（前端回退显示 ID） */
    private String nicknameOf(Long userId) {
        if (userId == null) {
            return null;
        }
        com.ben.daily_check_in.entity.User u = userMapper.selectById(userId);
        return u == null ? null : u.getNickname();
    }

    private Task requireTask(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BizException.notFound("任务不存在");
        }
        return task;
    }

    private void requireCompany(Long companyId) {
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw BizException.notFound("公司不存在");
        }
        if (company.getStatus() == 0) {
            throw BizException.conflict("公司已解散");
        }
    }

    private void requireMember(Long companyId) {
        LoginUser me = UserContext.require();
        if (me.isAdmin()) {
            return;
        }
        if (userCompanyMapper.selectActive(me.userId(), companyId) == null) {
            throw BizException.forbidden("你不是该公司成员");
        }
    }

    private boolean isManagerOrFounder(Long companyId, Long userId) {
        String role = userCompanyMapper.selectActiveRole(userId, companyId);
        return "FOUNDER".equals(role) || "MANAGER".equals(role);
    }

    private void requireManagerOrFounder(Long companyId) {
        requireCompany(companyId);
        LoginUser me = UserContext.require();
        if (me.isAdmin()) {
            return;
        }
        if (!isManagerOrFounder(companyId, me.userId())) {
            throw BizException.forbidden("需要创始人或管理者权限");
        }
    }
}
