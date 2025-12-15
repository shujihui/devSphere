<script setup lang="ts">
import { ref, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { useChatStore, type ChatMessage, MsgType } from '../../stores/chatStore'
import { MessageContentType } from '../../services/chatService'
import { useUserStore } from '../../stores/userStore'
import ChatDetailModal from './ChatDetailModal.vue'
import GroupDetailModal from './GroupDetailModal.vue'

const chatStore = useChatStore()
const userStore = useUserStore()

const messagesContainer = ref<HTMLElement | null>(null)

// [V7] 状态：是否正在加载更多历史消息
const isLoadingMore = ref(false)

// [V6] 宽限期活动通知
const handleActivity = () => {
  chatStore.handleUserActivity()
}

// [V7] 滚动到顶部的处理函数（加载历史）
const handleScrollTop = async () => {
  const el = messagesContainer.value
  if (!el) return

  // 如果已经在加载中，直接返回
  if (isLoadingMore.value) return

  // 小于 5px 视为到顶（更稳妥），避免精度问题
  if (el.scrollTop > 5) return

  const currentRoomId = chatStore.activeRoomId
  if (!currentRoomId) return

  // 检查 store 中是否还有更多数据
  const pagination = chatStore.roomPagination[currentRoomId]
  if (pagination && !pagination.hasMore) {
    // 没有更多了
    return
  }

  // 开始加载
  isLoadingMore.value = true

  // 记录加载前的高度
  const oldScrollHeight = el.scrollHeight

  try {
    // 加载历史（store 内会把新消息 prepend 到前面）
    await chatStore.loadMoreMessages(currentRoomId)

    // 等待 DOM 更新（确保消息插入）
    await nextTick()

    // 等到浏览器布局稳定后恢复滚动
    requestAnimationFrame(() => {
      const newScrollHeight = el.scrollHeight
      // 恢复到用户看到的位置（新高度 - 老高度）
      el.scrollTop = newScrollHeight - oldScrollHeight
    })
  } catch (error) {
    console.error('加载更多历史消息失败:', error)
  } finally {
    // 稍微延迟以避免抖动
    setTimeout(() => {
      isLoadingMore.value = false
    }, 120)
  }
}





// Helper: Detect Message Type
// Helper: Detect Message Type
const isImageUrl = (content: string) => {
  if (!content) return false
  // Add support for svg and dicebear api
  return /\.(jpg|jpeg|png|gif|webp|bmp|svg)$/i.test(content) || 
         content.startsWith('blob:') || 
         content.includes('api.dicebear.com')
}

const isFileUrl = (content: string) => {
  if (!content) return false
  // Simple check: if it looks like a URL but not an image
  return (content.startsWith('http') || content.startsWith('/')) && !isImageUrl(content)
}

const getFileName = (url: string) => {
  try {
    return url.substring(url.lastIndexOf('/') + 1)
  } catch {
    return '未知文件'
  }
}

// Chat Image Preview
import ImageViewer from '../ImageViewer.vue'
const showImageViewer = ref(false)
const previewImages = ref<string[]>([])

const previewChatImage = (url: string) => {
  previewImages.value = [url]
  showImageViewer.value = true
}

const formatCallMessage = (msg: ChatMessage) => {
  const isSelf = String(msg.senderId) === String(userStore.userInfo?.id)

  if (msg.content === 'CANCELLED') {
    return isSelf ? '已取消' : '对方已取消'
  }
  if (msg.content === 'REJECTED') {
    return isSelf ? '已拒绝' : '对方已拒绝'
  }
  if (msg.content.startsWith('DURATION:')) {
    const parts = msg.content.split(':');
    // DURATION:HH:MM:SS or DURATION:MM:SS
    const duration = parts.length === 3 ? `${parts[1]}:${parts[2]}` : `${parts[1]}:${parts[2]}:${parts[3]}`;
    return `通话时长 ${duration}`
  }
  return msg.content
}

const getCallIconType = (msg: ChatMessage) => {
  if (msg.messageType === MessageContentType.VIDEO_CALL) return 'video'
  if (msg.messageType === MessageContentType.VOICE_CALL) return 'phone'
  return 'phone' // Default fallback
}

import { formatImageUrl } from '../../utils/image'

const isCallMessage = (msg: ChatMessage) => {
  return (
    msg.messageType === MessageContentType.VOICE_CALL || 
    msg.messageType === MessageContentType.VIDEO_CALL ||
    msg.content === 'CANCELLED' || 
    msg.content === 'REJECTED' || 
    msg.content.startsWith('DURATION:')
  )
}

// 判断是否是群聊邀请消息
const isGroupCallInvite = (msg: ChatMessage) => {
  return msg.messageType === MessageContentType.GROUP_CALL_INVITE
}

// 解析群聊邀请消息
const parseGroupCallInvite = (msg: ChatMessage) => {
  try {
    return JSON.parse(msg.content)
  } catch {
    return null
  }
}

// 处理加入群聊通话
const handleJoinGroupCall = (msg: ChatMessage) => {
  const invite = parseGroupCallInvite(msg)
  if (!invite) {
    console.error('无法解析群聊邀请')
    return
  }
  
  const groupId = String(chatStore.activeConversation?.id)
  const hostId = String(msg.senderId)
  const callType = invite.callType
  
  console.log('[ChatBox] 加入群聊通话:', { groupId, hostId, callType })
  
  webRTCService.joinGroupCall(groupId, hostId, callType)
}




// ---------- 滚动/加载交互逻辑 ----------

// [V8] 初始加载状态 (用于隐藏滚动过程)
// [V8] 初始加载状态 (用于隐藏滚动过程)
const isInitialLoad = ref(false)

// ---------- 滚动/加载交互逻辑 ----------

// 1) 监听 activeRoomId
watch(
  () => chatStore.activeRoomId,
  (newId) => {
    if (!newId) return
    
    // 切换房间时，先隐藏内容，避免看到从上往下滚动的过程
    isInitialLoad.value = true

    if (chatStore.lastOpenedLoadedRoomId === newId) {
      scrollToBottom('auto')
      chatStore.lastOpenedLoadedRoomId = null as any
      return
    }
    const stop = watch(
      () => chatStore.lastOpenedLoadedRoomId,
      (val) => {
        if (val === newId) {
          scrollToBottom('auto')
          chatStore.lastOpenedLoadedRoomId = null as any
          stop()
        }
      },
      { immediate: true }
    )
  },
  { immediate: true }
)

// 2) 监听消息长度变化
watch(
  () => chatStore.activeMessages.length,
  async (newLength, oldLength) => {
    if (newLength <= oldLength) return
    if (isLoadingMore.value) return
    if (chatStore.lastMessageDirection === 'append') {
      await scrollToBottom('smooth')
      chatStore.lastMessageDirection = null as any
    }
  }
)
// Upload Logic
import { ossService } from '../../services/ossService'

const imageInput = ref<HTMLInputElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

const triggerImageUpload = () => imageInput.value?.click()
const triggerFileUpload = () => fileInput.value?.click()

const handleImageUpload = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  
  try {
    const res = await ossService.upload(file)
    insertHtmlAtCursor(`<img src="${res.url}" class="w-20 h-20 object-cover inline-block align-middle mx-1 rounded select-none" contenteditable="false" />`)
  } catch (error) {
    console.error('Image upload failed:', error)
    alert('图片上传失败')
  } finally {
    if (imageInput.value) imageInput.value.value = ''
  }
}

const handleFileUpload = async (e: Event) => {
  // File upload still sends immediately or we could implement a file card in editor?
  // For now, let's keep file upload as immediate send or maybe just text link?
  // User asked for "mixed text and image", didn't specify file.
  // Let's keep file upload as immediate send for now to avoid complexity of rendering file card in contenteditable.
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  try {
    const res = await ossService.upload(file)
    chatStore.sendMessage(res.url, MessageContentType.FILE)
  } catch (error) {
    console.error('File upload failed:', error)
    alert('文件上传失败')
  } finally {
    if (fileInput.value) fileInput.value.value = ''
  }
}

const handlePaste = async (e: ClipboardEvent) => {
  const items = e.clipboardData?.items
  if (!items) return

  for (const item of items) {
    if (item.type.indexOf('image') !== -1) {
      const file = item.getAsFile()
      if (file) {
        e.preventDefault()
        try {
          const res = await ossService.upload(file)
          insertHtmlAtCursor(`<img src="${res.url}" class="w-20 h-20 object-cover inline-block align-middle mx-1 rounded select-none" contenteditable="false" />`)
        } catch (error) {
          console.error('Paste upload failed:', error)
          alert('图片粘贴上传失败')
        }
      }
      return
    }
  }
}
// Call Logic
import webRTCService from '../../services/WebRTCService'

// Emoji & Sticker Logic
const showEmojiPicker = ref(false)
const activeEmojiTab = ref<'emoji' | 'sticker'>('emoji')

const emojiList = [
  '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃',
  '😉', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗', '😚', '😙',
  '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔',
  '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥',
  '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢', '🤮',
  '🤧', '🥵', '🥶', '🥴', '😵', '🤯', '🤠', '🥳', '😎', '🤓',
  '🧐', '😕', '😟', '🙁', '😮', '😯', '😲', '😳', '🥺', '😦',
  '😧', '😨', '😰', '😥', '😢', '😭', '😱', '😖', '😣', '😞',
  '😓', '😩', '😫', '🥱', '😤', '😡', '😠', '🤬', '😈', '👿',
  '💀', '☠', '💩', '🤡', '👹', '👺', '👻', '👽', '👾', '🤖',
  '👍', '👎', '👌', '✌', '🤞', '🤟', '🤘', '🤙', '👈', '👉',
  '👆', '👇', '✋', '🤚', '🖐', '🖖', '👋', '🤙', '💪', '🖕',
  '✍', '🙏', '💍', '💄', '💋', '👄', '👅', '👂', '👃', '👣',
  '👁', '👀', '🧠', '🦴', '🦷', '🗣', '👤', '👥', '🫂', '👶',
  '❤', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔',
  '❣', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮',
]

const stickerList = [
  'https://api.dicebear.com/7.x/fun-emoji/svg?seed=Felix',
  'https://api.dicebear.com/7.x/fun-emoji/svg?seed=Aneka',
  'https://api.dicebear.com/7.x/fun-emoji/svg?seed=Bella',
  'https://api.dicebear.com/7.x/fun-emoji/svg?seed=Caleb',
  'https://api.dicebear.com/7.x/fun-emoji/svg?seed=Dylan',
  'https://api.dicebear.com/7.x/fun-emoji/svg?seed=Eliza',
  'https://api.dicebear.com/7.x/fun-emoji/svg?seed=Fiona',
  'https://api.dicebear.com/7.x/fun-emoji/svg?seed=George',
  'https://api.dicebear.com/7.x/fun-emoji/svg?seed=Hannah',
]
// 发送消息 (Rich Text Handler)
const editorRef = ref<HTMLElement | null>(null)

// Helper: Insert HTML at cursor
const insertHtmlAtCursor = (html: string) => {
  const sel = window.getSelection()
  if (sel && sel.rangeCount > 0) {
    const range = sel.getRangeAt(0)
    // Ensure the range is within our editor
    if (editorRef.value && editorRef.value.contains(range.commonAncestorContainer)) {
      range.deleteContents()
      const el = document.createElement('div')
      el.innerHTML = html
      const frag = document.createDocumentFragment()
      let node: Node | null
      let lastNode: Node | null = null
      while ((node = el.firstChild)) {
        lastNode = frag.appendChild(node)
      }
      range.insertNode(frag)
      if (lastNode) {
        range.setStartAfter(lastNode)
        range.collapse(true)
        sel.removeAllRanges()
        sel.addRange(range)
      }
      return
    }
  }
  // Fallback: Append to end
  if (editorRef.value) {
    editorRef.value.innerHTML += html
    // Move cursor to end
    const range = document.createRange()
    range.selectNodeContents(editorRef.value)
    range.collapse(false)
    const sel = window.getSelection()
    sel?.removeAllRanges()
    sel?.addRange(range)
  }
}

const handleSend = async (e?: KeyboardEvent) => {
  if (e && e.shiftKey) return // Allow Shift+Enter for new line
  if (e) e.preventDefault() // Prevent default div newline behavior on Enter

  if (!editorRef.value) return
  const nodes = editorRef.value.childNodes
  if (nodes.length === 0) return

  // Parse and send sequence
  let currentText = ''
  
  const sendText = async () => {
    if (currentText.trim()) {
      await chatStore.sendMessage(currentText)
      currentText = ''
    } else {
      currentText = '' // Clear whitespace
    }
  }

  for (const node of Array.from(nodes)) {
    if (node.nodeType === Node.TEXT_NODE) {
      currentText += node.textContent
    } else if (node.nodeType === Node.ELEMENT_NODE) {
      const el = node as HTMLElement
      if (el.tagName === 'IMG') {
        // Send accumulated text first
        await sendText()
        // Send Image
        const src = (el as HTMLImageElement).getAttribute('src')
        if (src) {
          await chatStore.sendMessage(src, MessageContentType.IMAGE)
        }
      } else if (el.tagName === 'BR') {
        currentText += '\n'
      } else {
        // Handle other elements (like divs from copy-paste) as text
        currentText += el.textContent
      }
    }
  }
  // Send remaining text
  await sendText()

  // Clear editor
  editorRef.value.innerHTML = ''
  scrollToBottom('auto')
}

const handleMenuClick = () => {
  if (!chatStore.activeConversation) return
  if (chatStore.activeConversation.type === MsgType.GROUP) {
    chatStore.openGroupDetailModal(chatStore.activeConversation.id)
  } else {
    chatStore.openChatDetailModal()
  }
}

// 滚动到底部
const scrollToBottom = async (behavior: 'smooth' | 'auto' = 'smooth') => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTo({
      top: messagesContainer.value.scrollHeight,
      behavior,
    })
    
    // 如果是初始加载（auto），滚动完成后显示内容
    if (behavior === 'auto' && isInitialLoad.value) {
      // 稍微延迟一帧确保渲染完成
      requestAnimationFrame(() => {
        isInitialLoad.value = false
      })
    }
  }
}

// 判断是否需要显示时间戳（5 分钟间隔）
const shouldShowTime = (currentMsg: ChatMessage, index: number) => {
  if (index === 0) return true
  const prevMsg = chatStore.activeMessages[index - 1]
  if (!prevMsg || !prevMsg.time || !currentMsg.time) return true
  
  const currTime = currentMsg.time instanceof Date ? currentMsg.time.getTime() : new Date(currentMsg.time).getTime()
  const prevTime = prevMsg.time instanceof Date ? prevMsg.time.getTime() : new Date(prevMsg.time).getTime()
  
  return currTime - prevTime > 5 * 60 * 1000 // 5 分钟
}

// 格式化时间戳，健壮处理无效日期
const formatMessageTime = (date: Date | string | null) => {
  const d = date ? (date instanceof Date ? date : new Date(date)) : new Date()
  if (isNaN(d.getTime())) return ''
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

// 辅助：消息 key 生成（保证唯一且稳定）
const messageKey = (msg: ChatMessage, index: number) => {
  if (msg.id) return String(msg.id)
  if (msg.tempId) return msg.tempId
  return `idx_${index}`
}

const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

const insertEmoji = (emoji: string) => {
  insertHtmlAtCursor(emoji)
}

const sendSticker = (url: string) => {
  insertHtmlAtCursor(`<img src="${url}" class="w-10 h-10 inline-block align-middle mx-1 select-none" contenteditable="false" />`)
  showEmojiPicker.value = false
}

// Close picker when clicking outside
const closePicker = () => {
  showEmojiPicker.value = false
}

onMounted(() => {
  document.addEventListener('click', closePicker)
})

onUnmounted(() => {
  document.removeEventListener('click', closePicker)
})

const handleVoiceCall = () => {
  const conv = chatStore.activeConversation
  if (!conv) return
  
  const mode = conv.type === MsgType.GROUP ? 'group' : 'p2p'
  
  const targetId = mode === 'group' ? String(conv.id) : conv.targetId

  // Start call
  webRTCService.startCall(targetId, {
    id: targetId,
    name: conv.name,
    avatar: conv.avatar
  }, 'audio', mode)
}

const handleVideoCall = () => {
  const conv = chatStore.activeConversation
  if (!conv) return
  
  const mode = conv.type === MsgType.GROUP ? 'group' : 'p2p'
  
  const targetId = mode === 'group' ? String(conv.id) : conv.targetId

  // Start call
  webRTCService.startCall(targetId, {
    id: targetId,
    name: conv.name,
    avatar: conv.avatar
  }, 'video', mode)
}
</script>

<template>
  <div class="flex flex-col h-full bg-[#F5F7FA] dark:bg-slate-900 transition-colors duration-300 relative" @click="handleActivity">
    
    <!-- 1. 顶部 Header (毛玻璃效果) -->
    <div class="h-[72px] px-6 flex items-center justify-between border-b border-slate-200/50 dark:border-slate-700/50 bg-white/80 dark:bg-slate-900/80 backdrop-blur-xl z-20 sticky top-0 transition-all">
      <div class="flex items-center gap-4">
        <div class="flex flex-col">
          <div class="flex items-center gap-2">
            <span class="font-bold text-lg text-slate-900 dark:text-white tracking-tight">{{ chatStore.activeConversation?.name }}</span>
            <span
              v-if="chatStore.activeConversation?.type === MsgType.GROUP && chatStore.activeConversation?.memberCount"
              class="px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-500 text-xs rounded-full font-medium"
            >
              {{ chatStore.activeConversation.memberCount }}人
            </span>
          </div>
          <!-- 在线状态 -->
          <div v-if="chatStore.activeConversation?.isOnline" class="flex items-center gap-1.5 mt-0.5">
             <span class="relative flex h-2 w-2">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-500 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <span class="text-xs text-emerald-600 dark:text-emerald-400 font-medium">在线</span>
          </div>
        </div>
      </div>
      
      <!-- 更多操作按钮 -->
      <div class="flex items-center gap-2 relative">
        <!-- Voice Call Button -->
        <button 
          v-if="chatStore.activeConversation?.type === MsgType.PRIVATE || chatStore.activeConversation?.type === MsgType.GROUP"
          @click="handleVoiceCall" 
          class="p-2.5 text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white rounded-xl transition-all active:scale-95"
          title="语音通话"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
        </button>

        <!-- Video Call Button -->
        <button 
          v-if="chatStore.activeConversation?.type === MsgType.PRIVATE || chatStore.activeConversation?.type === MsgType.GROUP"
          @click="handleVideoCall" 
          class="p-2.5 text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white rounded-xl transition-all active:scale-95"
          title="视频通话"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
        </button>

        <button @click="handleMenuClick" class="p-2.5 text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white rounded-xl transition-all active:scale-95">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></svg>
        </button>
      </div>
    </div>

    <!-- 2. 消息区域 -->
    <div
      ref="messagesContainer"
      @scroll="handleScrollTop"
      class="flex-1 overflow-y-auto p-6 space-y-8 custom-scrollbar transition-opacity duration-200"
      :class="{ 'opacity-0': isInitialLoad }"
    >
      <!-- 加载 Loading -->
      <div v-if="isLoadingMore" class="flex justify-center py-4">
        <div class="w-8 h-8 border-4 border-blue-100 dark:border-slate-700 border-t-blue-500 rounded-full animate-spin"></div>
      </div>

      <template v-for="(msg, index) in chatStore.activeMessages" :key="messageKey(msg, index)">
        <!-- 时间分割线 -->
        <div v-if="shouldShowTime(msg, index)" class="flex justify-center my-6">
          <span class="text-xs font-medium text-slate-400 dark:text-slate-500 bg-slate-200/50 dark:bg-slate-800/50 px-3 py-1 rounded-full backdrop-blur-sm">
            {{ formatMessageTime(msg.time) }}
          </span>
        </div>

        <!-- 消息体 -->
        <div
          class="flex gap-4 group"
          :class="String(msg.senderId) === String(userStore.userInfo?.id) ? 'flex-row-reverse' : ''"
        >
          <!-- 头像 -->
          <img
            :src="formatImageUrl(msg.senderAvatar)"
            class="h-10 w-10 rounded-2xl bg-white dark:bg-slate-800 border-2 border-white dark:border-slate-700 shadow-sm shrink-0 cursor-pointer hover:scale-105 transition-transform self-start mt-1"
            :title="msg.senderName"
          />

          <div class="flex flex-col max-w-[70%]" :class="String(msg.senderId) === String(userStore.userInfo?.id) ? 'items-end' : 'items-start'">
            <!-- 群聊显示昵称 -->
            <div v-if="chatStore.activeConversation?.type === MsgType.GROUP && String(msg.senderId) !== String(userStore.userInfo?.id)" class="text-[11px] text-slate-400 dark:text-slate-500 mb-1 px-1 font-medium">
              {{ msg.senderName }}
            </div>

            <div class="relative">
              <!-- 气泡 -->
              <div
                class="px-5 py-3 text-[15px] leading-relaxed shadow-sm transition-all duration-200"
                :class="[
                  'whitespace-pre-wrap',
                  'break-words',
                  String(msg.senderId) === String(userStore.userInfo?.id)
                    ? 'bg-gradient-to-br from-blue-500 to-indigo-600 text-white rounded-2xl rounded-tr-sm shadow-blue-500/20'
                    : 'bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 border border-slate-100 dark:border-slate-700 rounded-2xl rounded-tl-sm',
                  msg.status === 'sending' ? 'opacity-70' : ''
                ]"
              >
                <!-- Image Message -->
                <div v-if="isImageUrl(msg.content)" class="max-w-[300px] rounded-lg overflow-hidden cursor-pointer" @click="previewChatImage(msg.content)">
                  <img :src="msg.content" class="w-full h-auto object-cover" loading="lazy" />
                </div>
                <!-- File Message -->
                <div v-else-if="isFileUrl(msg.content)" class="flex items-center gap-3 p-1 min-w-[200px]">
                  <div class="p-3 bg-slate-100 dark:bg-slate-700 rounded-lg">
                     <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-blue-500"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>
                  </div>
                  <div class="flex-1 min-w-0">
                  <div class="text-sm font-medium truncate max-w-[150px]">{{ getFileName(msg.content) }}</div>
                    <a :href="msg.content" target="_blank" download class="text-xs text-blue-500 hover:underline mt-0.5 block">点击下载</a>
                  </div>
                </div>
                <!-- Call Message -->
                <!-- Call Message -->
                <div v-else-if="isCallMessage(msg)" class="flex items-center gap-1.5 text-sm text-gray-500 bg-gray-100 px-3 py-1.5 rounded-full">
                  <span>{{ formatCallMessage(msg) }}</span>
                  <!-- Phone Icon -->
                  <svg v-if="getCallIconType(msg) === 'phone'" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
                  <!-- Video Icon -->
                  <svg v-if="getCallIconType(msg) === 'video'" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="23 7 16 12 23 17 23 7"></polygon><rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect></svg>
                </div>
                
                <!-- 群聊通话邀请卡片 -->
                <div 
                  v-else-if="isGroupCallInvite(msg)" 
                  class="bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 border border-blue-200 dark:border-blue-800 rounded-xl p-4 max-w-sm min-w-[280px]"
                >
                  <div class="flex items-start gap-3">
                    <!-- 图标 -->
                    <div class="flex-shrink-0 w-12 h-12 bg-blue-500 rounded-full flex items-center justify-center">
                      <svg v-if="parseGroupCallInvite(msg)?.callType === 'video'" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <polygon points="23 7 16 12 23 17 23 7"></polygon>
                        <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
                      </svg>
                      <svg v-else xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                      </svg>
                    </div>
                    
                    <!-- 内容 -->
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-1">
                        <span class="font-semibold text-gray-900 dark:text-white">
                          {{ msg.senderName }}
                        </span>
                        <span class="text-xs text-gray-500">
                          邀请你加入{{ parseGroupCallInvite(msg)?.callType === 'video' ? '视频' : '语音' }}通话
                        </span>
                      </div>
                      
                      <p class="text-sm text-gray-600 dark:text-gray-300 mb-3">
                        点击下方按钮加入通话
                      </p>
                      
                      <!-- 加入按钮 -->
                      <button
                        @click="handleJoinGroupCall(msg)"
                        class="w-full bg-blue-500 hover:bg-blue-600 text-white font-medium py-2 px-4 rounded-lg transition-colors flex items-center justify-center gap-2"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                        </svg>
                        加入通话
                      </button>
                    </div>
                  </div>
                </div>
                
                <!-- Text Message -->
                <div v-else>
                  {{ msg.content }}
                </div>
              </div>

              <!-- 状态图标 (发送中/失败) -->
              <div
                class="absolute top-1/2 -translate-y-1/2 flex items-center gap-2 transition-all duration-200"
                :class="[
                  String(msg.senderId) === String(userStore.userInfo?.id) ? '-left-8' : '-right-8'
                ]"
              >
                <div v-if="msg.status === 'sending'" class="w-4 h-4 border-2 border-slate-300 border-t-blue-500 rounded-full animate-spin"></div>
                <svg v-else-if="msg.status === 'error'" class="h-5 w-5 text-rose-500 cursor-pointer hover:scale-110 transition-transform" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 17.5228 17.5228 22 12 22ZM11 15V17H13V15H11ZM11 7V13H13V7H11Z"></path></svg>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 3. 底部输入区 (悬浮式设计) -->
    <div class="p-6 pt-2 bg-[#F5F7FA] dark:bg-slate-900 transition-colors z-20">
      <div class="bg-white dark:bg-slate-800 rounded-3xl border border-slate-200 dark:border-slate-700 shadow-sm focus-within:shadow-lg focus-within:border-blue-500/30 dark:focus-within:border-blue-500/30 transition-all duration-300 relative">
        
        <!-- 工具栏 -->
        <div class="flex items-center gap-1 px-3 pt-2 text-slate-400 dark:text-slate-500 relative">
          <button @click.stop="toggleEmojiPicker" class="p-2 hover:bg-slate-100 dark:hover:bg-slate-700/50 rounded-xl transition-colors hover:text-blue-500" title="表情"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" x2="9.01" y1="9" y2="9"/><line x1="15" x2="15.01" y1="9" y2="9"/></svg></button>
          
          <!-- Emoji Picker Popover -->
          <Transition
            enter-active-class="transition duration-200 ease-out"
            enter-from-class="transform scale-95 opacity-0 translate-y-2"
            enter-to-class="transform scale-100 opacity-100 translate-y-0"
            leave-active-class="transition duration-150 ease-in"
            leave-from-class="transform scale-100 opacity-100 translate-y-0"
            leave-to-class="transform scale-95 opacity-0 translate-y-2"
          >
            <div 
              v-if="showEmojiPicker"
              @click.stop
              class="absolute bottom-12 left-0 w-72 bg-white dark:bg-slate-800 rounded-2xl shadow-2xl border border-slate-100 dark:border-slate-700 overflow-hidden z-50 flex flex-col"
            >
              <!-- Tabs -->
              <div class="flex border-b border-slate-100 dark:border-slate-700">
                <button 
                  @click="activeEmojiTab = 'emoji'"
                  class="flex-1 py-2.5 text-sm font-medium transition-colors"
                  :class="activeEmojiTab === 'emoji' ? 'text-blue-600 bg-blue-50 dark:bg-blue-900/20' : 'text-slate-500 hover:bg-slate-50 dark:hover:bg-slate-700'"
                >
                  Emoji
                </button>
                <button 
                  @click="activeEmojiTab = 'sticker'"
                  class="flex-1 py-2.5 text-sm font-medium transition-colors"
                  :class="activeEmojiTab === 'sticker' ? 'text-blue-600 bg-blue-50 dark:bg-blue-900/20' : 'text-slate-500 hover:bg-slate-50 dark:hover:bg-slate-700'"
                >
                  贴图
                </button>
              </div>

              <!-- Content -->
              <div class="h-64 overflow-y-auto custom-scrollbar p-3">
                <!-- Emoji Grid -->
                <div v-if="activeEmojiTab === 'emoji'" class="grid grid-cols-6 gap-1">
                  <button 
                    v-for="emoji in emojiList" 
                    :key="emoji"
                    @click="insertEmoji(emoji)"
                    class="h-9 w-9 flex items-center justify-center text-xl hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                  >
                    {{ emoji }}
                  </button>
                </div>

                <!-- Sticker Grid -->
                <div v-if="activeEmojiTab === 'sticker'" class="grid grid-cols-3 gap-2">
                  <button 
                    v-for="(sticker, index) in stickerList" 
                    :key="index"
                    @click="sendSticker(sticker)"
                    class="aspect-square p-2 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-xl transition-colors"
                  >
                    <img :src="sticker" class="w-full h-full object-contain" />
                  </button>
                </div>
              </div>
            </div>
          </Transition>
          <button @click="triggerImageUpload" class="p-2 hover:bg-slate-100 dark:hover:bg-slate-700/50 rounded-xl transition-colors hover:text-blue-500" title="发送图片"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/></svg></button>
          <button @click="triggerFileUpload" class="p-2 hover:bg-slate-100 dark:hover:bg-slate-700/50 rounded-xl transition-colors hover:text-blue-500" title="发送文件"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg></button>
          <!-- Hidden Inputs -->
          <input type="file" ref="imageInput" accept="image/*" class="hidden" @change="handleImageUpload" />
          <input type="file" ref="fileInput" class="hidden" @change="handleFileUpload" />
          <div class="h-4 w-px bg-slate-200 dark:bg-slate-700 mx-2"></div>
          <button class="p-2 hover:bg-slate-100 dark:hover:bg-slate-700/50 rounded-xl transition-colors hover:text-blue-500" title="代码块"><svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg></button>
        </div>

        <!-- Rich Text Editor -->
        <div
          ref="editorRef"
          contenteditable="true"
          @keydown.enter="handleSend"
          @paste="handlePaste"
          class="w-full h-24 px-5 py-2 bg-transparent overflow-y-auto text-[15px] text-slate-800 dark:text-slate-100 outline-none custom-scrollbar leading-relaxed"
          style="min-height: 96px; max-height: 150px;"
        ></div>
        
        <!-- 发送按钮 -->
        <div class="flex justify-end px-4 pb-4">
           <button
            @click="handleSend()"
            class="px-6 py-2 bg-blue-600 text-white text-sm font-bold rounded-xl hover:bg-blue-700 hover:shadow-lg hover:shadow-blue-500/30 active:scale-95 transition-all flex items-center gap-2"
          >
            <span>发送</span>
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
          </button>
        </div>
      </div>
    </div>
    <!-- Modals -->
    <GroupDetailModal />
    <ChatDetailModal />
    <ImageViewer v-model="showImageViewer" :images="previewImages" />
  </div>
</template>
