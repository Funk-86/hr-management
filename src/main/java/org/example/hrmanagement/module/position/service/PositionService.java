package org.example.hrmanagement.module.position.service;

import org.example.hrmanagement.module.position.dto.PositionCreateDTO;
import org.example.hrmanagement.module.position.dto.PositionUpdateDTO;
import org.example.hrmanagement.module.position.vo.PositionVO;

import java.util.List;

public interface PositionService {
    List<PositionVO> listByDeptId(Long deptId);

    PositionVO getById(Long id);

    void insert(PositionCreateDTO dto);

    void update(Long id, PositionUpdateDTO dto);

    void delete(Long id);
}
