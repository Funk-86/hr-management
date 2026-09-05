package org.example.hrmanagement.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.hrmanagement.module.task.entity.Task;

public interface TaskMapper extends BaseMapper<Task> {

    @Select("SELECT * FROM hr_task WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Task selectByIdForUpdate(@Param("id") Long id);
}
