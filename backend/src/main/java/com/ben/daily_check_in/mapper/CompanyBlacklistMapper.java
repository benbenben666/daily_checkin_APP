package com.ben.daily_check_in.mapper;

import com.ben.daily_check_in.entity.CompanyBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompanyBlacklistMapper {

    /** 写黑名单（幂等：重复拉黑更新操作人与原因） */
    int upsert(@Param("companyId") Long companyId,
               @Param("userId") Long userId,
               @Param("operatorId") Long operatorId,
               @Param("reason") String reason);

    boolean exists(@Param("companyId") Long companyId, @Param("userId") Long userId);

    int delete(@Param("companyId") Long companyId, @Param("userId") Long userId);

    List<CompanyBlacklist> selectByCompany(@Param("companyId") Long companyId);
}
