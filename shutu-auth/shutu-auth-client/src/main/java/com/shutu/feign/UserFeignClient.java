/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.shutu.feign;

import com.shutu.commons.security.user.UserDetail;
import com.shutu.commons.tools.constant.ServiceConstant;
import com.shutu.commons.tools.utils.Result;
import com.shutu.dto.SysUserDTO;
import com.shutu.feign.fallback.UserFeignClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户接口
 */
@FeignClient(name = ServiceConstant.SHUTU_ADMIN_SERVER, contextId = "UserFeignClient", fallbackFactory = UserFeignClientFallbackFactory.class)
public interface UserFeignClient {

    /**
     * 根据用户ID，获取用户信息
     */
    @GetMapping("sys/user/getById")
    Result<UserDetail> getById(@RequestParam("id") Long id);

    /**
     * 根据用户ID，获取角色Id列表
     */
    @GetMapping("sys/role/getRoleIdList")
    Result<List<Long>> getRoleIdList(@RequestParam("userId") Long userId);

    /**
     * 根据角色ID,查询用户ID列表
     */
    @PostMapping("sys/user/getUserIdListByRoleIdList")
    Result<List<Long>> getUserIdListByRoleIdList(@RequestParam List<Long> ids);

    /**
     * 根据岗位ID,查询用户ID列表
     */
    @PostMapping("sys/user/getUserIdListByPostIdList")
    Result<List<Long>> getUserIdListByPostIdList(@RequestParam List<Long> ids);

    /**
     * 根据部门ID,查询部门领导列表
     */
    @PostMapping("sys/user/getLeaderIdListByDeptIdList")
    Result<List<Long>> getLeaderIdListByDeptIdList(@RequestParam List<Long> ids);

    /**
     * 根据用户ID,查询部门领导ID
     */
    @PostMapping("sys/user/getLeaderIdListByUserId")
    Result<Long> getLeaderIdListByUserId(@RequestParam("userId") Long userId);


    /**
     * 根据用户ID列表，批量查询用户信息
     */
    @PostMapping("listByIds")
    Result<List<SysUserDTO>> listByIds(@RequestBody List<Long> ids);


}