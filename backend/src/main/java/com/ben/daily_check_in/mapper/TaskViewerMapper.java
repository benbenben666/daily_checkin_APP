package com.ben.daily_check_in.mapper;

import com.ben.daily_check_in.entity.TaskViewer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskViewerMapper {

    int insert(@Param("taskId") Long taskId, @Param("userId") Long userId);

    int deleteByTask(@Param("taskId") Long taskId);

    List<TaskViewer> selectByTask(@Param("taskId") Long taskId);

    /** 带昵称的可见人列表 */
    List<com.ben.daily_check_in.dto.task.TaskResponse.ViewerItem> selectWithUser(@Param("taskId") Long taskId);
}
