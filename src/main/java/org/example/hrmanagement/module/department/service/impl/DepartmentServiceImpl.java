package org.example.hrmanagement.module.department.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.module.department.dto.DepartmentCreateDTO;
import org.example.hrmanagement.module.department.dto.DepartmentUpdateDTO;
import org.example.hrmanagement.module.department.entity.Department;
import org.example.hrmanagement.module.department.mapper.DepartmentMapper;
import org.example.hrmanagement.module.department.service.DepartmentService;
import org.example.hrmanagement.module.department.vo.DepartmentTreeVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentMapper departmentMapper;

    @Override
    @CacheEvict(value = "departmentTree", allEntries = true)
    public void createDepartment(DepartmentCreateDTO dto) {
        if(dto.getParentId()!=null&&dto.getParentId()!=0){
            Department department = departmentMapper.selectById(dto.getParentId());
            if(department==null){
                throw new BusinessException("父节点不存在");
            }
        }

        Long count=departmentMapper.selectCount(new LambdaQueryWrapper<Department>().eq(Department::getDeptCode,dto.getDeptCode()));
        if(count>0){
            throw new BusinessException("部门编号已存在");
        }

        departmentMapper.insert(toEntity(dto));
        log.info("部门创建成功: deptCode={}, deptName={}", dto.getDeptCode(), dto.getDeptName());
    }

    @Override
    @CacheEvict(value = "departmentTree", allEntries = true)
    public void updateDepartment(Long id,DepartmentUpdateDTO dto) {
        Department department = departmentMapper.selectById(id);
        if(department==null){
            throw new BusinessException("部门不存在");
        }

        department.setDeptName(dto.getDeptName());
        if (dto.getLeaderId() != null) {
            department.setLeaderId(dto.getLeaderId());
        }
        if (dto.getSortOrder() != null) {
            department.setSortOrder(dto.getSortOrder());
        }
        if (dto.getStatus() != null) {
            department.setStatus(dto.getStatus());
        }
        departmentMapper.updateById(department);
        log.info("部门更新成功: id={}, deptName={}", id, dto.getDeptName());
    }

    @Override
    @CacheEvict(value = "departmentTree", allEntries = true)
    public void deleteDepartment(Long id) {
        Department department = departmentMapper.selectById(id);
        if(department==null){
            throw new BusinessException("部门不存在");
        }

        Long count=departmentMapper.selectCount(new LambdaQueryWrapper<Department>().eq(Department::getParentId,id));
        if(count>0){
            throw new BusinessException("存在子部门，无法删除");
        }

        departmentMapper.deleteById(id);
        log.info("部门删除成功: id={}", id);
    }

    @Override
    @Cacheable(value = "departmentTree")
    public List<DepartmentTreeVO> listTree() {
        log.debug("从数据库加载部门树");
        List<Department> departments=departmentMapper.selectList(new LambdaQueryWrapper<Department>().orderByAsc(Department::getSortOrder));
        return buildTree(departments);
    }

    @Override
    public DepartmentTreeVO getById(Long id) {
        if(id==null){
            throw new BusinessException("id属性为空");
        }
        Department department=departmentMapper.selectById(id);
        if(department==null){
            throw new BusinessException("部门不存在");
        }
        return toTreeVO(department);
    }

    private DepartmentTreeVO toTreeVO(Department department){
        return getDepartmentTreeVO(department);
    }

    private DepartmentTreeVO getDepartmentTreeVO(Department department) {
        DepartmentTreeVO departmentTreeVO=new DepartmentTreeVO();
        departmentTreeVO.setId(department.getId());
        departmentTreeVO.setParentId(department.getParentId());
        departmentTreeVO.setDeptName(department.getDeptName());
        departmentTreeVO.setDeptCode(department.getDeptCode());
        departmentTreeVO.setSortOrder(department.getSortOrder());
        departmentTreeVO.setStatus(department.getStatus());
        departmentTreeVO.setChildren(new ArrayList<>());
        return departmentTreeVO;
    }

    private Department toEntity(DepartmentCreateDTO dto) {
        Department department = new Department();
        department.setParentId(dto.getParentId()!=null ? dto.getParentId() : 0L );
        department.setDeptCode(dto.getDeptCode());
        department.setDeptName(dto.getDeptName());
        department.setLeaderId(dto.getLeaderId());
        department.setSortOrder(dto.getSortOrder()!=null ? dto.getSortOrder() : 0);
        department.setStatus(dto.getStatus()!=null ? dto.getStatus() : 1);
        return department;
    }

    private List<DepartmentTreeVO> buildTree(List<Department> departments) {
        List<DepartmentTreeVO> voList= departments.stream().map(dept -> {
            return getDepartmentTreeVO(dept);
        }).toList();

        Map<Long,DepartmentTreeVO> voMap=voList.stream().collect(Collectors.toMap(DepartmentTreeVO::getId,v->v));

        List<DepartmentTreeVO> tree=new ArrayList<>();
        for(Department department:departments){
            DepartmentTreeVO current=voMap.get(department.getId());
            if(department.getParentId()==null || department.getParentId()==0){
                tree.add(current);
            }else{
                DepartmentTreeVO parent=voMap.get(department.getParentId());
                if(parent!=null){
                    parent.getChildren().add(current);
                }
            }
        }
        return tree;
    }
}
