package com.shutu.zhiliao.websocket.service;

import com.shutu.zhiliao.model.vo.ws.request.WSBaseRequest;
import com.shutu.zhiliao.model.vo.ws.response.WSBaseResp;
import io.netty.channel.Channel;
import org.springframework.stereotype.Service;

@Service
public interface WebSocketService {

    /**
     * 处理所有ws连接的事件
     *
     * @param channel 渠道
     */
    void connect(Channel channel);

    /**
     * 处理ws断开连接的事件
     *
     * @param channel 渠道
     */
    void removed(Channel channel);

    /**
     * 推动消息给所有在线的人
     *
     * @param wsBaseResp 发送的消息体
     * @param skipUid    需要跳过的人
     */
    void sendToAllOnline(WSBaseResp<?> wsBaseResp, Long skipUid);

    /**
     * 推动消息给所有在线的人
     *
     * @param wsBaseResp 发送的消息体
     */
    void sendToAllOnline(WSBaseResp<?> wsBaseResp);

    void sendToUid(WSBaseResp<?> wsBaseResp, Long uid);

    void sendMessage(Channel channel, WSBaseRequest req);

    void sendMessage(String token, WSBaseRequest req);

}



