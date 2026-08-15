package org.example.hrmanagement.module.salary.service;

import org.example.hrmanagement.module.salary.dto.SalaryBaseDictSaveDTO;
import org.example.hrmanagement.module.salary.dto.TaskScoreBonusDictSaveDTO;
import org.example.hrmanagement.module.salary.vo.SalaryBaseDictVO;
import org.example.hrmanagement.module.salary.vo.TaskScoreBonusDictVO;

import java.util.List;

public interface SalaryDictService {

    List<SalaryBaseDictVO> listBaseDict();

    void saveBaseDict(SalaryBaseDictSaveDTO dto);

    void updateBaseDict(Long id, SalaryBaseDictSaveDTO dto);

    void deleteBaseDict(Long id);

    List<TaskScoreBonusDictVO> listScoreBonusDict();

    void saveScoreBonusDict(TaskScoreBonusDictSaveDTO dto);

    void updateScoreBonusDict(Long id, TaskScoreBonusDictSaveDTO dto);

    void deleteScoreBonusDict(Long id);
}
