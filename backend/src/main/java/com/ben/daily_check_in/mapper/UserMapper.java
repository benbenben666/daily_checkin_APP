package com.ben.daily_check_in.mapper;

import com.ben.daily_check_in.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User selectById(@Param("id") Long id);

    User selectByPhone(@Param("phone") String phone);

    /** 列表昵称批量填充专用：只取 id / nickname，不读 password */
    List<User> selectByIds(@Param("ids") List<Long> ids);

    /** 锁住用户行（SELECT ... FOR UPDATE），用于把「每天最多申请 3 次」串行化 */
    Long lockById(@Param("id") Long id);

    /** 注册：不写 system_role，由数据库默认值给出 'USER' */
    int insert(@Param("phone") String phone,
               @Param("password") String password,
               @Param("nickname") String nickname);

    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int updateAvatar(@Param("id") Long id, @Param("avatarKey") String avatarKey);

    /** 软删除 = 禁用 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateSystemRole(@Param("id") Long id, @Param("systemRole") String systemRole);

    List<User> selectAll(@Param("keyword") String keyword);
}
