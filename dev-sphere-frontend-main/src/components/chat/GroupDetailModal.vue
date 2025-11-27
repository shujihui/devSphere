<script setup lang="ts">
import { ref, computed } from 'vue' 
import { useChatStore } from '../../stores/chatStore'
import { useUserStore } from '../../stores/userStore'
import { formatImageUrl } from '@/utils/image'

import ContactPicker from '../call/ContactPicker.vue'

const chatStore = useChatStore()
const userStore = useUserStore()

const isEditingName = ref(false)
const newGroupName = ref('')
const editError = ref('')

const showAddMemberModal = ref(false)
const isDeleteMode = ref(false)

// 点击修改群名
const startEdit = () => {
  newGroupName.value = chatStore.currentGroupDetail?.name || ''
  isEditingName.value = true
  editError.value = ''
}

// 确认修改
const confirmEdit = async () => {
  if (!newGroupName.value) {
    editError.value = '群名不能为空'
    return
  }
  if (newGroupName.value === chatStore.currentGroupDetail?.name) {
    isEditingName.value = false
    return
  }
  
  try {
    if (chatStore.currentGroupDetail?.roomId) {
      await chatStore.updateGroupName(chatStore.currentGroupDetail.roomId, newGroupName.value)
      isEditingName.value = false
    }
  } catch (e: any) {
    editError.value = e.message || '修改失败'
  }
}

// 退出群聊
const handleQuitGroup = async () => {
  if (!confirm('确定要退出该群聊吗？')) return
  
  try {
    if (chatStore.currentGroupDetail?.roomId) {
      await chatStore.quitGroup(chatStore.currentGroupDetail.roomId)
    }
  } catch (e: any) {
    alert(e.message || '退出失败')
  }
}

// 邀请成员
const handleInviteMember = async (selectedUsers: any[]) => {
  if (!chatStore.currentGroupDetail?.roomId) return
  
  try {
    const userIds = selectedUsers.map(u => u.id)
    await chatStore.inviteToGroup(chatStore.currentGroupDetail.roomId, userIds)
    showAddMemberModal.value = false
  } catch (e: any) {
    alert(e.message || '邀请失败')
  }
}

// 移除成员
const toggleDeleteMode = () => {
  isDeleteMode.value = !isDeleteMode.value
}

const handleKickMember = async (uid: string) => {
  if (!chatStore.currentGroupDetail?.roomId) return
  if (!confirm('确定要将该成员移出群聊吗？')) return
  
  try {
    await chatStore.kickFromGroup(chatStore.currentGroupDetail.roomId, uid)
    // If no members left (unlikely for owner), or just to be safe
    if (chatStore.currentGroupMembers.length <= 1) {
      isDeleteMode.value = false
    }
  } catch (e: any) {
    alert(e.message || '移除失败')
  }
}

// 判断当前用户是否为群主
const isOwner = computed(() => {
  return userStore.userInfo?.id === chatStore.currentGroupDetail?.ownerId
})
</script>

<template>
  <Teleport to="body">
    <Transition 
      enter-active-class="transition-opacity duration-300 ease-out" 
      enter-from-class="opacity-0" 
      enter-to-class="opacity-100" 
      leave-active-class="transition-opacity duration-200 ease-in" 
      leave-from-class="opacity-100" 
      leave-to-class="opacity-0"
    >
      <div 
        v-if="chatStore.isGroupDetailOpen" 
        class="fixed inset-0 z-[100] bg-slate-900/40 backdrop-blur-sm" 
        @click="chatStore.closeGroupDetailModal"
      ></div>
    </Transition>

    <Transition
      enter-active-class="transition duration-400 ease-out-cubic"
      enter-from-class="translate-x-full"
      enter-to-class="translate-x-0"
      leave-active-class="transition duration-300 ease-in-cubic"
      leave-from-class="translate-x-0"
      leave-to-class="translate-x-full"
    >
      <div 
        v-if="chatStore.isGroupDetailOpen" 
        class="fixed top-0 right-0 z-[101] h-full w-full max-w-sm bg-white shadow-2xl flex flex-col"
      >
        <div class="flex items-center justify-between p-5 border-b border-slate-100 shrink-0">
          <h3 class="text-lg font-bold text-vibrant-main">群聊设置</h3>
          <button @click="chatStore.closeGroupDetailModal" class="p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600 rounded-full transition-all">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
          </button>
        </div>

        <div v-if="chatStore.isLoadingGroupDetail" class="flex-1 flex items-center justify-center">
          <svg class="animate-spin h-8 w-8 text-brand-blue" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
        </div>

        <div v-else-if="chatStore.currentGroupDetail" class="flex-1 overflow-y-auto p-6 space-y-6">
          
          <div class="flex items-center gap-4">
            <img :src="formatImageUrl(chatStore.currentGroupDetail.avatar)" class="h-16 w-16 rounded-2xl bg-slate-100" />
            <div class="flex-1 min-w-0">
              <div v-if="!isEditingName" class="flex items-center gap-2">
                <h2 class="text-xl font-bold truncate" :title="chatStore.currentGroupDetail.name">
                  {{ chatStore.currentGroupDetail.name }}
                </h2>
                <button v-if="isOwner" @click="startEdit" class="p-1 text-slate-400 hover:text-brand-blue transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
                </button>
              </div>
              
              <div v-else class="flex items-center gap-2">
                 <input 
                   v-model="newGroupName"
                   type="text" 
                   class="flex-1 h-9 px-3 rounded-lg bg-slate-100 border border-brand-blue/50 ring-2 ring-brand-blue/10 outline-none"
                 />
                 <button @click="confirmEdit" class="p-2 text-functional-green hover:bg-green-100 rounded-lg">
                   <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                 </button>
                 <button @click="isEditingName = false" class="p-2 text-functional-red hover:bg-red-100 rounded-lg">
                   <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
                 </button>
              </div>

              <div v-if="editError" class="text-xs text-functional-red mt-1">{{ editError }}</div>
              
              <div class="text-sm text-slate-500 mt-1">
                {{ chatStore.currentGroupDetail.memberCount }} 名成员
              </div>
            </div>
          </div>

          <div>
            <h4 class="text-sm font-bold text-vibrant-muted mb-3">群成员</h4>
            <div class="grid grid-cols-5 gap-4">
              <!-- Add Member Button -->
              <div 
                @click="showAddMemberModal = true"
                class="flex flex-col items-center gap-2 text-center cursor-pointer group"
              >
                <div class="h-12 w-12 rounded-full border-2 border-dashed border-slate-300 flex items-center justify-center text-slate-400 group-hover:border-brand-blue group-hover:text-brand-blue transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                </div>
                <span class="text-xs text-slate-400 group-hover:text-brand-blue">邀请</span>
              </div>

              <!-- Remove Member Button (Owner Only) -->
              <div 
                v-if="isOwner"
                @click="toggleDeleteMode"
                class="flex flex-col items-center gap-2 text-center cursor-pointer group"
              >
                <div class="h-12 w-12 rounded-full border-2 border-dashed border-slate-300 flex items-center justify-center text-slate-400 group-hover:border-functional-red group-hover:text-functional-red transition-colors" :class="{ 'bg-functional-red/10 border-functional-red text-functional-red': isDeleteMode }">
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                </div>
                <span class="text-xs text-slate-400 group-hover:text-functional-red">{{ isDeleteMode ? '完成' : '移除' }}</span>
              </div>

              <!-- Members -->
              <div 
                v-for="member in chatStore.currentGroupMembers" 
                :key="member.uid" 
                class="flex flex-col items-center gap-2 text-center relative group"
              >
                <div class="relative">
                  <img :src="formatImageUrl(member.avatar)" class="h-12 w-12 rounded-full bg-slate-100" />
                  
                  <!-- Owner Badge -->
                  <span v-if="member.isOwner === 1" class="absolute -bottom-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-functional-amber text-white ring-2 ring-white" title="群主">
                    <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><path d="M12 1L9 9H1l6.91 5.59L4.5 23l7.5-5.82L19.5 23l-3.41-8.41L23 9h-8L12 1z"/></svg>
                  </span>

                  <!-- Delete Badge (Delete Mode & Not Self) -->
                  <button 
                    v-if="isDeleteMode && String(member.uid) !== String(userStore.userInfo?.id)"
                    @click="handleKickMember(String(member.uid))"
                    class="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-functional-red text-white flex items-center justify-center ring-2 ring-white hover:scale-110 transition-transform shadow-sm"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                  </button>
                </div>
                <span class="text-xs text-slate-500 w-full truncate">{{ member.username }}</span>
              </div>
            </div>
          </div>

        </div>

        <div class="p-6 border-t border-slate-100 shrink-0">
          <button 
            @click="handleQuitGroup"
            class="w-full h-11 bg-functional-red/10 text-functional-red font-bold rounded-xl hover:bg-functional-red hover:text-white transition-all"
          >
            退出群聊
          </button>
        </div>

      </div>
    </Transition>

    <!-- Contact Picker for Inviting Members -->
    <ContactPicker
      :is-open="showAddMemberModal"
      mode="friends"
      :exclude-ids="chatStore.currentGroupMembers.map(m => String(m.uid))"
      @close="showAddMemberModal = false"
      @invite="handleInviteMember"
    />
  </Teleport>
</template>

<style scoped>
/* 自定义滚动条 */
.overflow-y-auto::-webkit-scrollbar {
  width: 5px;
}
.overflow-y-auto::-webkit-scrollbar-track {
  background: transparent;
}
.overflow-y-auto::-webkit-scrollbar-thumb {
  @apply bg-slate-200 rounded-full hover:bg-slate-300;
}
</style>