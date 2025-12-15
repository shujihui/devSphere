import { useCallStore, type CallType } from '../stores/callStore'
import { useChatStore } from '../stores/chatStore'
import { useUserStore } from '../stores/userStore'
import wsService, { WSReqType } from './WebSocketService'
import { shallowRef } from 'vue'

// STUN 服务器配置
const rtcConfig = {
    iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },
        { urls: 'stun:stun2.l.google.com:19302' },
    ]
}

class WebRTCService {
    // 多对等连接支持: Map<用户ID, RTCPeerConnection>
    private peerConnections: Map<string, RTCPeerConnection> = new Map()

    // 本地媒体流(所有连接共享)
    private localMediaStream: MediaStream | null = null
    public localStream = shallowRef<MediaStream | null>(null)

    // 远程媒体流: Map<用户ID, MediaStream>
    public remoteStreams = shallowRef<Map<string, MediaStream>>(new Map())

    // 单个远程流(兼容 P2P UI)
    public remoteStream = shallowRef<MediaStream | null>(null)

    private targetId: string = ''

    // 待处理的 offer 和 ICE 候选队列
    private pendingOffers: Map<string, RTCSessionDescriptionInit> = new Map()
    private candidateQueues: Map<string, RTCIceCandidateInit[]> = new Map()

    constructor() {
        wsService.registerRtcHandler(this.handleSignal.bind(this))
    }

    // 发起通话
    async startCall(targetId: string, targetUserInfo: any, type: CallType = 'audio', mode: 'p2p' | 'group' = 'p2p') {
        const callStore = useCallStore()
        const chatStore = useChatStore()

        if (callStore.callState !== 'idle') {
            console.warn('[WebRTC] Call already in progress')
            return
        }

        if (wsService.status.value !== 1) {
            wsService.ensureConnected()
            alert('网络连接未就绪，正在尝试重连，请稍后再试')
            return
        }

        this.targetId = targetId
        callStore.startCall(targetUserInfo, type, mode)

        if (mode === 'p2p') {
            // P2P 模式
            await this.initLocalMedia()

            const messageType = type === 'video' ? 6 : 5
            const content = `发起${type === 'video' ? '视频' : '语音'}通话`
            chatStore.sendCallMessage(targetId, content, messageType, 'p2p')

            await this.createPeerConnection(targetId)

            const pc = this.peerConnections.get(targetId)
            if (pc) {
                const offer = await pc.createOffer()
                await pc.setLocalDescription(offer)
                this.sendSignal('offer', offer, targetId)
            }
        } else {
            // 群聊模式
            console.log('[WebRTC] 发送群聊通话邀请')

            const content = JSON.stringify({
                type: 'GROUP_CALL_INVITE',
                callType: type,
                groupId: targetId,
                groupName: targetUserInfo.name
            })

            chatStore.sendCallMessage(targetId, content, 7, 'group')

            await this.initLocalMedia()
            callStore.connectCall()

            const userStore = useUserStore()
            callStore.activeGroupCall = {
                groupId: targetId,
                hostId: String(userStore.userInfo?.id),
                callType: type
            }
        }
    }

    // 邀请用户加入通话(群聊模式)
    async inviteUsers(users: any[]) {
        const callStore = useCallStore()
        if (callStore.callMode !== 'group') return

        for (const user of users) {
            if (this.peerConnections.has(user.id)) continue

            callStore.addParticipant(user)

            await this.createPeerConnection(user.id)

            const pc = this.peerConnections.get(user.id)
            if (pc) {
                const offer = await pc.createOffer()
                await pc.setLocalDescription(offer)
                this.sendSignal('offer', offer, user.id)
            }
        }
    }

    // 加入群聊通话
    async joinGroupCall(groupId: string, hostId: string, callType: CallType) {
        const callStore = useCallStore()

        if (callStore.callState !== 'idle') {
            console.warn('[WebRTC] 已在通话中,无法加入')
            return
        }

        console.log('[WebRTC] 加入群聊通话:', { groupId, hostId, callType })

        this.targetId = groupId
        callStore.joinGroupCall(groupId, hostId, callType)

        await this.initLocalMedia()
        await this.createPeerConnection(hostId)

        const pc = this.peerConnections.get(hostId)
        if (pc) {
            const offer = await pc.createOffer()
            await pc.setLocalDescription(offer)
            this.sendGroupJoinSignal('offer', offer, hostId, groupId)
        }

        callStore.connectCall()
    }

    // 发送加入群聊信令
    private sendGroupJoinSignal(type: string, payload: any, targetId: string, groupId: string) {
        const callStore = useCallStore()
        const userStr = localStorage.getItem('userInfo')
        let rawUserInfo: any = {}
        try { rawUserInfo = JSON.parse(userStr || '{}') } catch (e) { }

        const senderUserInfo = {
            id: rawUserInfo.id || rawUserInfo.uid,
            name: rawUserInfo.realName || rawUserInfo.username || 'Unknown',
            avatar: rawUserInfo.headUrl || rawUserInfo.avatar || 'default',
            callType: callStore.callType,
            callMode: 'group',
            groupId: groupId,
            action: 'JOIN_GROUP_CALL'
        }

        wsService.send({
            type: WSReqType.RTC_SIGNAL,
            userId: targetId,
            data: JSON.stringify({
                type,
                payload,
                senderId: senderUserInfo.id,
                senderUserInfo
            })
        })
    }

    // 处理来电
    async handleIncomingCall(senderId: string, senderUserInfo: any, offer: RTCSessionDescriptionInit, mode: 'p2p' | 'group' = 'p2p') {
        const callStore = useCallStore()

        if (callStore.callState !== 'idle') {
            this.sendSignal('busy', null, senderId)
            return
        }

        this.targetId = senderUserInfo.groupId || senderId
        const type: CallType = senderUserInfo.callType || 'audio'

        callStore.incomingCall(senderUserInfo, type, mode)
        this.pendingOffers.set(senderId, offer)
    }

    // 接听通话
    async acceptCall() {
        const callStore = useCallStore()
        let targetId = this.targetId

        if (callStore.callMode === 'group') {
            const inviterId = this.pendingOffers.keys().next().value
            if (inviterId) {
                targetId = inviterId
            }
        }

        const offer = this.pendingOffers.get(targetId)
        if (!offer) {
            console.error('[WebRTC] No pending offer to accept from', targetId)
            return
        }

        await this.initLocalMedia()
        await this.createPeerConnection(targetId)
        const pc = this.peerConnections.get(targetId)

        if (pc) {
            await pc.setRemoteDescription(new RTCSessionDescription(offer))
            this.pendingOffers.delete(targetId)

            // 处理队列中的 ICE 候选
            const queue = this.candidateQueues.get(targetId) || []
            while (queue.length > 0) {
                const candidate = queue.shift()
                if (candidate) {
                    await pc.addIceCandidate(new RTCIceCandidate(candidate))
                }
            }

            const answer = await pc.createAnswer()
            await pc.setLocalDescription(answer)
            this.sendSignal('answer', answer, targetId)

            callStore.connectCall()
        }
    }

    // 拒绝通话
    rejectCall() {
        const callStore = useCallStore()

        // 立即更新 UI 状态
        callStore.reset()

        // 异步处理网络通知和清理
        setTimeout(() => {
            this.sendSignal('reject', null, this.targetId)
            this.cleanup()
        }, 0)
    }

    // 挂断通话
    hangup() {
        const callStore = useCallStore()

        // 立即更新 UI 状态
        callStore.endCall()

        // 异步处理清理工作
        setTimeout(() => {
            this.peerConnections.forEach((_, peerId) => {
                this.sendSignal('hangup', null, peerId)
            })
            this.cleanup()
        }, 0)
    }

    // 处理信令消息
    async handleSignal(data: any) {
        const callStore = useCallStore()
        const { type, payload, senderId, senderUserInfo } = data

        switch (type) {
            case 'offer':
                // 处理加入群聊请求
                if (senderUserInfo.action === 'JOIN_GROUP_CALL') {
                    console.log('[WebRTC] 收到成员加入群聊请求:', senderId)

                    callStore.addParticipant(senderUserInfo, 'connecting')

                    await this.createPeerConnection(senderId)
                    const pc = this.peerConnections.get(senderId)

                    if (pc) {
                        await pc.setRemoteDescription(new RTCSessionDescription(payload))
                        const answer = await pc.createAnswer()
                        await pc.setLocalDescription(answer)
                        this.sendSignal('answer', answer, senderId)
                    }

                    this.notifyOtherParticipants(senderId, senderUserInfo)
                    break
                }

                // 群聊中的新成员
                if (callStore.callState === 'connected' && callStore.callMode === 'group') {
                    console.log('[WebRTC] Received offer from new peer in group call:', senderId)

                    callStore.addParticipant(senderUserInfo, 'connected')

                    await this.createPeerConnection(senderId)
                    const pc = this.peerConnections.get(senderId)
                    if (pc) {
                        await pc.setRemoteDescription(new RTCSessionDescription(payload))
                        const answer = await pc.createAnswer()
                        await pc.setLocalDescription(answer)
                        this.sendSignal('answer', answer, senderId)
                    }
                } else {
                    // 普通来电
                    if (senderId && senderUserInfo) {
                        await this.handleIncomingCall(senderId, senderUserInfo, payload, senderUserInfo.callMode || 'p2p')
                    }
                }
                break;

            case 'answer':
                const pc = this.peerConnections.get(senderId)
                if (pc) {
                    await pc.setRemoteDescription(new RTCSessionDescription(payload))
                    if (senderId === this.targetId && callStore.callState === 'calling') {
                        callStore.connectCall()
                    }
                    callStore.updateParticipantStatus(senderId, 'connected')
                }
                break

            case 'candidate':
                if (payload) {
                    const pc = this.peerConnections.get(senderId)
                    if (pc && pc.remoteDescription) {
                        await pc.addIceCandidate(new RTCIceCandidate(payload))
                    } else {
                        // 加入队列等待处理
                        if (!this.candidateQueues.has(senderId)) {
                            this.candidateQueues.set(senderId, [])
                        }
                        this.candidateQueues.get(senderId)?.push(payload)
                    }
                }
                break

            case 'hangup':
                if (callStore.callMode === 'p2p') {
                    this.cleanup()
                    callStore.endCall()
                } else {
                    this.closePeerConnection(senderId)
                    callStore.removeParticipant(senderId)
                    if (this.peerConnections.size === 0) {
                        this.cleanup()
                        callStore.endCall()
                    }
                }
                break

            case 'busy':
            case 'reject':
                if (callStore.callMode === 'p2p') {
                    alert('对方忙或拒绝')
                    this.cleanup()
                    callStore.reset()
                } else {
                    callStore.removeParticipant(senderId)
                }
                break

            case 'new_member':
                // 收到新成员加入通知
                const { newMemberId, newMemberInfo } = data
                console.log('[WebRTC] 收到新成员通知,建立连接:', newMemberId)

                callStore.addParticipant(newMemberInfo, 'connecting')

                await this.createPeerConnection(newMemberId)
                const newPc = this.peerConnections.get(newMemberId)
                if (newPc) {
                    const offer = await newPc.createOffer()
                    await newPc.setLocalDescription(offer)
                    this.sendSignal('offer', offer, newMemberId)
                }
                break
        }
    }

    // 初始化本地媒体流
    private async initLocalMedia() {
        const callStore = useCallStore()
        if (this.localMediaStream) return

        const constraints = {
            audio: true,
            video: callStore.callType === 'video' ? {
                facingMode: 'user',
                width: { ideal: 640 },
                height: { ideal: 480 }
            } : false
        }

        try {
            this.localMediaStream = await navigator.mediaDevices.getUserMedia(constraints)
            this.localStream.value = this.localMediaStream

            if (callStore.callType === 'video') {
                callStore.isCameraOn = true
            }
            this.updateMediaState()
        } catch (err: any) {
            console.error('Failed to get user media', err)
            throw err
        }
    }

    // 创建对等连接
    private async createPeerConnection(targetId: string) {
        if (this.peerConnections.has(targetId)) return

        console.log('[WebRTC] Creating PC for', targetId)
        const pc = new RTCPeerConnection(rtcConfig)

        // 添加本地轨道
        if (this.localMediaStream) {
            this.localMediaStream.getTracks().forEach(track => {
                pc.addTrack(track, this.localMediaStream!)
            })
        }

        // ICE 候选处理
        pc.onicecandidate = (event) => {
            if (event.candidate) {
                this.sendSignal('candidate', event.candidate, targetId)
            }
        }

        // 连接状态监听
        pc.onconnectionstatechange = () => {
            console.log(`[WebRTC] Connection state with ${targetId}:`, pc.connectionState)
        }

        // 远程轨道处理
        pc.ontrack = (event) => {
            console.log(`[WebRTC] Received remote track from ${targetId}`, event.streams[0])
            const newMap = new Map(this.remoteStreams.value)
            newMap.set(targetId, event.streams[0])
            this.remoteStreams.value = newMap

            if (!this.remoteStream.value) {
                this.remoteStream.value = event.streams[0] || null
            }
        }

        this.peerConnections.set(targetId, pc)
    }

    // 关闭对等连接
    private closePeerConnection(targetId: string) {
        const pc = this.peerConnections.get(targetId)
        if (pc) {
            pc.close()
            this.peerConnections.delete(targetId)
        }

        const newMap = new Map(this.remoteStreams.value)
        newMap.delete(targetId)
        this.remoteStreams.value = newMap

        if (this.remoteStream.value && !newMap.size) {
            this.remoteStream.value = null
        }
    }

    // 通知其他参与者有新成员加入
    private notifyOtherParticipants(newMemberId: string, newMemberInfo: any) {
        const callStore = useCallStore()

        callStore.participants.forEach((participant, participantId) => {
            if (participantId !== newMemberId && participant.status === 'connected') {
                console.log(`[WebRTC] 通知 ${participantId} 与新成员 ${newMemberId} 建立连接`)

                wsService.send({
                    type: WSReqType.RTC_SIGNAL,
                    userId: participantId,
                    data: JSON.stringify({
                        type: 'new_member',
                        newMemberId: newMemberId,
                        newMemberInfo: newMemberInfo
                    })
                })
            }
        })
    }

    // 发送信令
    private sendSignal(type: string, payload: any, targetId: string) {
        const callStore = useCallStore()

        const userStr = localStorage.getItem('userInfo')
        let rawUserInfo: any = {}
        try { rawUserInfo = JSON.parse(userStr || '{}') } catch (e) { }

        const senderUserInfo = {
            id: rawUserInfo.id || rawUserInfo.uid,
            name: rawUserInfo.realName || rawUserInfo.username || 'Unknown',
            avatar: rawUserInfo.headUrl || rawUserInfo.avatar || 'default',
            callType: callStore.callType,
            callMode: callStore.callMode,
            groupId: callStore.callMode === 'group' ? this.targetId : undefined
        }

        wsService.send({
            type: WSReqType.RTC_SIGNAL,
            userId: targetId,
            data: JSON.stringify({
                type,
                payload,
                senderId: senderUserInfo.id,
                senderUserInfo
            })
        })
    }

    // 更新媒体状态
    updateMediaState() {
        const callStore = useCallStore()
        if (this.localMediaStream) {
            this.localMediaStream.getAudioTracks().forEach(t => t.enabled = !callStore.isMuted)
            this.localMediaStream.getVideoTracks().forEach(t => t.enabled = callStore.isCameraOn)
        }
    }

    // 切换静音
    toggleMute() {
        const callStore = useCallStore()
        callStore.isMuted = !callStore.isMuted
        this.updateMediaState()
    }

    // 切换摄像头开关
    toggleVideo() {
        const callStore = useCallStore()
        callStore.isCameraOn = !callStore.isCameraOn
        this.updateMediaState()
    }

    // 切换前后摄像头
    async switchCamera() {
        const callStore = useCallStore()
        if (callStore.callType !== 'video' || !this.localMediaStream) return

        this.localMediaStream.getVideoTracks().forEach(t => t.stop())
        callStore.isFrontCamera = !callStore.isFrontCamera

        try {
            const newStream = await navigator.mediaDevices.getUserMedia({
                audio: true,
                video: { facingMode: callStore.isFrontCamera ? 'user' : 'environment' }
            })

            const videoTrack = newStream.getVideoTracks()[0]
            this.localMediaStream = newStream
            this.localStream.value = newStream

            // 替换所有连接中的视频轨道
            this.peerConnections.forEach(pc => {
                const sender = pc.getSenders().find(s => s.track?.kind === 'video')
                if (sender && videoTrack) sender.replaceTrack(videoTrack)
            })

            this.updateMediaState()
        } catch (e) {
            console.error('Switch camera failed', e)
        }
    }

    public connectionState = shallowRef<string>('connected')

    // 清理资源
    private cleanup() {
        if (this.localMediaStream) {
            this.localMediaStream.getTracks().forEach(t => t.stop())
            this.localMediaStream = null
        }
        this.peerConnections.forEach(pc => pc.close())
        this.peerConnections.clear()

        this.remoteStreams.value = new Map()
        this.remoteStream.value = null
        this.localStream.value = null
        this.pendingOffers.clear()
        this.candidateQueues.clear()
    }
}

export default new WebRTCService()
