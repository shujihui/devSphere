<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from '../components/chat/Sidebar.vue'

const route = useRoute()

// Determine active tab based on route
const activeTab = computed(() => {
  if (route.path.startsWith('/moments')) return 'moments'
  if (route.path.startsWith('/profile')) return 'settings' // Highlight settings for profile for now, or add 'profile' tab
  if (route.path.startsWith('/chat')) {
    return route.query.tab === 'contacts' ? 'contacts' : 'chat'
  }
  return 'chat'
})
</script>

<template>
  <div class="flex h-full w-full overflow-hidden bg-[#F5F7FA] dark:bg-slate-900 text-slate-900 dark:text-white transition-colors duration-300 font-sans">
    <!-- Global Sidebar -->
    <Sidebar :active-tab="activeTab" class="shrink-0" />

    <!-- Main Content Area -->
    <div class="flex-1 h-full overflow-hidden relative">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <keep-alive include="ChatView"> 
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </transition>
      </router-view>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
