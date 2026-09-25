package com.ben.daily_check_in.mapper;

import com.ben.daily_check_in.entity.TaskAssignee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskAssigneeMapper {

    int insert(@Param("taskId") Long taskId, @Param("userId") Long userId);

    int deleteByTask(@Param("taskId") Long taskId);

    List<TaskAssignee> selectByTask(@Param("taskId") Long taskId);

    boolean isAssignee(@Param("taskId") Long taskId, @Param("userId") Long userId);

    /** 带昵称的指派人列表 */
    List<com.ben.daily_check_in.dto.task.TaskResponse.AssigneeItem> selectWithUser(@Param("taskId") Long taskId);
}
