package com.shutu.shutuapi.feign;

import com.shutu.commons.tools.constant.ServiceConstant;
import com.shutu.commons.tools.utils.Result;
import com.shutu.shutuapi.dto.SysUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ServiceConstant.SHUTU_AUTH_SERVER ,contextId = "SysUserClient")
public interface SysUserClient {
    /**
     * 根据id用户信息
     */
    @GetMapping("/auth/user/{id}")
    Result<SysUserDTO> get(@PathVariable("id") Long id);

    /**
     * 更新用户信息
     */
    @PutMapping("/auth/user")
    Result update(@RequestBody SysUserDTO dto);
}
