package com.shutu.service.Impl;

import com.shutu.dao.SysUserDao;
import com.shutu.domain.entity.SysUserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserDao sysUserDao;

    // 配置用数据库中的用户登录Security
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询自己数据库的用户信息
        SysUserEntity user = sysUserDao.selectByUsername(username);
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles()
                .build();
    }

}
