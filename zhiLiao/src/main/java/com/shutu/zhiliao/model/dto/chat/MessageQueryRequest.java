package com.shutu.zhiliao.model.dto.chat;

import com.shutu.zhiliao.util.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MessageQueryRequest extends PageRequest {
    /**
     * 房间 ID
     */
    private Long roomId;
}
