package org.example.hrmanagement.module.position.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.module.department.entity.Department;
import org.example.hrmanagement.module.department.mapper.DepartmentMapper;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.position.dto.PositionCreateDTO;
import org.example.hrmanagement.module.position.dto.PositionUpdateDTO;
import org.example.hrmanagement.module.position.entity.Position;
import org.example.hrmanagement.module.position.mapper.PositionMapper;
import org.example.hrmanagement.module.position.service.PositionService;
import org.example.hrmanagement.module.position.vo.PositionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PositionServiceImpl implements PositionService {
    @Autowired
    private PositionMapper positionMapper;
    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private EmployeeMapper employeeMapper;


    @Override
    public List<PositionVO> listByDeptId(Long deptId) {
        if (deptId == null) {
            throw new BusinessException("部门id不能为空");
        }

        Department department = departmentMapper.selectById(deptId);
        if (department == null) {
            throw new BusinessException("部门不存在");
        }

        List<Long> deptIds = collectDeptIdsIncludingDescendants(deptId);
        List<Position> positions = positionMapper.selectList(
                new LambdaQueryWrapper<Position>()
                        .in(Position::getDeptId, deptIds)
                        .orderByAsc(Position::getDeptId)
                        .orderByAsc(Position::getPositionName));

        Map<Long, String> deptNameMap = departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));

        return positions.stream()
                .map(p -> toVO(p, deptNameMap.getOrDefault(p.getDeptId(), department.getDeptName())))
                .collect(Collectors.toList());
    }

    /** 含本部门及全部下级部门 ID（选总公司时可看全公司岗位） */
    private List<Long> collectDeptIdsIncludingDescendants(Long deptId) {
        Set<Long> ids = new HashSet<>();
        ids.add(deptId);
        List<Department> all = departmentMapper.selectList(null);
        collectChildDeptIds(all, deptId, ids);
        return new ArrayList<>(ids);
    }

    private void collectChildDeptIds(List<Department> all, Long parentId, Set<Long> ids) {
        for (Department d : all) {
            if (Objects.equals(d.getParentId(), parentId)) {
                ids.add(d.getId());
                collectChildDeptIds(all, d.getId(), ids);
            }
        }
    }

    @Override
    public PositionVO getById(Long id) {
        Position position = positionMapper.selectById(id);
        if (position == null) {
            throw new BusinessException("岗位不存在");
        }
        Department department = departmentMapper.selectById(position.getDeptId());
        String deptName = department != null ? department.getDeptName() : null;
        return toVO(position,deptName);
    }

    @Override
    public void insert(PositionCreateDTO dto) {
        Department department = departmentMapper.selectById(dto.getDeptId());
        if (department == null) {
            throw new BusinessException("部门不存在");
        }
        Long count=positionMapper.selectCount(new LambdaQueryWrapper<Position>().eq(Position::getPositionCode,dto.getPositionCode()));
        if (count>0){
            throw new BusinessException("岗位编码以存在");
        }
        positionMapper.insert(toEntity(dto));
    }

    @Override
    public void update(Long id, PositionUpdateDTO dto) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        Department department = departmentMapper.selectById(dto.getDeptId());
        if (department == null) {
            throw new BusinessException("部门不存在");
        }
        Position position = positionMapper.selectById(id);
        if (position == null) {
            throw new BusinessException("岗位不存在");
        }
        position.setPositionName(dto.getPositionName());
        position.setDeptId(dto.getDeptId());
        if(dto.getLevel()!=null){
            position.setLevel(dto.getLevel());
        }
        if(dto.getStatus()!=null){
            position.setStatus(dto.getStatus());
        }
        positionMapper.updateById(position);
    }

    @Override
    public void delete(Long id) {
        Position position = positionMapper.selectById(id);
        if (position == null) {
            throw new BusinessException("岗位不存在");
        }
        Long count = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>().eq(Employee::getPositionId, id)
        );
        if (count > 0) {
            throw new BusinessException("该岗位下存在员工，无法删除");
        }
        positionMapper.deleteById(id);
    }

    private PositionVO toVO(Position position, String deptName) {
        PositionVO vo = new PositionVO();
        vo.setId(position.getId());
        vo.setPositionName(position.getPositionName());
        vo.setPositionCode(position.getPositionCode());
        vo.setDeptId(position.getDeptId());
        vo.setDeptName(deptName);
        vo.setLevel(position.getLevel());
        vo.setStatus(position.getStatus());
        return vo;
    }

    private Position toEntity(PositionCreateDTO dto) {
        Position position = new Position();
        position.setPositionName(dto.getPositionName());
        position.setPositionCode(dto.getPositionCode());
        position.setDeptId(dto.getDeptId());
        position.setLevel(dto.getLevel() != null ? dto.getLevel() : 1);
        position.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return position;
    }
}
