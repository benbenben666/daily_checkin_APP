package com.ben.daily_check_in.mapper;

import com.ben.daily_check_in.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMapper {

    int insert(Task task);

    Task selectById(@Param("id") Long id);

    /** 编辑任务（只更新非 null 字段） */
    int update(Task task);

    /**
     * 完成/补交任务（抢单原子性核心）：一条带条件的 UPDATE，
     * 同时完成「检查状态 + 检查补交窗口 + 检查执行权限 + 写入完成」。
     * 影响 1 行 = 成功，0 行 = 被抢先/已取消/无权限/超补交窗口。
     */
    int complete(@Param("taskId") Long taskId,
                 @Param("userId") Long userId,
                 @Param("submitContent") String submitContent);

    /** 取消任务：仅 PENDING / EXPIRED 可取消 */
    int cancel(@Param("taskId") Long taskId,
               @Param("cancelledBy") Long cancelledBy,
               @Param("cancelReason") String cancelReason);

    /** 定时任务：把已过截止时间的 PENDING 任务标记为 EXPIRED */
    int markExpired();

    /** 创始人/管理者/管理员：本公司全部任务 */
    List<Task> selectAllByCompany(@Param("companyId") Long companyId);

    /** 员工：待抢视图 */
    List<Task> selectGrabList(@Param("companyId") Long companyId, @Param("userId") Long userId);

    /** 员工：历史视图 */
    List<Task> selectHistoryList(@Param("companyId") Long companyId, @Param("userId") Long userId);

    /** 单条任务可见性校验 */
    boolean canView(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
