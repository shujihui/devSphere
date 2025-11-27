<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue'
import { useCallStore } from '../../stores/callStore'
import { useUserStore } from '../../stores/userStore'
import webRTCService from '../../services/WebRTCService'
import { formatImageUrl } from '../../utils/image'
import ContactPicker from './ContactPicker.vue'

const callStore = useCallStore()
const userStore = useUserStore()

const showInviteModal = ref(false)

const handleInviteUsers = (users: any[]) => {
  webRTCService.inviteUsers(users)
}

// Grid Layout Logic
const participants = computed(() => {
  const list = []
  // Add self
  list.push({
    id: 'self',
    name: userStore.userInfo?.username || 'Me',
    avatar: userStore.userAvatar || '', 
    isSelf: true,
    stream: webRTCService.localStream.value,
    status: 'connected'
  })
  
  // Add remotes
  callStore.participants.forEach((p, id) => {
    list.push({
      ...p,
      isSelf: false,
      stream: webRTCService.remoteStreams.value.get(id),
      status: p.status
    })
  })
  return list
})

const gridClass = computed(() => {
  const count = participants.value.length
  if (count <= 1) return 'grid-cols-1'
  if (count === 2) return 'grid-cols-1 sm:grid-cols-2'
  if (count <= 4) return 'grid-cols-2'
  if (count <= 6) return 'grid-cols-2 sm:grid-cols-3'
  return 'grid-cols-3'
})

// Duration Timer
const duration = ref('00:00')
let timerInterval: any = null

watch(() => callStore.startTime, (newTime) => {
  if (newTime) {
    timerInterval = setInterval(() => {
      const now = new Date()
      const diff = Math.floor((now.getTime() - newTime.getTime()) / 1000)
      const mins = Math.floor(diff / 60).toString().padStart(2, '0')
      const secs = (diff % 60).toString().padStart(2, '0')
      duration.value = `${mins}:${secs}`
    }, 1000)
  } else {
    if (timerInterval) clearInterval(timerInterval)
    duration.value = '00:00'
  }
})

// Actions
const handleAccept = () => webRTCService.acceptCall()
const handleReject = () => webRTCService.rejectCall()
const handleHangup = () => webRTCService.hangup()
const toggleMute = () => webRTCService.toggleMute()
const toggleVideo = () => webRTCService.toggleVideo()
const switchCamera = () => webRTCService.switchCamera()

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="callStore.callState !== 'idle' && callStore.callMode === 'group'" class="fixed inset-0 z-[9999] bg-slate-900 flex flex-col">
        
        <!-- Header -->
        <div class="absolute top-0 left-0 right-0 p-4 flex justify-between items-center z-20 bg-gradient-to-b from-black/50 to-transparent">
          <div class="flex items-center gap-2">
            <div class="bg-emerald-500/20 text-emerald-400 px-3 py-1 rounded-full text-sm font-medium backdrop-blur-md border border-emerald-500/20 flex items-center gap-2">
              <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              {{ duration }}
            </div>
          </div>
          <div class="text-white/80 text-sm font-medium">
            {{ participants.length }} 人正在通话
          </div>
        </div>

        <!-- Incoming Call UI -->
        <div v-if="callStore.callState === 'incoming'" class="absolute inset-0 z-50 bg-slate-900/90 backdrop-blur-xl flex flex-col items-center justify-center">
            <div class="relative mb-12">
                <div class="w-32 h-32 rounded-full p-1 border-4 border-white/10 relative z-10">
                    <img :src="formatImageUrl(callStore.remoteUserInfo?.avatar)" class="w-full h-full rounded-full object-cover bg-slate-800 shadow-2xl" />
                </div>
                <div class="absolute inset-0 rounded-full border-4 border-blue-500/30 animate-ping"></div>
                <div class="absolute inset-0 rounded-full border-4 border-blue-500/20 animate-ping" style="animation-delay: 0.5s"></div>
            </div>
            
            <h2 class="text-2xl font-bold text-white mb-2">{{ callStore.remoteUserInfo?.name }}</h2>
            <p class="text-slate-400 mb-12">邀请你加入群组通话...</p>

            <div class="flex items-center gap-12">
                <!-- Reject -->
                <button @click="handleReject" class="flex flex-col items-center gap-3 group">
                    <div class="w-16 h-16 rounded-full bg-rose-500 hover:bg-rose-600 flex items-center justify-center text-white shadow-lg shadow-rose-500/30 transition-all group-active:scale-95">
                        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </div>
                    <span class="text-sm text-slate-300 font-medium">拒绝</span>
                </button>

                <!-- Accept -->
                <button @click="handleAccept" class="flex flex-col items-center gap-3 group">
                    <div class="w-16 h-16 rounded-full bg-emerald-500 hover:bg-emerald-600 flex items-center justify-center text-white shadow-lg shadow-emerald-500/30 transition-all group-active:scale-95 animate-bounce">
                        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
                    </div>
                    <span class="text-sm text-slate-300 font-medium">接听</span>
                </button>
            </div>
        </div>

        <!-- Grid Content -->
        <div class="flex-1 p-4 overflow-y-auto custom-scrollbar flex items-center justify-center">
          <div class="grid gap-4 w-full max-w-6xl transition-all duration-500" :class="gridClass">
            
            <div 
              v-for="p in participants" 
              :key="p.id"
              class="relative aspect-video bg-slate-800 rounded-2xl overflow-hidden border border-white/10 shadow-xl group"
            >
              <!-- Video Stream -->
              <video 
                v-if="p.stream && (p.isSelf ? callStore.isCameraOn : true)"
                :srcObject="p.stream"
                class="w-full h-full object-cover"
                :class="{ 'mirror': p.isSelf }"
                autoplay
                playsinline
                :muted="p.isSelf" 
              ></video>
              
              <!-- Avatar Fallback (Audio Only or Connecting) -->
              <div v-else class="absolute inset-0 flex flex-col items-center justify-center bg-slate-800">
                <div class="w-20 h-20 rounded-full p-1 border-2 border-white/20 mb-3 relative">
                   <img :src="formatImageUrl(p.avatar) || `https://api.dicebear.com/7.x/initials/svg?seed=${p.name}`" class="w-full h-full rounded-full object-cover bg-slate-700" />
                   <!-- Status Indicator -->
                   <div v-if="p.status === 'connecting'" class="absolute inset-0 rounded-full border-2 border-blue-500 border-t-transparent animate-spin"></div>
                </div>
                <span class="text-white font-medium">{{ p.name }}</span>
                <span class="text-xs text-white/50 mt-1">
                    {{ p.status === 'connecting' ? '连接中...' : '已关闭摄像头' }}
                </span>
              </div>

              <!-- Name Tag -->
              <div class="absolute bottom-3 left-3 bg-black/40 backdrop-blur-md px-2 py-1 rounded-lg text-xs text-white font-medium flex items-center gap-2">
                <span>{{ p.name }}</span>
                <svg v-if="!p.stream?.getAudioTracks()[0]?.enabled" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-red-400"><line x1="1" y1="1" x2="23" y2="23"/><path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6"/><path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a7 7 0 0 1-.11 1.23"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>
              </div>
            </div>

          </div>
        </div>

        <!-- Bottom Controls -->
        <div class="p-8 pb-10 flex justify-center items-center gap-6 z-20 bg-gradient-to-t from-black/80 via-black/40 to-transparent">
            
            <!-- Mute -->
            <button @click="toggleMute" class="control-btn" :class="callStore.isMuted ? 'bg-white text-slate-900' : 'bg-white/10 text-white hover:bg-white/20'">
                <svg v-if="!callStore.isMuted" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="1" y1="1" x2="23" y2="23"/><path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6"/><path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a7 7 0 0 1-.11 1.23"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>
            </button>

            <!-- Video -->
            <button @click="toggleVideo" class="control-btn" :class="!callStore.isCameraOn ? 'bg-white text-slate-900' : 'bg-white/10 text-white hover:bg-white/20'">
                <svg v-if="callStore.isCameraOn" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 7l-7 5 7 5V7z"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m16 13 5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5"/><rect x="2" y="6" width="14" height="12" rx="2"/><line x1="2" y1="2" x2="22" y2="22"/></svg>
            </button>

            <!-- Hangup -->
            <button @click="handleHangup" class="w-16 h-16 rounded-full bg-rose-500 hover:bg-rose-600 flex items-center justify-center text-white shadow-xl shadow-rose-500/30 transition-all active:scale-95">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.68 13.31a16 16 0 0 0 3.41 2.6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7 2 2 0 0 1 1.72 2v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.42 19.42 0 0 1-3.33-2.67m-2.67-3.34a19.79 19.79 0 0 1-3.07-8.63A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91"/><line x1="23" y1="1" x2="1" y2="23"/></svg>
            </button>

            <!-- Invite -->
            <button @click="showInviteModal = true" class="control-btn bg-white/10 text-white hover:bg-white/20">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
            </button>
        </div>

      </div>
    </Transition>
    
    <ContactPicker 
      :is-open="showInviteModal"
      :group-id="callStore.remoteUserInfo?.groupId || callStore.remoteUserInfo?.id || 0"
      :exclude-ids="participants.map(p => p.id)"
      @close="showInviteModal = false"
      @invite="handleInviteUsers"
    />
  </Teleport>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.control-btn {
  @apply w-12 h-12 rounded-full flex items-center justify-center transition-all duration-300 active:scale-95 shadow-lg backdrop-blur-md border border-white/10;
}

.mirror {
  transform: scaleX(-1);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
}
</style>
