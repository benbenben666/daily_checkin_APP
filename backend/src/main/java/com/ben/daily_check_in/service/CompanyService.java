package com.ben.daily_check_in.service;

import com.ben.daily_check_in.common.BizException;
import com.ben.daily_check_in.dto.company.*;
import com.ben.daily_check_in.entity.Company;
import com.ben.daily_check_in.entity.UserCompany;
import com.ben.daily_check_in.mapper.CompanyBlacklistMapper;
import com.ben.daily_check_in.mapper.CompanyMapper;
import com.ben.daily_check_in.mapper.JoinApplicationMapper;
import com.ben.daily_check_in.mapper.UserCompanyMapper;
import com.ben.daily_check_in.security.LoginUser;
import com.ben.daily_check_in.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * 公司：创建 / 我的公司 / 详情 / 申请加入 / 成员管理 / 拉黑 / 解散。
 */
@Service
public class CompanyService {

    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CompanyMapper companyMapper;
    private final UserCompanyMapper userCompanyMapper;
    private final JoinApplicationMapper applicationMapper;
    private final CompanyBlacklistMapper blacklistMapper;

    public CompanyService(CompanyMapper companyMapper, UserCompanyMapper userCompanyMapper,
                          JoinApplicationMapper applicationMapper, CompanyBlacklistMapper blacklistMapper) {
        this.companyMapper = companyMapper;
        this.userCompanyMapper = userCompanyMapper;
        this.applicationMapper = applicationMapper;
        this.blacklistMapper = blacklistMapper;
    }

    /** C-1 任何用户可创建公司，创建者自动成为创始人（同一事务） */
    @Transactional
    public Long createCompany(CreateCompanyRequest req) {
        Long userId = UserContext.requireUserId();
        if (req.getName() == null || req.getName().isBlank()) {
            throw BizException.badRequest("公司名称不能为空");
        }
        // 生成唯一邀请码（撞唯一键则重试）
        String inviteCode;
        do {
            inviteCode = generateInviteCode();
        } while (companyMapper.selectByInviteCode(inviteCode) != null);

        companyMapper.insert(req.getName().trim(), inviteCode);
        Company company = companyMapper.selectByInviteCode(inviteCode);
        // uk_one_founder 保证每家公司不会出现第二个创始人
        userCompanyMapper.insert(userId, company.getId(), "FOUNDER");
        return company.getId();
    }

    /** 我所在的公司及角色 */
    public List<MyCompanyResponse> myCompanies() {
        Long userId = UserContext.requireUserId();
        List<UserCompany> relations = userCompanyMapper.selectActiveByUser(userId);
        List<MyCompanyResponse> result = new ArrayList<>();
        for (UserCompany uc : relations) {
            Company company = companyMapper.selectById(uc.getCompanyId());
            if (company == null || company.getStatus() == 0) {
                continue;
            }
            MyCompanyResponse item = new MyCompanyResponse();
            item.setCompanyId(company.getId());
            item.setCompanyName(company.getName());
            item.setCompanyRole(uc.getCompanyRole());
            item.setJoinedAt(uc.getJoinedAt());
            result.add(item);
        }
        return result;
    }

    public CompanyDetailResponse detail(Long companyId) {
        Long userId = UserContext.requireUserId();
        Company company = requireCompany(companyId);
        CompanyDetailResponse resp = new CompanyDetailResponse();
        resp.setId(company.getId());
        resp.setName(company.getName());
        resp.setStatus(company.getStatus());
        resp.setMemberCount(userCompanyMapper.countMembers(companyId));
        resp.setCreatedAt(company.getCreatedAt());
        String myRole = userCompanyMapper.selectActiveRole(userId, companyId);
        resp.setMyRole(myRole);
        // 邀请码仅创始人/管理者/系统管理员可见
        if ("FOUNDER".equals(myRole) || "MANAGER".equals(myRole) || UserContext.require().isAdmin()) {
            resp.setInviteCode(company.getInviteCode());
        }
        return resp;
    }

    /** M-1~M-6 申请加入公司 */
    public Long join(JoinCompanyRequest req) {
        Long userId = UserContext.requireUserId();
        if (req.getInviteCode() == null || req.getInviteCode().isBlank()) {
            throw BizException.badRequest("邀请码不能为空");
        }
        if (!"MANAGER".equals(req.getApplyRole()) && !"EMPLOYEE".equals(req.getApplyRole())) {
            throw BizException.badRequest("申请身份只能是管理者或员工");
        }
        Company company = companyMapper.selectByInviteCode(req.getInviteCode().trim());
        if (company == null || company.getStatus() == 0) {
            throw BizException.notFound("邀请码无效或公司已解散");
        }
        if (blacklistMapper.exists(company.getId(), userId)) {
            throw BizException.forbidden("无法申请该公司");
        }
        if (userCompanyMapper.selectActive(userId, company.getId()) != null) {
            throw BizException.conflict("你已是该公司成员");
        }
        // M-3 / M-5 每日最多 3 次，全部状态计入
        if (applicationMapper.countToday(userId, company.getId()) >= 3) {
            throw BizException.conflict("今日申请次数已达上限（3 次）");
        }
        // M-4 已有待审批申请时不能再申请
        if (applicationMapper.hasPending(userId, company.getId())) {
            throw BizException.conflict("已有待审批的申请，请勿重复提交");
        }
        applicationMapper.insert(userId, company.getId(), req.getApplyRole());
        com.ben.daily_check_in.entity.JoinApplication created =
                applicationMapper.selectLatestPending(userId, company.getId());
        return created == null ? null : created.getId();
    }

    public List<MemberResponse> members(Long companyId) {
        requireCompany(companyId);
        requireMember(companyId);
        return userCompanyMapper.selectMembers(companyId);
    }

    /** M-12 移除成员（不拉黑）。创始人不可被移除 */
    @Transactional
    public void removeMember(Long companyId, Long targetUserId) {
        Long operatorId = UserContext.requireUserId();
        requireCompany(companyId);
        requireManagerOrFounder(companyId);
        if (operatorId.equals(targetUserId)) {
            throw BizException.badRequest("不能移除自己");
        }
        String targetRole = userCompanyMapper.selectActiveRole(targetUserId, companyId);
        if (targetRole == null) {
            throw BizException.notFound("该用户不是本公司在职成员");
        }
        if ("FOUNDER".equals(targetRole)) {
            throw BizException.forbidden("不能移除创始人");
        }
        userCompanyMapper.removeMember(targetUserId, companyId);
    }

    /** M-9~M-11 拉黑（在职/非在职合并为一条代码路径，见开发文档 7.8） */
    @Transactional
    public void blacklist(Long companyId, BlacklistRequest req) {
        Long operatorId = UserContext.requireUserId();
        requireCompany(companyId);
        requireManagerOrFounder(companyId);
        if (req.getUserId() == null) {
            throw BizException.badRequest("缺少被拉黑用户");
        }
        if (operatorId.equals(req.getUserId())) {
            throw BizException.badRequest("不能拉黑自己");
        }
        String targetRole = userCompanyMapper.selectActiveRole(req.getUserId(), companyId);
        if ("FOUNDER".equals(targetRole)) {
            throw BizException.forbidden("不能拉黑创始人");
        }
        // ① 写黑名单（幂等）
        blacklistMapper.upsert(companyId, req.getUserId(), operatorId, req.getReason());
        // ② 踢出公司——只在职时生效，否则影响 0 行
        userCompanyMapper.kickByBlacklist(req.getUserId(), companyId);
        // ③ 待审批申请 → 显示为被拒绝（不暴露拉黑事实）
        applicationMapper.rejectPendingByBlacklist(req.getUserId(), companyId);
    }

    /** C-5 解散公司：仅创始人可解散本公司；系统管理员可解散任意公司 */
    @Transactional
    public void dissolve(Long companyId) {
        LoginUser me = UserContext.require();
        Company company = requireCompany(companyId);
        if (company.getStatus() == 0) {
            throw BizException.conflict("公司已解散");
        }
        if (!me.isAdmin()) {
            String myRole = userCompanyMapper.selectActiveRole(me.userId(), companyId);
            if (!"FOUNDER".equals(myRole)) {
                throw BizException.forbidden("只有创始人或系统管理员可以解散公司");
            }
        }
        companyMapper.dissolve(companyId, me.userId());
    }

    // ---------- 内部工具 ----------

    private Company requireCompany(Long companyId) {
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw BizException.notFound("公司不存在");
        }
        return company;
    }

    /** 必须是该公司在职成员或系统管理员 */
    private void requireMember(Long companyId) {
        LoginUser me = UserContext.require();
        if (me.isAdmin()) {
            return;
        }
        if (userCompanyMapper.selectActive(me.userId(), companyId) == null) {
            throw BizException.forbidden("你不是该公司成员");
        }
    }

    /** 必须是该公司的创始人/管理者或系统管理员 */
    private void requireManagerOrFounder(Long companyId) {
        LoginUser me = UserContext.require();
        if (me.isAdmin()) {
            return;
        }
        String role = userCompanyMapper.selectActiveRole(me.userId(), companyId);
        if (!"FOUNDER".equals(role) && !"MANAGER".equals(role)) {
            throw BizException.forbidden("需要创始人或管理者权限");
        }
    }

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
