package com.shutu.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shutu.domain.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色管理
 *
 * @author Mark sunlightcs@gmail.com
 * @since 1.0.0
 */
@Mapper
public interface SysRoleDao extends BaseMapper<SysRoleEntity> {
	
}
