<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { chatService, type GroupMemberVo } from '../../services/chatService'
import { formatImageUrl } from '../../utils/image'
import { useUserStore } from '../../stores/userStore'

const props = defineProps<{
  groupId?: string | number
  excludeIds?: string[]
  isOpen: boolean
  mode?: 'group-members' | 'friends'
}>()

const emit = defineEmits(['close', 'invite'])

const userStore = useUserStore()
const members = ref<GroupMemberVo[]>([])
const loading = ref(false)
const selectedIds = ref<Set<string>>(new Set())
const searchQuery = ref('')

const fetchMembers = async () => {
  loading.value = true
  try {
    if (props.mode === 'friends') {
      // Fetch friends
      const res = await chatService.getFriendList()
      // Flatten FriendContentVo[] -> GroupMemberVo[]
      const allFriends: GroupMemberVo[] = []
      res.forEach(group => {
        group.content.forEach(friend => {
          allFriends.push({
            uid: friend.uid,
            username: friend.name,
            avatar: friend.avatar,
            isOwner: 0
          })
        })
      })
      members.value = allFriends
    } else {
      // Fetch group members
      if (!props.groupId) return
      const res = await chatService.getGroupMembers(Number(props.groupId))
      members.value = res
    }
  } catch (e) {
    console.error('Failed to fetch members', e)
  } finally {
    loading.value = false
  }
}

watch(() => props.isOpen, (val) => {
  if (val) {
    fetchMembers()
  }
})

onMounted(() => {
  if (props.isOpen) {
    fetchMembers()
  }
})

const filteredMembers = computed(() => {
  return members.value.filter(m => {
    // Exclude self
    if (String(m.uid) === String(userStore.userInfo?.id)) return false
    // Exclude already in call/group (if passed)
    if (props.excludeIds?.includes(String(m.uid))) return false
    
    // For 'friends' mode, we might want to exclude friends who are ALREADY in the group
    // But excludeIds handles that if passed correctly.
    
    // Filter by search
    if (searchQuery.value && !m.username.toLowerCase().includes(searchQuery.value.toLowerCase())) return false
    return true
  })
})

const toggleSelection = (id: string) => {
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
  } else {
    selectedIds.value.add(id)
  }
}

const handleInvite = () => {
  const selectedUsers = members.value
    .filter(m => selectedIds.value.has(String(m.uid)))
    .map(m => ({
      id: String(m.uid),
      name: m.username,
      avatar: m.avatar || `https://api.dicebear.com/7.x/initials/svg?seed=${m.username}`
    }))
  emit('invite', selectedUsers)
  emit('close')
}
</script>

<template>
  <div v-if="isOpen" class="fixed inset-0 z-[10000] flex items-center justify-center bg-black/50 backdrop-blur-sm">
    <div class="bg-white dark:bg-slate-800 w-full max-w-md rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[80vh]">
      
      <!-- Header -->
      <div class="p-4 border-b border-slate-100 dark:border-slate-700 flex justify-between items-center">
        <h3 class="text-lg font-bold text-slate-900 dark:text-white">邀请成员</h3>
        <button @click="$emit('close')" class="p-2 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-full transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-slate-500"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
      </div>

      <!-- Search -->
      <div class="p-4 pb-2">
        <div class="relative">
          <input 
            v-model="searchQuery"
            type="text" 
            placeholder="搜索成员..." 
            class="w-full pl-10 pr-4 py-2 bg-slate-100 dark:bg-slate-700/50 border-none rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none text-slate-900 dark:text-white placeholder-slate-400"
          >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="absolute left-3 top-2.5 text-slate-400"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
        </div>
      </div>

      <!-- List -->
      <div class="flex-1 overflow-y-auto p-2 custom-scrollbar">
        <div v-if="loading" class="flex justify-center py-8">
          <div class="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
        </div>
        
        <div v-else-if="filteredMembers.length === 0" class="text-center py-8 text-slate-400 text-sm">
          没有找到可邀请的成员
        </div>

        <div v-else class="space-y-1">
          <div 
            v-for="member in filteredMembers" 
            :key="member.uid"
            @click="toggleSelection(String(member.uid))"
            class="flex items-center gap-3 p-2 rounded-xl cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors"
          >
            <!-- Checkbox -->
            <div 
              class="w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors"
              :class="selectedIds.has(String(member.uid)) ? 'bg-blue-500 border-blue-500' : 'border-slate-300 dark:border-slate-600'"
            >
              <svg v-if="selectedIds.has(String(member.uid))" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" class="text-white"><polyline points="20 6 9 17 4 12"></polyline></svg>
            </div>

            <img :src="formatImageUrl(member.avatar)" class="w-10 h-10 rounded-full object-cover bg-slate-200" />
            <span class="text-sm font-medium text-slate-700 dark:text-slate-200">{{ member.username }}</span>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="p-4 border-t border-slate-100 dark:border-slate-700 flex justify-end gap-3 bg-slate-50 dark:bg-slate-800/50">
        <button 
          @click="$emit('close')"
          class="px-4 py-2 text-sm font-medium text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700 rounded-lg transition-colors"
        >
          取消
        </button>
        <button 
          @click="handleInvite"
          :disabled="selectedIds.size === 0"
          class="px-6 py-2 text-sm font-bold text-white bg-blue-500 hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg shadow-lg shadow-blue-500/20 transition-all active:scale-95"
        >
          邀请 ({{ selectedIds.size }})
        </button>
      </div>

    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.5);
  border-radius: 10px;
}
</style>
