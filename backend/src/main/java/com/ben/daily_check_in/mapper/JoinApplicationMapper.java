package com.ben.daily_check_in.mapper;

import com.ben.daily_check_in.entity.JoinApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JoinApplicationMapper {

    int insert(@Param("userId") Long userId,
               @Param("companyId") Long companyId,
               @Param("applyRole") String applyRole);

    JoinApplication selectById(@Param("id") Long id);

    /** 同一用户对同一公司今日申请次数（全部状态计入） */
    int countToday(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /** 是否已有待审批申请 */
    boolean hasPending(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /** 某用户对某公司最新的待审批申请 */
    JoinApplication selectLatestPending(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /** 待审批列表 */
    List<com.ben.daily_check_in.dto.application.ApplicationResponse> selectPending(@Param("companyId") Long companyId);

    /** 全部申请历史 */
    List<com.ben.daily_check_in.dto.application.ApplicationResponse> selectHistory(@Param("companyId") Long companyId);

    /** 我提交的申请（用于前端展示我的申请状态） */
    List<com.ben.daily_check_in.dto.application.ApplicationResponse> selectByUser(@Param("userId") Long userId);

    /** 审批通过：仅当仍为 PENDING 时生效，影响 0 行说明已被他人处理 */
    int approve(@Param("id") Long id, @Param("reviewerId") Long reviewerId);

    int reject(@Param("id") Long id, @Param("reviewerId") Long reviewerId, @Param("rejectReason") String rejectReason);

    /** 申请人撤回 */
    int cancelByUser(@Param("id") Long id, @Param("userId") Long userId);

    /** 拉黑时把待审批申请置为已拒绝（不暴露拉黑事实） */
    int rejectPendingByBlacklist(@Param("userId") Long userId, @Param("companyId") Long companyId);
}
