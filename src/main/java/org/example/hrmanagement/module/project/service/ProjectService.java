package org.example.hrmanagement.module.project.service;

import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.project.dto.ProjectCreateDTO;
import org.example.hrmanagement.module.project.dto.ProjectProgressDTO;
import org.example.hrmanagement.module.project.dto.ProjectUpdateDTO;
import org.example.hrmanagement.module.project.vo.ProjectVO;
import org.example.hrmanagement.module.task.vo.TaskVO;

import java.util.List;

public interface ProjectService {

    PageResult<ProjectVO> page(String scope, Integer status, PageQuery pageQuery);

    ProjectVO getDetail(Long id);

    List<TaskVO> listProjectTasks(Long projectId);

    Long create(ProjectCreateDTO dto);

    void update(Long id, ProjectUpdateDTO dto);

    void updateProgress(Long id, ProjectProgressDTO dto);

    void close(Long id);

    /** 由任务进度驱动：未锁定时按根任务进度均值回写 */
    void refreshProgressFromTasks(Long projectId);

    /** 当前用户参与（负责人或成员）且未关闭的项目数 */
    long countMyActiveProjects();
}
