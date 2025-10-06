package com.shutu.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shutu.dao.SysUserFriendDao;
import com.shutu.domain.entity.SysUserEntity;
import com.shutu.domain.entity.SysUserFriendEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.shutu.service.SysUserFriendService;

/**
 * 好友管理
 */
@Service
@RequiredArgsConstructor
public class SysUserFriendServiceImpl extends ServiceImpl<SysUserFriendDao, SysUserFriendEntity> implements SysUserFriendService {


}