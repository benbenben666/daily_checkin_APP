package com.ben.daily_check_in.mapper;

import com.ben.daily_check_in.entity.Company;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompanyMapper {

    /** 插入后 company.id 会被回填（useGeneratedKeys） */
    int insert(Company company);

    Company selectById(@Param("id") Long id);

    Company selectByInviteCode(@Param("inviteCode") String inviteCode);

    /** 解散公司 */
    int dissolve(@Param("id") Long id, @Param("dissolvedBy") Long dissolvedBy);

    List<Company> selectAll(@Param("keyword") String keyword);
}
