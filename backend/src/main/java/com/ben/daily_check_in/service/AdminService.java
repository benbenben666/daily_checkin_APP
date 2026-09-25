package com.ben.daily_check_in.service;

import com.ben.daily_check_in.common.BizException;
import com.ben.daily_check_in.dto.admin.*;
import com.ben.daily_check_in.entity.Company;
import com.ben.daily_check_in.entity.User;
import com.ben.daily_check_in.mapper.CompanyMapper;
import com.ben.daily_check_in.mapper.UserCompanyMapper;
import com.ben.daily_check_in.mapper.UserMapper;
import com.ben.daily_check_in.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统管理（仅 ADMIN）。
 */
@Service
public class AdminService {

    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final UserCompanyMapper userCompanyMapper;

    public AdminService(UserMapper userMapper, CompanyMapper companyMapper,
                        UserCompanyMapper userCompanyMapper) {
        this.userMapper = userMapper;
        this.companyMapper = companyMapper;
        this.userCompanyMapper = userCompanyMapper;
    }

    public List<AdminUserResponse> listUsers(String keyword) {
        requireAdmin();
        List<AdminUserResponse> result = new ArrayList<>();
        for (User u : userMapper.selectAll(keyword)) {
            AdminUserResponse r = new AdminUserResponse();
            r.setId(u.getId());
            r.setPhone(u.getPhone());
            r.setNickname(u.getNickname());
            r.setAvatarKey(u.getAvatarKey());
            r.setSystemRole(u.getSystemRole());
            r.setStatus(u.getStatus());
            r.setCreatedAt(u.getCreatedAt());
            result.add(r);
        }
        return result;
    }

    public void updateUser(Long userId, AdminUpdateUserRequest req) {
        requireAdmin();
        requireUser(userId);
        if (req.getNickname() != null) {
            userMapper.updateNickname(userId, req.getNickname());
        }
        if (req.getStatus() != null) {
            if (req.getStatus() != 0 && req.getStatus() != 1) {
                throw BizException.badRequest("status 只能是 0 或 1");
            }
            userMapper.updateStatus(userId, req.getStatus());
        }
    }

    /** A-5 删除用户 = 软删除（禁用） */
    public void deleteUser(Long userId) {
        Long operatorId = UserContext.requireUserId();
        requireAdmin();
        if (userId.equals(operatorId)) {
            throw BizException.badRequest("不能删除自己");
        }
        requireUser(userId);
        userMapper.updateStatus(userId, 0);
    }

    /** 添加/删除管理员 */
    public void updateRole(Long userId, String systemRole) {
        Long operatorId = UserContext.requireUserId();
        requireAdmin();
        if (!"USER".equals(systemRole) && !"ADMIN".equals(systemRole)) {
            throw BizException.badRequest("角色只能是 USER 或 ADMIN");
        }
        if (userId.equals(operatorId) && "USER".equals(systemRole)) {
            throw BizException.badRequest("不能取消自己的管理员身份");
        }
        requireUser(userId);
        userMapper.updateSystemRole(userId, systemRole);
    }

    public List<AdminCompanyResponse> listCompanies(String keyword) {
        requireAdmin();
        List<AdminCompanyResponse> result = new ArrayList<>();
        for (Company c : companyMapper.selectAll(keyword)) {
            AdminCompanyResponse r = new AdminCompanyResponse();
            r.setId(c.getId());
            r.setName(c.getName());
            r.setInviteCode(c.getInviteCode());
            r.setStatus(c.getStatus());
            r.setMemberCount(userCompanyMapper.countMembers(c.getId()));
            r.setCreatedAt(c.getCreatedAt());
            r.setDissolvedAt(c.getDissolvedAt());
            result.add(r);
        }
        return result;
    }

    /** 系统管理员可解散任意公司 */
    @Transactional
    public void dissolveCompany(Long companyId) {
        Long operatorId = UserContext.requireUserId();
        requireAdmin();
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw BizException.notFound("公司不存在");
        }
        if (company.getStatus() == 0) {
            throw BizException.conflict("公司已解散");
        }
        companyMapper.dissolve(companyId, operatorId);
    }

    private void requireAdmin() {
        if (!UserContext.require().isAdmin()) {
            throw BizException.forbidden("需要系统管理员权限");
        }
    }

    private void requireUser(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw BizException.notFound("用户不存在");
        }
    }
}
