package com.shutu.devSphere.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shutu.commons.security.user.SecurityUser;
import com.shutu.devSphere.model.entity.RoomGroup;
import com.shutu.devSphere.model.entity.UserRoomRelate;
import com.shutu.devSphere.model.enums.chat.FriendSearchTypeEnum;
import com.shutu.devSphere.model.enums.chat.FriendTargetTypeEnum;
import com.shutu.devSphere.model.vo.friend.AddFriendVo;
import com.shutu.devSphere.service.RoomGroupService;
import com.shutu.devSphere.service.UserRoomRelateService;
import com.shutu.devSphere.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 群组搜索策略
 */
@Component
@RequiredArgsConstructor
public class GroupSearchStrategy implements FriendSearchStrategy {
    
    private final RoomGroupService roomGroupService;
    private final UserRoomRelateService userRoomRelateService;
    
    @Override
    public boolean supports(String id) {
        return CommonUtils.isNumericExceptLastS(id);
    }
    
    @Override
    public AddFriendVo search(String id) {
        String roomId = id.substring(0, id.length() - 1);
        RoomGroup roomGroup = roomGroupService.getOne(
            new LambdaQueryWrapper<RoomGroup>().eq(RoomGroup::getRoomId, roomId)
        );
        if (roomGroup == null) {
            return null;
        }
        
        AddFriendVo addFriendVo = new AddFriendVo();
        addFriendVo.setAvatar(roomGroup.getAvatar());
        addFriendVo.setType(FriendSearchTypeEnum.GROUP.getType());
        addFriendVo.setName(roomGroup.getName());
        addFriendVo.setRoomId(roomGroup.getRoomId());
        
        UserRoomRelate userRoomRelate = userRoomRelateService.getOne(
            new LambdaQueryWrapper<UserRoomRelate>()
                .eq(UserRoomRelate::getUserId, SecurityUser.getUserId())
                .eq(UserRoomRelate::getRoomId, roomId)
        );
        
        if (userRoomRelate != null) {
            addFriendVo.setFriendTarget(FriendTargetTypeEnum.JOIN.getType());
        }
        return addFriendVo;
    }
}
