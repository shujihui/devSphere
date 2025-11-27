import { defineStore } from 'pinia'
import { ref } from 'vue'

export type CallState = 'idle' | 'calling' | 'incoming' | 'connected' | 'ended'
export type CallType = 'audio' | 'video'

export interface RemoteUserInfo {
    id: string
    name: string
    avatar: string
    groupId?: string
}

export const useCallStore = defineStore('call', () => {
    const callState = ref<CallState>('idle')
    const callType = ref<CallType>('audio')
    const remoteUserInfo = ref<RemoteUserInfo | null>(null)
    const startTime = ref<Date | null>(null)
    const isMuted = ref(false)
    const isCameraOn = ref(true)
    const isFrontCamera = ref(true)
    const isSpeakerOn = ref(false) // For UI toggle state (though browser handles output)

    // Group Call State
    const callMode = ref<'p2p' | 'group'>('p2p')
    const participants = ref<Map<string, RemoteUserInfo & { status: 'connecting' | 'connected' | 'audio-only' }>>(new Map())

    let callingTimeout: any = null

    // Actions
    function startCall(userInfo: RemoteUserInfo, type: CallType = 'audio', mode: 'p2p' | 'group' = 'p2p') {
        callState.value = 'calling'
        callType.value = type
        callMode.value = mode
        remoteUserInfo.value = userInfo
        isMuted.value = false
        isCameraOn.value = type === 'video'
        isSpeakerOn.value = type === 'video' // Default to speaker for video calls

        // Initialize participants
        participants.value.clear()
        if (mode === 'group') {
            // In group mode, DO NOT add the group itself as a participant
            // addParticipant(userInfo) 
        }

        // 60s timeout for no answer
        if (callingTimeout) clearTimeout(callingTimeout)
        callingTimeout = setTimeout(() => {
            if (callState.value === 'calling') {
                // Timeout - cancel call
                endCall()
                // Ideally we should send a "cancel" signal here too, but for now just end local state
                // In a real app, WebRTCService should handle the signaling part of "cancel"
            }
        }, 60000)
    }

    function incomingCall(userInfo: RemoteUserInfo, type: CallType = 'audio', mode: 'p2p' | 'group' = 'p2p') {
        callState.value = 'incoming'
        callType.value = type
        callMode.value = mode
        remoteUserInfo.value = userInfo
        isMuted.value = false
        isCameraOn.value = type === 'video'
        isSpeakerOn.value = type === 'video'

        participants.value.clear()
        if (mode === 'group') {
            addParticipant(userInfo, 'connected') // The caller is already "connected" in terms of presence
        }
    }

    function addParticipant(user: RemoteUserInfo, status: 'connecting' | 'connected' | 'audio-only' = 'connecting') {
        participants.value.set(user.id, { ...user, status })
    }

    function updateParticipantStatus(userId: string, status: 'connecting' | 'connected' | 'audio-only') {
        const p = participants.value.get(userId)
        if (p) {
            p.status = status
            participants.value.set(userId, p)
        }
    }

    function removeParticipant(userId: string) {
        participants.value.delete(userId)
    }

    function connectCall() {
        if (callingTimeout) clearTimeout(callingTimeout)
        callState.value = 'connected'
        startTime.value = new Date()
    }

    function endCall() {
        if (callingTimeout) clearTimeout(callingTimeout)
        callState.value = 'ended'
        startTime.value = null
        // Don't nullify remoteUserInfo immediately so we can show "Ended" screen with avatar
        setTimeout(() => {
            if (callState.value === 'ended') {
                reset()
            }
        }, 2000)
    }

    function reset() {
        if (callingTimeout) clearTimeout(callingTimeout)
        callState.value = 'idle'
        callType.value = 'audio'
        callMode.value = 'p2p'
        remoteUserInfo.value = null
        startTime.value = null
        isMuted.value = false
        isCameraOn.value = true
        isFrontCamera.value = true
        isSpeakerOn.value = false
        participants.value.clear()
    }

    return {
        callState,
        callType,
        callMode,
        remoteUserInfo,
        participants,
        startTime,
        isMuted,
        isCameraOn,
        isFrontCamera,
        isSpeakerOn,
        startCall,
        incomingCall,
        connectCall,
        endCall,
        reset,
        addParticipant,
        updateParticipantStatus,
        removeParticipant
    }
})
