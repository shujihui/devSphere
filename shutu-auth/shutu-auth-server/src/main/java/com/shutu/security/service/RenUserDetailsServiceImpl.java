package com.shutu.security.service;

import com.shutu.service.SysUserDetailService;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.shutu.commons.security.enums.UserStatusEnum;
import com.shutu.commons.security.user.UserDetail;
import com.shutu.commons.tools.exception.ErrorCode;


/**
 * UserDetailsService
 *
 * @author Mark sunlightcs@gmail.com
 */
@Service
public class RenUserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private SysUserDetailService sysUserDetailService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetail userDetail = sysUserDetailService.getUserDetailByUsername(username);
        if (userDetail == null) {
            try {
                throw new Exception(String.valueOf(ErrorCode.ACCOUNT_NOT_EXIST));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 账号不可用
        if (userDetail.getStatus() == UserStatusEnum.DISABLE.value()) {
            userDetail.setEnabled(false);
        }

        return userDetail;
    }
}
