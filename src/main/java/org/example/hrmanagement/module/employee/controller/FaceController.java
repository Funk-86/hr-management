package org.example.hrmanagement.module.employee.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.employee.dto.FaceEnrollDTO;
import org.example.hrmanagement.module.employee.service.FaceService;
import org.example.hrmanagement.module.employee.vo.FaceStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "人脸识别")
@RestController
@RequestMapping("/people")
public class FaceController {
    @Autowired
    private FaceService faceService;


    @PostMapping("/{employeeId}/face/enroll")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    public Result<Void> enroll(
            @PathVariable Long employeeId,
            @Valid @RequestBody FaceEnrollDTO dto) {
        faceService.enroll(employeeId, dto);
        return Result.success();
    }

    @GetMapping("/{employeeId}/face/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    public Result<FaceStatusVO> getFaceStatus(@PathVariable Long employeeId) {
        return Result.success(faceService.getStatus(employeeId));
    }
}
