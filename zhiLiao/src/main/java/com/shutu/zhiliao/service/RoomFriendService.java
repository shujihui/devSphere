package com.shutu.zhiliao.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.shutu.zhiliao.model.entity.RoomFriend;

/**
* @author 聪
* @description 针对表【room_friend(用户私聊表)】的数据库操作Service
* @createDate 2024-02-18 10:45:29
*/
public interface RoomFriendService extends IService<RoomFriend> {

    /**
     * 获取房间好友
     *
     * @param uid 用户ID
     * @return {@link RoomFriend}
     */
    RoomFriend getRoomFriend(Long uid);

}
