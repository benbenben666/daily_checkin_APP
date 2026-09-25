package com.ben.daily_check_in.service;

import com.ben.daily_check_in.common.BizException;
import com.ben.daily_check_in.dto.application.ApplicationResponse;
import com.ben.daily_check_in.entity.Company;
import com.ben.daily_check_in.entity.JoinApplication;
import com.ben.daily_check_in.mapper.CompanyMapper;
import com.ben.daily_check_in.mapper.JoinApplicationMapper;
import com.ben.daily_check_in.mapper.UserCompanyMapper;
import com.ben.daily_check_in.security.LoginUser;
import com.ben.daily_check_in.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 申请与审批。审批权限见开发文档 M-8 / 7.7：
 * 申请 MANAGER 仅创始人可批；申请 EMPLOYEE 创始人和管理者都可批；
 * 公司当前没有管理者时一律回退给创始人。
 */
@Service
public class ApplicationService {

    private final JoinApplicationMapper applicationMapper;
    private final UserCompanyMapper userCompanyMapper;
    private final CompanyMapper companyMapper;

    public ApplicationService(JoinApplicationMapper applicationMapper,
                              UserCompanyMapper userCompanyMapper,
                              CompanyMapper companyMapper) {
        this.applicationMapper = applicationMapper;
        this.userCompanyMapper = userCompanyMapper;
        this.companyMapper = companyMapper;
    }

    /** 待审批列表（仅创始人/管理者/系统管理员） */
    public List<ApplicationResponse> pending(Long companyId) {
        requireReviewer(companyId);
        return applicationMapper.selectPending(companyId);
    }

    /** 全部申请历史（仅创始人/管理者/系统管理员） */
    public List<ApplicationResponse> history(Long companyId) {
        requireReviewer(companyId);
        return applicationMapper.selectHistory(companyId);
    }

    /** 我提交的申请 */
    public List<ApplicationResponse> mine() {
        return applicationMapper.selectByUser(UserContext.requireUserId());
    }

    /** 通过（事务：先条件更新申请状态，再写成员关系） */
    @Transactional
    public void approve(Long applicationId) {
        Long reviewerId = UserContext.requireUserId();
        JoinApplication app = requirePending(applicationId);
        checkReviewPermission(app.getCompanyId(), reviewerId, app.getApplyRole());

        // 影响 0 行说明已被他人处理
        int rows = applicationMapper.approve(applicationId, reviewerId);
        if (rows == 0) {
            throw BizException.conflict("该申请已被处理");
        }
        // 曾离职则复用原成员记录行，避免撞唯一键
        userCompanyMapper.upsertOnApprove(app.getUserId(), app.getCompanyId(), app.getApplyRole());
    }

    public void reject(Long applicationId, String rejectReason) {
        Long reviewerId = UserContext.requireUserId();
        JoinApplication app = requirePending(applicationId);
        checkReviewPermission(app.getCompanyId(), reviewerId, app.getApplyRole());

        int rows = applicationMapper.reject(applicationId, reviewerId, rejectReason);
        if (rows == 0) {
            throw BizException.conflict("该申请已被处理");
        }
    }

    /** 申请人撤回 */
    public void cancel(Long applicationId) {
        Long userId = UserContext.requireUserId();
        int rows = applicationMapper.cancelByUser(applicationId, userId);
        if (rows == 0) {
            throw BizException.conflict("申请不存在或已处理，无法撤回");
        }
    }

    // ---------- 内部工具 ----------

    private JoinApplication requirePending(Long applicationId) {
        JoinApplication app = applicationMapper.selectById(applicationId);
        if (app == null) {
            throw BizException.notFound("申请不存在");
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw BizException.conflict("该申请已被处理");
        }
        return app;
    }

    /** 是否有权查看/处理该公司的申请 */
    private void requireReviewer(Long companyId) {
        LoginUser me = UserContext.require();
        if (me.isAdmin()) {
            return;
        }
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw BizException.notFound("公司不存在");
        }
        String role = userCompanyMapper.selectActiveRole(me.userId(), companyId);
        if (!"FOUNDER".equals(role) && !"MANAGER".equals(role)) {
            throw BizException.forbidden("需要创始人或管理者权限");
        }
    }

    /** 按申请身份与审批人角色校验审批权限 */
    private void checkReviewPermission(Long companyId, Long reviewerId, String applyRole) {
        LoginUser me = UserContext.require();
        if (me.isAdmin()) {
            return;
        }
        String reviewerRole = userCompanyMapper.selectActiveRole(reviewerId, companyId);
        if (reviewerRole == null) {
            throw BizException.forbidden("你不是该公司成员");
        }
        if ("MANAGER".equals(applyRole)) {
            // 申请管理者只能由创始人审批
            if (!"FOUNDER".equals(reviewerRole)) {
                throw BizException.forbidden("管理者申请只能由创始人审批");
            }
            return;
        }
        // 申请员工：创始人或管理者可批。
        // 「公司当前没有管理者时回退给创始人」无需额外判断：
        // 若公司没有在职管理者，审批人就不可能是 MANAGER，自然只剩创始人。
        if (!"FOUNDER".equals(reviewerRole) && !"MANAGER".equals(reviewerRole)) {
            throw BizException.forbidden("需要创始人或管理者权限");
        }
    }
}
