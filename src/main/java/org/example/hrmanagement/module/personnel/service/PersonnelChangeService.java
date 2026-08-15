package org.example.hrmanagement.module.personnel.service;

import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.personnel.dto.PersonnelApproveDTO;
import org.example.hrmanagement.module.personnel.dto.PersonnelChangeCreateDTO;
import org.example.hrmanagement.module.personnel.dto.PersonnelChangeQueryDTO;
import org.example.hrmanagement.module.personnel.vo.PersonnelChangeVO;

public interface PersonnelChangeService {

    PageResult<PersonnelChangeVO> page(PersonnelChangeQueryDTO query);

    PersonnelChangeVO getDetail(Long id);

    Long create(PersonnelChangeCreateDTO dto);

    void approve(Long id, PersonnelApproveDTO dto);

    void cancel(Long id);

    /** 审批通过后生效：回写员工档案 */
    void effect(Long id);
}
