package com.shutu.service.Impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shutu.commons.security.user.SecurityUser;
import com.shutu.commons.security.user.UserDetail;
import com.shutu.commons.tools.utils.ConvertUtils;
import com.shutu.commons.tools.utils.Result;
import com.shutu.dao.SysUserDao;
import com.shutu.dao.SysUserFriendDao;
import com.shutu.domain.entity.SysUserEntity;
import com.shutu.domain.entity.SysUserFriendEntity;
import com.shutu.redis.SysMenuRedis;
import com.shutu.service.SysMenuService;
import com.shutu.service.SysRoleDataScopeService;
import com.shutu.service.SysUserDetailService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * UserDetail Service
 */
@Service
public class SysUserDetailServiceImpl implements SysUserDetailService {
    @Resource
    private SysUserDao sysUserDao;
    @Resource
    private SysMenuRedis sysMenuRedis;
    @Resource
    private SysMenuService sysMenuService;
    @Resource
    private SysRoleDataScopeService sysRoleDataScopeService;

    @Resource
    private SysUserFriendDao sysUserFriendDao;

    @Override
    public UserDetail getUserDetailById(Long id) {
        SysUserEntity user = sysUserDao.getById(id);

        UserDetail userDetail = ConvertUtils.sourceToTarget(user, UserDetail.class);
        initUserData(userDetail);

        return userDetail;
    }

    @Override
    public UserDetail getUserDetailByUsername(String username) {
        SysUserEntity user = sysUserDao.getByUsername(username);

        UserDetail userDetail = ConvertUtils.sourceToTarget(user, UserDetail.class);
        initUserData(userDetail);

        return userDetail;
    }


    /**
     * 初始化用户数据
     */
    private void initUserData(UserDetail userDetail) {
        if (userDetail == null) {
            return;
        }

        //清空当前用户，菜单导航、权限标识
        sysMenuRedis.delete(userDetail.getId());

        //用户部门数据权限
        List<Long> deptIdList = sysRoleDataScopeService.getDataScopeList(userDetail.getId());
        userDetail.setDeptIdList(deptIdList);

        //获取用户权限标识
        Set<String> authorities = sysMenuService.getUserPermissions(userDetail);
        userDetail.setAuthoritySet(authorities);
    }



    /**
     * 添加好友
     * @return
     */
    @Override
    @Transactional
    public Result<Boolean> addFriend(String userName) {
        //查询
        LambdaQueryWrapper<SysUserEntity> lam = new LambdaQueryWrapper<>();
        lam.like(SysUserEntity::getUsername,userName);
        SysUserEntity sysUserEntity = sysUserDao.selectOne(lam);
        //互相添加好友
        Long userId = SecurityUser.getUserId();
        SysUserFriendEntity sysUserFriendEntity = new SysUserFriendEntity();
        sysUserFriendEntity.setFriendId(sysUserEntity.getId());
        sysUserFriendEntity.setUserId(userId);
        int insert = sysUserFriendDao.insert(sysUserFriendEntity);

        if (insert == 0){
            return new Result<Boolean>().error("好友添加失败!");
        }
        return new Result<Boolean>().ok(true);
    }

    /**
     * 获得我的好友信息
     * @return
     */
    @Override
    public Result<List<SysUserEntity>> getMyFriend() {
        //查询获取当前用户的所有信息
        Long userId = SecurityUser.getUserId();
        //获取所有的朋友id
        List<Long> friendIds = sysUserFriendDao.getFriendId(userId);
        //获取所有的朋友的信息
        LambdaQueryWrapper<SysUserEntity> lam1 = new LambdaQueryWrapper<>();
        lam1.in(SysUserEntity::getId, friendIds);
        List<SysUserEntity> sysUserEntities = sysUserDao.selectList(lam1);

        return new Result<List<SysUserEntity>>().ok(sysUserEntities);
    }

    @Override
    public void updateUserAddressById(Long id, String address) {

    }
}
