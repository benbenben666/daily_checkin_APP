package com.ben.daily_check_in.service;

import com.ben.daily_check_in.common.BizException;
import com.ben.daily_check_in.dto.task.*;
import com.ben.daily_check_in.entity.Company;
import com.ben.daily_check_in.entity.Task;
import com.ben.daily_check_in.entity.TaskImage;
import com.ben.daily_check_in.entity.User;
import com.ben.daily_check_in.mapper.*;
import com.ben.daily_check_in.security.LoginUser;
import com.ben.daily_check_in.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务：发布 / 编辑 / 列表 / 详情 / 完成(抢单) / 取消。
 * 抢单原子性见开发文档 6.7 / 7.4 —— 条件 UPDATE + 行锁，不先查后写。
 */
@Service
public class TaskService {

    /** 单次请求允许携带的指派 / 可见人数量上限（防超大请求把事务撑爆） */
    private static final int MAX_RELATION_IDS = 200;
    /** 单张任务允许的配图数量上限 */
    private static final int MAX_IMAGES = 20;
    private static final int TITLE_MAX = 200;
    private static final int REASON_MAX = 200;
    private static final int OBJECT_KEY_MAX = 500;

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
        if (req == null) {
            throw BizException.badRequest("请求内容不能为空");
        }
        // 必填字段先判空，避免后面 title.trim() 抛 NPE 或直接撞数据库 NOT NULL
        if (req.getTaskType() == null) {
            throw BizException.badRequest("任务类型不能为空");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw BizException.badRequest("任务标题不能为空");
        }
        if (req.getTimeLimitMinutes() == null) {
            throw BizException.badRequest("限时时长不能为空");
        }
        validateTaskFields(req.getTaskType(), req.getTitle(), req.getTimeLimitMinutes(),
                req.getVisibility(), req.getAssigneeIds(), req.getViewerIds());
        checkAllowLateSubmit(req.getAllowLateSubmit());
        // 指派 / 可见人必须是本公司当前在职成员，否则会写入跨公司脏关联
        requireActiveMembers(companyId, req.getAssigneeIds(), "被指派人");
        requireActiveMembers(companyId, req.getViewerIds(), "可见人");

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

    /** T-12 发布者和有管理权限的人可改，所有字段都可改（已完成/已取消的任务除外） */
    @Transactional
    public void update(Long taskId, UpdateTaskRequest req) {
        if (req == null) {
            throw BizException.badRequest("请求内容不能为空");
        }
        Long userId = UserContext.requireUserId();
        Task task = requireTask(taskId);
        requireCompany(task.getCompanyId());
        if (!"PENDING".equals(task.getStatus()) && !"EXPIRED".equals(task.getStatus())) {
            throw BizException.conflict("已完成或已取消的任务不能编辑");
        }

        LoginUser me = UserContext.require();
        boolean isPublisher = task.getPublisherId().equals(userId);
        boolean isManager = me.isAdmin() || isManagerOrFounder(task.getCompanyId(), userId);
        // 发布者这条路径同样要求「仍是在职成员」：
        // 被移除/拉黑后，发布者不得再改自己当年发的任务（否则能靠改限时延长截止时间）
        if (isPublisher) {
            requireMember(task.getCompanyId());
        }
        if (!isPublisher && !isManager) {
            throw BizException.forbidden("只有发布者或创始人/管理者可以编辑任务");
        }

        validateTaskFields(req.getTaskType(), req.getTitle(), req.getTimeLimitMinutes(),
                req.getVisibility(), req.getAssigneeIds(), req.getViewerIds());
        checkAllowLateSubmit(req.getAllowLateSubmit());
        requireActiveMembers(task.getCompanyId(), req.getAssigneeIds(), "被指派人");
        requireActiveMembers(task.getCompanyId(), req.getViewerIds(), "可见人");

        String finalType = req.getTaskType() != null ? req.getTaskType() : task.getTaskType();
        String finalVisibility = req.getVisibility() != null ? req.getVisibility() : task.getVisibility();

        // 必须用「合并后的生效值」校验：否则对已存在的 ASSIGNED 任务传 assigneeIds=[]，
        // taskType 为 null 会跳过上面的校验，清空指派人后 task_type='ASSIGNED' 却无指派行，
        // 任务将永远无法被任何人完成（抢单条件要求 task_assignee 命中）。
        if ("ASSIGNED".equals(finalType) && req.getAssigneeIds() != null && req.getAssigneeIds().isEmpty()) {
            throw BizException.badRequest("指定任务必须至少保留一个被指派人");
        }
        if ("RESTRICTED".equals(finalVisibility) && req.getViewerIds() != null && req.getViewerIds().isEmpty()) {
            throw BizException.badRequest("指定人可见时必须至少保留一个可见人");
        }

        boolean hasTaskField = req.getTaskType() != null || req.getTitle() != null
                || req.getDescription() != null || req.getTimeLimitMinutes() != null
                || req.getVisibility() != null || req.getAllowLateSubmit() != null;
        boolean hasRelationField = req.getAssigneeIds() != null || req.getViewerIds() != null
                || req.getDetailImages() != null;
        if (!hasTaskField && !hasRelationField) {
            throw BizException.badRequest("没有需要修改的字段");
        }

        // 只有确实有 task 表字段要改时才执行 UPDATE：
        // 全为 null 时 MyBatis 的 <set> 会拼出 `UPDATE task WHERE id=?` 这种非法 SQL
        if (hasTaskField) {
            Task patch = new Task();
            patch.setId(taskId);
            patch.setTaskType(req.getTaskType());
            patch.setTitle(req.getTitle() == null ? null : req.getTitle().trim());
            patch.setDescription(req.getDescription());
            patch.setTimeLimitMinutes(req.getTimeLimitMinutes());
            patch.setVisibility(req.getVisibility());
            patch.setAllowLateSubmit(req.getAllowLateSubmit());
            taskMapper.update(patch);
        }

        // 编辑后以最新字段为准处理关联数据
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
        // 昵称一次性批量查，避免每条任务 2 次查询的 N+1
        Map<Long, String> nicknames = loadNicknames(tasks);
        List<TaskResponse> result = new ArrayList<>();
        for (Task t : tasks) {
            result.add(toResponse(t, false, nicknames));
        }
        return result;
    }

    public TaskResponse detail(Long taskId) {
        Long userId = UserContext.requireUserId();
        Task task = requireTask(taskId);
        requireCompany(task.getCompanyId());
        requireMember(task.getCompanyId());

        LoginUser me = UserContext.require();
        // 可见性口径必须与列表一致：管理者在列表里看得到，就必须点得进来
        boolean privileged = me.isAdmin() || isManagerOrFounder(task.getCompanyId(), userId);
        if (!privileged && !taskMapper.canView(taskId, userId)) {
            throw BizException.forbidden("无权查看该任务");
        }
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(task.getPublisherId());
        if (task.getCompletedBy() != null) {
            ids.add(task.getCompletedBy());
        }
        return toResponse(task, true, loadNicknames(ids));
    }

    /** T-4/T-7/T-8 完成/超时补交：条件 UPDATE 保证抢单原子性 */
    @Transactional
    public void complete(Long taskId, CompleteTaskRequest req) {
        Long userId = UserContext.requireUserId();
        Task task = requireTask(taskId);
        requireCompany(task.getCompanyId());
        requireMember(task.getCompanyId());

        int rows = taskMapper.complete(taskId, userId,
                req == null ? null : req.getSubmitContent());
        if (rows == 0) {
            throw BizException.conflict("提交失败：任务已被他人完成/已取消，或已超出补交时限，或无权执行该任务");
        }
        if (req != null) {
            saveImages(taskId, "SUBMIT", req.getSubmitImages());
        }
    }

    /** T-9 本公司创始人和管理者都可以取消 */
    @Transactional
    public void cancel(Long taskId, String cancelReason) {
        Long userId = UserContext.requireUserId();
        Task task = requireTask(taskId);
        requireManagerOrFounder(task.getCompanyId());
        if (cancelReason != null && cancelReason.length() > REASON_MAX) {
            throw BizException.badRequest("取消原因不能超过 " + REASON_MAX + " 个字符");
        }

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
        if (title != null) {
            if (title.isBlank()) {
                throw BizException.badRequest("任务标题不能为空");
            }
            if (title.trim().length() > TITLE_MAX) {
                throw BizException.badRequest("任务标题不能超过 " + TITLE_MAX + " 个字符");
            }
        }
        // T-3 限时 5 分钟 ~ 30 天
        if (timeLimitMinutes != null && (timeLimitMinutes < 5 || timeLimitMinutes > 43200)) {
            throw BizException.badRequest("限时时长必须在 5 分钟 ~ 30 天之间");
        }
        if (visibility != null && !"PUBLIC".equals(visibility) && !"RESTRICTED".equals(visibility)) {
            throw BizException.badRequest("可见范围只能是 PUBLIC 或 RESTRICTED");
        }
        checkRelationIds(assigneeIds, "被指派人");
        checkRelationIds(viewerIds, "可见人");
        if ("ASSIGNED".equals(taskType) && (assigneeIds == null || assigneeIds.isEmpty())) {
            throw BizException.badRequest("指定任务必须选择被指派人");
        }
        if ("RESTRICTED".equals(visibility) && (viewerIds == null || viewerIds.isEmpty())) {
            throw BizException.badRequest("指定人可见时必须选择可见人");
        }
    }

    /** allow_late_submit 是 0/1 开关，非法值会撞 CHECK 约束报 500 */
    private void checkAllowLateSubmit(Integer allowLateSubmit) {
        if (allowLateSubmit != null && allowLateSubmit != 0 && allowLateSubmit != 1) {
            throw BizException.badRequest("「允许超时补交」只能是 0 或 1");
        }
    }

    private void checkRelationIds(List<Long> ids, String label) {
        if (ids == null) {
            return;
        }
        if (ids.size() > MAX_RELATION_IDS) {
            throw BizException.badRequest(label + "最多 " + MAX_RELATION_IDS + " 个");
        }
        for (Long id : ids) {
            if (id == null) {
                throw BizException.badRequest(label + "中存在空值");
            }
        }
    }

    /** 传入的 ID 必须全部是该公司当前在职成员 */
    private void requireActiveMembers(Long companyId, List<Long> ids, String label) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Set<Long> distinct = new LinkedHashSet<>(ids);
        List<Long> valid = userCompanyMapper.selectActiveUserIds(companyId, new ArrayList<>(distinct));
        if (valid == null || valid.size() != distinct.size()) {
            throw BizException.badRequest(label + "必须是本公司的在职成员");
        }
    }

    private void saveAssignees(Long taskId, String taskType, List<Long> assigneeIds) {
        if (!"ASSIGNED".equals(taskType) || assigneeIds == null) {
            return;
        }
        // 去重：重复 ID 会撞 uk_task_user 唯一键报 500
        for (Long uid : new LinkedHashSet<>(assigneeIds)) {
            assigneeMapper.insert(taskId, uid);
        }
    }

    private void saveViewers(Long taskId, String visibility, List<Long> viewerIds) {
        if (!"RESTRICTED".equals(visibility) || viewerIds == null) {
            return;
        }
        for (Long uid : new LinkedHashSet<>(viewerIds)) {
            viewerMapper.insert(taskId, uid);
        }
    }

    private void saveImages(Long taskId, String imageType, List<String> objectKeys) {
        if (objectKeys == null) {
            return;
        }
        if (objectKeys.size() > MAX_IMAGES) {
            throw BizException.badRequest("图片最多 " + MAX_IMAGES + " 张");
        }
        for (int i = 0; i < objectKeys.size(); i++) {
            String objectKey = objectKeys.get(i);
            if (objectKey == null || objectKey.isBlank()) {
                throw BizException.badRequest("图片 objectKey 不能为空");
            }
            if (objectKey.length() > OBJECT_KEY_MAX) {
                throw BizException.badRequest("图片 objectKey 过长");
            }
            imageMapper.insert(taskId, imageType, objectKey, i);
        }
    }

    /** 批量取昵称：一次查询解决整个列表，避免 N+1 */
    private Map<Long, String> loadNicknames(List<Task> tasks) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Task t : tasks) {
            if (t.getPublisherId() != null) {
                ids.add(t.getPublisherId());
            }
            if (t.getCompletedBy() != null) {
                ids.add(t.getCompletedBy());
            }
        }
        return loadNicknames(ids);
    }

    private Map<Long, String> loadNicknames(Set<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        List<User> users = userMapper.selectByIds(new ArrayList<>(ids));
        if (users != null) {
            for (User u : users) {
                map.put(u.getId(), u.getNickname());
            }
        }
        return map;
    }

    private TaskResponse toResponse(Task t, boolean withDetails, Map<Long, String> nicknames) {
        TaskResponse r = new TaskResponse();
        r.setId(t.getId());
        r.setCompanyId(t.getCompanyId());
        r.setPublisherId(t.getPublisherId());
        r.setPublisherNickname(nicknames.get(t.getPublisherId()));
        r.setTaskType(t.getTaskType());
        r.setTitle(t.getTitle());
        r.setDescription(t.getDescription());
        r.setTimeLimitMinutes(t.getTimeLimitMinutes());
        r.setCreatedAt(t.getCreatedAt());
        r.setDeadlineAt(t.getDeadlineAt());
        r.setLateDeadlineAt(t.getLateDeadlineAt());
        r.setStatus(t.getStatus());
        r.setCompletedBy(t.getCompletedBy());
        r.setCompletedByNickname(nicknames.get(t.getCompletedBy()));
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
