package org.example.hrmanagement.module.position.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.position.dto.PositionCreateDTO;
import org.example.hrmanagement.module.position.dto.PositionUpdateDTO;
import org.example.hrmanagement.module.position.service.PositionService;
import org.example.hrmanagement.module.position.vo.PositionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "岗位管理")
@RestController
@RequestMapping("/positions")
public class PositionController {
    @Autowired
    private PositionService positionService;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @GetMapping
    public Result<List<PositionVO>> listByDeptId(@RequestParam Long deptId) {
        List<PositionVO> list = positionService.listByDeptId(deptId);
        return Result.success(list);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER')")
    @GetMapping("/{id}")
    public Result<PositionVO> getById(@PathVariable Long id) {
        return Result.success(positionService.getById(id));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PostMapping
    public Result<Void> insert(@Valid @RequestBody PositionCreateDTO dto) {
        positionService.insert(dto);
        return Result.success();
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,@Valid @RequestBody PositionUpdateDTO dto) {
        positionService.update(id,dto);
        return Result.success();
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return Result.success();
    }
}
