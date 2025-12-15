package com.shutu.websocket.service;

import com.shutu.model.vo.ws.request.WSBaseReq;
import com.shutu.model.vo.ws.response.WSBaseResp;
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

    /**
     * 推送消息给指定用户 (自动路由)
     */
    void sendToUid(WSBaseResp<?> wsBaseResp, Long uid);

    /**
     * 推送消息给本机用户 (仅限本机)
     * 用于 RouteMessageListener 回调
     */
    void sendToLocalUid(WSBaseResp<?> wsBaseResp, Long uid);

    void sendMessage(Channel channel, WSBaseReq req);

    /**
     * 处理 RTC 信令
     * 
     * @param channel
     * @param req
     */
    void handleRtcSignal(Channel channel, WSBaseReq req);

    /**
     * 心跳检测
     * 
     * @param channel
     */
    void heartbeat(Channel channel);
}
