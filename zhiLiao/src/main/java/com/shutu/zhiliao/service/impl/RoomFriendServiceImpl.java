package com.shutu.zhiliao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shutu.commons.security.user.SecurityUser;
import com.shutu.zhiliao.mapper.RoomFriendMapper;
import com.shutu.zhiliao.model.entity.RoomFriend;
import com.shutu.zhiliao.service.RoomFriendService;
import org.springframework.stereotype.Service;

/**
* @author 聪
* @description 针对表【room_friend(用户私聊表)】的数据库操作Service实现
* @createDate 2024-02-18 10:45:29
*/
@Service
public class RoomFriendServiceImpl extends ServiceImpl<RoomFriendMapper, RoomFriend>
    implements RoomFriendService {

    @Override
    public RoomFriend getRoomFriend(Long uid) {
        //查询是否为好友
        long loginUserId = SecurityUser.getUserId();
        //获取房间ID
        long uid1 = uid > loginUserId ? loginUserId : uid;
        long uid2 = uid > loginUserId ? uid : loginUserId;
        return this.getOne(new LambdaQueryWrapper<RoomFriend>().eq(RoomFriend::getUid1, uid1).eq(RoomFriend::getUid2, uid2));
    }
}




