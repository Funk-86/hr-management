package org.example.hrmanagement.module.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.module.position.entity.Position;
import org.example.hrmanagement.module.position.mapper.PositionMapper;
import org.example.hrmanagement.module.salary.dto.SalaryBaseDictSaveDTO;
import org.example.hrmanagement.module.salary.dto.TaskScoreBonusDictSaveDTO;
import org.example.hrmanagement.module.salary.entity.SalaryBaseDict;
import org.example.hrmanagement.module.salary.entity.TaskScoreBonusDict;
import org.example.hrmanagement.module.salary.mapper.SalaryBaseDictMapper;
import org.example.hrmanagement.module.salary.mapper.TaskScoreBonusDictMapper;
import org.example.hrmanagement.module.salary.service.SalaryDictService;
import org.example.hrmanagement.module.salary.vo.SalaryBaseDictVO;
import org.example.hrmanagement.module.salary.vo.TaskScoreBonusDictVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryDictServiceImpl implements SalaryDictService {

    private final SalaryBaseDictMapper salaryBaseDictMapper;
    private final TaskScoreBonusDictMapper taskScoreBonusDictMapper;
    private final PositionMapper positionMapper;

    @Override
    public List<SalaryBaseDictVO> listBaseDict() {
        List<SalaryBaseDict> list = salaryBaseDictMapper.selectList(
                new LambdaQueryWrapper<SalaryBaseDict>().orderByAsc(SalaryBaseDict::getPositionId));
        if (list.isEmpty()) {
            return List.of();
        }
        Set<Long> positionIds = list.stream()
                .map(SalaryBaseDict::getPositionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = positionIds.isEmpty()
                ? Map.of()
                : positionMapper.selectBatchIds(positionIds).stream()
                .collect(Collectors.toMap(Position::getId, Position::getPositionName, (a, b) -> a));
        return list.stream().map(d -> {
            SalaryBaseDictVO vo = new SalaryBaseDictVO();
            vo.setId(d.getId());
            vo.setPositionId(d.getPositionId());
            vo.setPositionName(nameMap.get(d.getPositionId()));
            vo.setBaseSalary(d.getBaseSalary());
            vo.setStatus(d.getStatus());
            vo.setRemark(d.getRemark());
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBaseDict(SalaryBaseDictSaveDTO dto) {
        requirePosition(dto.getPositionId());
        Long count = salaryBaseDictMapper.selectCount(
                new LambdaQueryWrapper<SalaryBaseDict>()
                        .eq(SalaryBaseDict::getPositionId, dto.getPositionId()));
        if (count != null && count > 0) {
            throw new BusinessException("该岗位已配置底薪，请直接修改");
        }
        SalaryBaseDict entity = new SalaryBaseDict();
        entity.setPositionId(dto.getPositionId());
        entity.setBaseSalary(dto.getBaseSalary());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        salaryBaseDictMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBaseDict(Long id, SalaryBaseDictSaveDTO dto) {
        SalaryBaseDict existing = salaryBaseDictMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("底薪字典不存在");
        }
        requirePosition(dto.getPositionId());
        Long count = salaryBaseDictMapper.selectCount(
                new LambdaQueryWrapper<SalaryBaseDict>()
                        .eq(SalaryBaseDict::getPositionId, dto.getPositionId())
                        .ne(SalaryBaseDict::getId, id));
        if (count != null && count > 0) {
            throw new BusinessException("该岗位已配置底薪");
        }
        existing.setPositionId(dto.getPositionId());
        existing.setBaseSalary(dto.getBaseSalary());
        existing.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        existing.setRemark(dto.getRemark());
        salaryBaseDictMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBaseDict(Long id) {
        if (salaryBaseDictMapper.selectById(id) == null) {
            throw new BusinessException("底薪字典不存在");
        }
        salaryBaseDictMapper.deleteById(id);
    }

    @Override
    public List<TaskScoreBonusDictVO> listScoreBonusDict() {
        return taskScoreBonusDictMapper.selectList(
                        new LambdaQueryWrapper<TaskScoreBonusDict>().orderByAsc(TaskScoreBonusDict::getGrade))
                .stream()
                .map(this::toScoreVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScoreBonusDict(TaskScoreBonusDictSaveDTO dto) {
        Long count = taskScoreBonusDictMapper.selectCount(
                new LambdaQueryWrapper<TaskScoreBonusDict>()
                        .eq(TaskScoreBonusDict::getGrade, dto.getGrade()));
        if (count != null && count > 0) {
            throw new BusinessException("该评分等级已存在，请直接修改");
        }
        TaskScoreBonusDict entity = new TaskScoreBonusDict();
        entity.setGrade(dto.getGrade());
        entity.setGradeLabel(dto.getGradeLabel().trim());
        entity.setBonusAmount(dto.getBonusAmount());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        taskScoreBonusDictMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScoreBonusDict(Long id, TaskScoreBonusDictSaveDTO dto) {
        TaskScoreBonusDict existing = taskScoreBonusDictMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("评分奖金字典不存在");
        }
        Long count = taskScoreBonusDictMapper.selectCount(
                new LambdaQueryWrapper<TaskScoreBonusDict>()
                        .eq(TaskScoreBonusDict::getGrade, dto.getGrade())
                        .ne(TaskScoreBonusDict::getId, id));
        if (count != null && count > 0) {
            throw new BusinessException("该评分等级已存在");
        }
        existing.setGrade(dto.getGrade());
        existing.setGradeLabel(dto.getGradeLabel().trim());
        existing.setBonusAmount(dto.getBonusAmount());
        existing.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        taskScoreBonusDictMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScoreBonusDict(Long id) {
        if (taskScoreBonusDictMapper.selectById(id) == null) {
            throw new BusinessException("评分奖金字典不存在");
        }
        taskScoreBonusDictMapper.deleteById(id);
    }

    private void requirePosition(Long positionId) {
        if (positionMapper.selectById(positionId) == null) {
            throw new BusinessException("岗位不存在");
        }
    }

    private TaskScoreBonusDictVO toScoreVo(TaskScoreBonusDict d) {
        TaskScoreBonusDictVO vo = new TaskScoreBonusDictVO();
        vo.setId(d.getId());
        vo.setGrade(d.getGrade());
        vo.setGradeLabel(d.getGradeLabel());
        vo.setBonusAmount(d.getBonusAmount());
        vo.setStatus(d.getStatus());
        return vo;
    }
}
