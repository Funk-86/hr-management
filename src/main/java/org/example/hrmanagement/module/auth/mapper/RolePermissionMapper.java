package org.example.hrmanagement.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.hrmanagement.module.auth.entity.RolePermission;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
