package com.shutu.service;

import com.shutu.commons.security.user.UserDetail;
import com.shutu.commons.tools.utils.Result;
import com.shutu.domain.entity.SysUserEntity;

import java.util.List;
import java.util.Set;

/**
 * UserDetail Service
 *
 * @author Mark sunlightcs@gmail.com
 */
public interface SysUserDetailService {
    /**
     * 根据用户ID，获取用户详情
     */
    UserDetail getUserDetailById(Long id);

    /**
     * 根据用户名，获取用户详情
     */
    UserDetail getUserDetailByUsername(String username);

    Result<Boolean> addFriend(String userName);

    Result<List<SysUserEntity>> getMyFriend();

    /**
     * 根据用户id更新用户地址信息
     */
    void updateUserAddressById(Long id,String address);

}
