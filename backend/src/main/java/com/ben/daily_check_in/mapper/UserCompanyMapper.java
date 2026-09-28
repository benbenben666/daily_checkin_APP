package com.ben.daily_check_in.mapper;

import com.ben.daily_check_in.entity.UserCompany;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCompanyMapper {

    int insert(@Param("userId") Long userId,
               @Param("companyId") Long companyId,
               @Param("companyRole") String companyRole);

    /** 审批通过时复用原成员记录行（曾离职则恢复在职） */
    int upsertOnApprove(@Param("userId") Long userId,
                        @Param("companyId") Long companyId,
                        @Param("companyRole") String companyRole);

    UserCompany selectActive(@Param("userId") Long userId, @Param("companyId") Long companyId);

    UserCompany selectAny(@Param("userId") Long userId, @Param("companyId") Long companyId);

    List<UserCompany> selectActiveByUser(@Param("userId") Long userId);

    /** 成员列表（在职） */
    List<com.ben.daily_check_in.dto.company.MemberResponse> selectMembers(@Param("companyId") Long companyId);

    int countMembers(@Param("companyId") Long companyId);

    /** 从给定 ID 中筛出「在该公司当前在职」的那部分，用于校验指派人 */
    List<Long> selectActiveUserIds(@Param("companyId") Long companyId,
                                   @Param("userIds") List<Long> userIds);

    /** 移除成员（不拉黑），仅在职时生效 */
    int removeMember(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /** 主动退出公司（M-13），仅在职时生效，离职方式记为 1 */
    int leave(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /** 拉黑踢出，仅在职时生效 */
    int kickByBlacklist(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /** 公司当前是否有在职管理者 */
    boolean hasManager(@Param("companyId") Long companyId);

    /** 某用户在某公司的在职角色，无则返回 null */
    String selectActiveRole(@Param("userId") Long userId, @Param("companyId") Long companyId);
}
