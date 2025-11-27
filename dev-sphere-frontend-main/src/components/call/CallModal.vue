<script setup lang="ts">
import { computed, ref, watch, onUnmounted, nextTick } from 'vue'
import { useCallStore } from '../../stores/callStore'
import webRTCService from '../../services/WebRTCService'
import { formatImageUrl } from '../../utils/image'

const callStore = useCallStore()
const remoteAudioRef = ref<HTMLAudioElement | null>(null)
const remoteVideoRef = ref<HTMLVideoElement | null>(null)
const localVideoRef = ref<HTMLVideoElement | null>(null)

// ... (keep existing code)

// Robust Video Binding
// Removed redundant watch blocks



// Duration Timer
const duration = ref('00:00')
let timerInterval: any = null

// Draggable Directive
const vDrag = {
  mounted(el: HTMLElement) {
    let isDragging = false
    let startX = 0
    let startY = 0
    let initialLeft = 0
    let initialTop = 0

    const onMouseDown = (e: MouseEvent) => {
      isDragging = true
      startX = e.clientX
      startY = e.clientY
      const rect = el.getBoundingClientRect()
      initialLeft = rect.left
      initialTop = rect.top
      el.style.cursor = 'grabbing'
      
      // Prevent selection
      e.preventDefault()
    }

    const onMouseMove = (e: MouseEvent) => {
      if (!isDragging) return
      const dx = e.clientX - startX
      const dy = e.clientY - startY
      
      // Boundary checks could be added here
      el.style.position = 'fixed'
      el.style.left = `${initialLeft + dx}px`
      el.style.top = `${initialTop + dy}px`
      el.style.bottom = 'auto'
      el.style.right = 'auto'
    }

    const onMouseUp = () => {
      isDragging = false
      el.style.cursor = 'grab'
    }

    el.addEventListener('mousedown', onMouseDown)
    window.addEventListener('mousemove', onMouseMove)
    window.addEventListener('mouseup', onMouseUp)

    // Touch support
    el.addEventListener('touchstart', (e) => {
      const touch = e.touches[0]
      if (touch) {
        onMouseDown({ clientX: touch.clientX, clientY: touch.clientY, preventDefault: () => {} } as any)
      }
    })
    window.addEventListener('touchmove', (e) => {
      const touch = e.touches[0]
      if (touch) {
        onMouseMove({ clientX: touch.clientX, clientY: touch.clientY } as any)
      }
    })
    window.addEventListener('touchend', onMouseUp)
  }
}

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

// Helper for safe video playback
const safePlayVideo = async (videoEl: HTMLVideoElement | null, stream: MediaStream | null) => {
  if (!videoEl || !stream) return
  
  try {
    videoEl.srcObject = stream
    // Check if already playing to avoid interruption
    if (videoEl.paused) {
      await videoEl.play()
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') {
      console.error('[CallModal] Video play failed', e)
    }
  }
}

// Watch for remote stream changes
watch(() => webRTCService.remoteStream.value, (newStream) => {
  console.log('[CallModal] Remote stream changed', newStream)
  if (newStream) {
    // Audio binding
    if (remoteAudioRef.value) {
      remoteAudioRef.value.srcObject = newStream
      remoteAudioRef.value.play().catch(e => console.error('Audio auto-play failed', e))
    }
    // Video binding
    safePlayVideo(remoteVideoRef.value, newStream)
  }
}, { immediate: true, flush: 'post' })

// Watch for local stream changes (for video self-view)
watch(() => webRTCService.localStream.value, (newStream) => {
  console.log('[CallModal] Local stream changed', newStream)
  if (newStream) {
    safePlayVideo(localVideoRef.value, newStream)
    if (localVideoRef.value) {
        localVideoRef.value.muted = true
    }
  }
}, { immediate: true, flush: 'post' })

// Watch for ref changes (in case stream was ready before mount)
watch(remoteVideoRef, (el) => {
  if (el && webRTCService.remoteStream.value) {
    safePlayVideo(el, webRTCService.remoteStream.value)
  }
})

watch(localVideoRef, (el) => {
  if (el && webRTCService.localStream.value) {
    safePlayVideo(el, webRTCService.localStream.value)
    el.muted = true
  }
})

const handleAccept = () => {
  webRTCService.acceptCall()
}

const handleReject = () => {
  webRTCService.rejectCall()
}

const handleHangup = () => {
  webRTCService.hangup()
}

const toggleMute = () => {
  webRTCService.toggleMute()
}

const toggleVideo = () => {
  webRTCService.toggleVideo()
}

const switchCamera = () => {
  webRTCService.switchCamera()
}

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="callStore.callState !== 'idle'" class="fixed inset-0 z-[9999] flex items-center justify-center overflow-hidden font-sans bg-slate-900">
        
        <!-- 1. Background Layer -->
        <!-- Video Call: Remote Video Background -->
        <div v-if="callStore.callType === 'video'" class="absolute inset-0">
           <video 
              ref="remoteVideoRef" 
              class="w-full h-full object-cover transition-opacity duration-700" 
              :class="webRTCService.remoteStream.value ? 'opacity-100' : 'opacity-0'"
              autoplay 
              playsinline
            ></video>
             <!-- Placeholder while loading video -->
             <div v-if="!webRTCService.remoteStream.value" class="absolute inset-0 bg-slate-900 flex items-center justify-center">
                <!-- Use blurred avatar as placeholder background -->
                <div class="absolute inset-0 bg-cover bg-center blur-3xl opacity-30 scale-110 transform" 
                     :style="`background-image: url(${formatImageUrl(callStore.remoteUserInfo?.avatar || '')})`"></div>
                
                <!-- Status Text -->
                <div class="relative z-10 flex flex-col items-center gap-3">
                    <div class="w-16 h-16 rounded-full bg-white/10 flex items-center justify-center backdrop-blur-md animate-pulse">
                        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-white/50"><path d="M23 7l-7 5 7 5V7z"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
                    </div>
                    <span class="text-white/60 font-medium text-lg">等待对方开启摄像头...</span>
                </div>
             </div>
        </div>

        <!-- Audio Call: Blurred Avatar Background -->
        <div v-else class="absolute inset-0">
            <div class="absolute inset-0 bg-cover bg-center blur-[100px] opacity-60 scale-125 transform transition-all duration-1000" 
                 :style="`background-image: url(${formatImageUrl(callStore.remoteUserInfo?.avatar || '')})`"></div>
            <div class="absolute inset-0 bg-black/40"></div> <!-- Overlay for readability -->
        </div>
        
        <!-- Remote Audio Element -->
        <audio ref="remoteAudioRef" autoplay></audio>


        <!-- 2. Main Content Layer -->
        <div class="relative z-10 w-full h-full flex flex-col justify-between p-8 safe-area-inset-bottom">
          
          <!-- Top Area: User Info -->
          <div class="flex flex-col items-center mt-16 transition-all duration-500"
               :class="(callStore.callType === 'video' && callStore.callState === 'connected') ? 'opacity-0 -translate-y-20 pointer-events-none' : 'opacity-100 translate-y-0'">
            
            <!-- Avatar Wrapper -->
            <div class="relative mb-8">
               <!-- Breathing Rings -->
               <div v-if="callStore.callState === 'calling' || callStore.callState === 'incoming'" class="absolute inset-0 rounded-full border border-white/20 animate-ping-slow"></div>
               <div v-if="callStore.callState === 'calling' || callStore.callState === 'incoming'" class="absolute inset-0 rounded-full border border-white/10 animate-ping-slower delay-150"></div>
               
               <img 
                :src="formatImageUrl(callStore.remoteUserInfo?.avatar)" 
                class="relative w-32 h-32 rounded-full shadow-2xl object-cover border-4 border-white/10"
              />
            </div>

            <!-- Name & Status -->
            <h2 class="text-4xl font-bold text-white tracking-tight drop-shadow-lg mb-3">{{ callStore.remoteUserInfo?.name }}</h2>
            
            <div class="flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/10 backdrop-blur-md border border-white/5 shadow-sm">
               <span v-if="callStore.callState === 'connected'" class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
               <span class="text-white/80 font-medium text-base">
                  <span v-if="callStore.callState === 'incoming'">
                    {{ callStore.callType === 'video' ? '邀请你视频通话...' : '邀请你语音通话...' }}
                  </span>
                  <span v-else-if="callStore.callState === 'calling'">正在等待对方接受...</span>
                  <span v-else-if="callStore.callState === 'connected'">
                      {{ duration }} 
                      <span class="text-xs opacity-70 ml-2">({{ webRTCService.connectionState.value }})</span>
                  </span>
                  <span v-else-if="callStore.callState === 'ended'">通话结束</span>
               </span>
            </div>
          </div>

          <!-- Middle Area: Local Video (PiP) -->
          <template v-if="callStore.callType === 'video'">
             <div 
              v-if="callStore.callState === 'connected' || callStore.callState === 'calling'"
              v-drag
              class="fixed right-6 top-6 w-36 h-52 sm:w-48 sm:h-72 bg-slate-800 rounded-2xl overflow-hidden shadow-2xl border border-white/10 cursor-grab active:cursor-grabbing z-20 hover:scale-105 transition-transform duration-300 group"
            >
              <video 
                ref="localVideoRef" 
                class="w-full h-full object-cover mirror" 
                autoplay 
                playsinline 
                muted
              ></video>
              
              <!-- Camera Off State -->
              <div v-if="!callStore.isCameraOn" class="absolute inset-0 flex flex-col items-center justify-center bg-slate-800/90 backdrop-blur-sm">
                <div class="w-10 h-10 rounded-full bg-slate-700 flex items-center justify-center mb-2">
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m16 13 5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5"/><rect x="2" y="6" width="14" height="12" rx="2"/><line x1="2" y1="2" x2="22" y2="22"/></svg>
                </div>
              </div>

              <!-- Permission Error (Subtle) -->
              <div v-if="!webRTCService.localStream.value && callStore.isCameraOn" class="absolute bottom-2 left-2 right-2 bg-red-500/90 backdrop-blur-md text-white text-[10px] font-medium text-center py-1 rounded-lg shadow-sm">
                无法获取摄像头
              </div>
            </div>
          </template>

          <!-- Bottom Area: Controls -->
          <div class="flex flex-col items-center mb-8">
            
            <!-- Controls Row -->
            <div class="flex items-center justify-center gap-8 sm:gap-12">
              
              <!-- Incoming Call Actions -->
              <template v-if="callStore.callState === 'incoming'">
                <button @click="handleReject" class="flex flex-col items-center gap-3 group">
                  <div class="w-16 h-16 sm:w-20 sm:h-20 rounded-full bg-rose-500 hover:bg-rose-600 flex items-center justify-center text-white shadow-xl shadow-rose-500/20 transition-all duration-300 group-active:scale-95">
                    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                  </div>
                  <span class="text-sm font-medium text-white/90">拒绝</span>
                </button>

                <button @click="handleAccept" class="flex flex-col items-center gap-3 group">
                  <div class="w-16 h-16 sm:w-20 sm:h-20 rounded-full bg-emerald-500 hover:bg-emerald-600 flex items-center justify-center text-white shadow-xl shadow-emerald-500/20 transition-all duration-300 group-active:scale-95 animate-bounce-subtle">
                    <svg v-if="callStore.callType === 'video'" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M23 7l-7 5 7 5V7z"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                  </div>
                  <span class="text-sm font-medium text-white/90">接听</span>
                </button>
              </template>

              <!-- Active Call Controls -->
              <template v-else>
                 <!-- Mute -->
                 <button @click="toggleMute" class="control-btn" :class="callStore.isMuted ? 'bg-white text-slate-900' : 'bg-white/10 text-white hover:bg-white/20'">
                   <svg v-if="!callStore.isMuted" xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>
                   <svg v-else xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="1" y1="1" x2="23" y2="23"/><path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6"/><path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a7 7 0 0 1-.11 1.23"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>
                 </button>

                 <!-- Video Toggle -->
                 <button v-if="callStore.callType === 'video'" @click="toggleVideo" class="control-btn" :class="!callStore.isCameraOn ? 'bg-white text-slate-900' : 'bg-white/10 text-white hover:bg-white/20'">
                   <svg v-if="callStore.isCameraOn" xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 7l-7 5 7 5V7z"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
                   <svg v-else xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m16 13 5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5"/><rect x="2" y="6" width="14" height="12" rx="2"/><line x1="2" y1="2" x2="22" y2="22"/></svg>
                 </button>

                 <!-- Switch Camera (Mobile/Tablet) -->
                 <button v-if="callStore.callType === 'video'" @click="switchCamera" class="control-btn bg-white/10 text-white hover:bg-white/20">
                   <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/><path d="M12 2v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="M2 12h2"/><path d="m4.93 19.07 1.41-1.41"/><path d="M12 22v-2"/><path d="m19.07 19.07-1.41-1.41"/><path d="M22 12h-2"/><path d="m19.07 4.93-1.41 1.41"/></svg>
                 </button>

                 <!-- Hangup -->
                 <button @click="handleHangup" class="w-16 h-16 sm:w-20 sm:h-20 rounded-full bg-rose-500 hover:bg-rose-600 flex items-center justify-center text-white shadow-xl shadow-rose-500/30 transition-all duration-300 active:scale-95">
                   <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.68 13.31a16 16 0 0 0 3.41 2.6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7 2 2 0 0 1 1.72 2v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.42 19.42 0 0 1-3.33-2.67m-2.67-3.34a19.79 19.79 0 0 1-3.07-8.63A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91"/><line x1="23" y1="1" x2="1" y2="23"/></svg>
                 </button>
              </template>
            </div>
          </div>
        </div>

      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* Animations */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes pulse-slow {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 0.2; transform: scale(1.1); }
}
.animate-ping-slow {
  animation: pulse-slow 3s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
.animate-ping-slower {
  animation: pulse-slow 4s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.animate-bounce-subtle {
  animation: bounce 2s infinite;
}

/* Utilities */
.control-btn {
  @apply w-14 h-14 sm:w-16 sm:h-16 rounded-full flex items-center justify-center transition-all duration-300 active:scale-95 shadow-lg backdrop-blur-md border border-white/10;
}
.mirror {
  transform: scaleX(-1);
}
</style>

<style scoped>
.animate-pulse-slow {
  animation: pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
.animate-bounce-subtle {
  animation: bounce 2s infinite;
}
</style>
