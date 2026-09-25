package com.ben.daily_check_in.mapper;

import com.ben.daily_check_in.entity.TaskImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskImageMapper {

    int insert(@Param("taskId") Long taskId,
               @Param("imageType") String imageType,
               @Param("objectKey") String objectKey,
               @Param("sortOrder") Integer sortOrder);

    /** 删除某任务的某类图片（编辑时整体替换） */
    int deleteByTaskAndType(@Param("taskId") Long taskId, @Param("imageType") String imageType);

    List<TaskImage> selectByTaskAndType(@Param("taskId") Long taskId, @Param("imageType") String imageType);
}
