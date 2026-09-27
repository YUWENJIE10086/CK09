<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore } from '@/store/tagsView'
import type { TagView } from '@/store/tagsView'

const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()

/** 右键菜单状态 */
const contextMenuVisible = ref(false)
const contextMenuLeft = ref(0)
const contextMenuTop = ref(0)
const selectedTag = ref<TagView | null>(null)

/** 标签栏滚动容器 */
const scrollContainer = ref<HTMLElement | null>(null)

/** 监听路由变化，添加标签 */
watch(
  () => route.path,
  () => {
    if (route.name && route.meta?.title) {
      tagsViewStore.addView(route)
    }
  },
  { immediate: true }
)

/** 当前激活的标签路径 */
const activeTag = computed(() => route.path)

/** 点击标签切换路由 */
const handleTagClick = (tag: TagView) => {
  router.push({ path: tag.path, query: tag.query })
}

/** 关闭标签 */
const handleClose = (tag: TagView) => {
  tagsViewStore.removeView(tag.path)
  // 如果关闭的是当前标签，则跳转到最后一个标签
  if (tag.path === route.path) {
    const views = tagsViewStore.visitedViews
    if (views.length > 0) {
      router.push(views[views.length - 1].path)
    } else {
      router.push('/dashboard')
    }
  }
}

/** 右键菜单 */
const handleContextMenu = (e: MouseEvent, tag: TagView) => {
  e.preventDefault()
  selectedTag.value = tag
  contextMenuLeft.value = e.clientX
  contextMenuTop.value = e.clientY
  contextMenuVisible.value = true
}

/** 关闭右键菜单 */
const closeContextMenu = () => {
  contextMenuVisible.value = false
}

/** 关闭当前标签 */
const closeCurrentTag = () => {
  if (selectedTag.value) {
    handleClose(selectedTag.value)
  }
  closeContextMenu()
}

/** 关闭其他标签 */
const closeOtherTags = () => {
  if (selectedTag.value) {
    tagsViewStore.removeOtherViews(selectedTag.value.path)
    router.push(selectedTag.value.path)
  }
  closeContextMenu()
}

/** 关闭所有标签 */
const closeAllTags = () => {
  tagsViewStore.removeAllViews()
  router.push('/dashboard')
  closeContextMenu()
}

/** 点击其他区域关闭右键菜单 */
document.addEventListener('click', closeContextMenu)
</script>

<template>
  <div class="tags-view" ref="scrollContainer">
    <div class="tags-view-wrapper">
      <div
        v-for="tag in tagsViewStore.visitedViews"
        :key="tag.path"
        class="tags-view-item"
        :class="{ active: tag.path === activeTag }"
        @click="handleTagClick(tag)"
        @contextmenu="handleContextMenu($event, tag)"
      >
        <span class="tag-title">{{ tag.title }}</span>
        <el-icon
          class="tag-close"
          :size="12"
          @click.stop="handleClose(tag)"
        >
          <Close />
        </el-icon>
      </div>
    </div>

    <!-- 右键菜单 -->
    <transition name="fade">
      <div
        v-if="contextMenuVisible"
        class="context-menu"
        :style="{ left: contextMenuLeft + 'px', top: contextMenuTop + 'px' }"
      >
        <div class="context-menu-item" @click="closeCurrentTag">
          <el-icon :size="14"><Close /></el-icon>
          关闭当前
        </div>
        <div class="context-menu-item" @click="closeOtherTags">
          <el-icon :size="14"><SemiSelect /></el-icon>
          关闭其他
        </div>
        <div class="context-menu-item" @click="closeAllTags">
          <el-icon :size="14"><CircleClose /></el-icon>
          关闭所有
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.tags-view {
  height: 38px;
  background: #ffffff;
  border-bottom: 1px solid var(--border-light);
  padding: 0 12px;
  display: flex;
  align-items: center;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  flex-shrink: 0;
}

/* 标签栏滚动条 */
.tags-view::-webkit-scrollbar {
  height: 0;
}

.tags-view-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tags-view-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 14px;
  font-size: 13px;
  color: var(--text-muted);
  background: var(--surface-hover);
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  white-space: nowrap;
  flex-shrink: 0;
  position: relative;
  font-weight: 400;
  letter-spacing: 0.01em;
}

.tags-view-item:hover {
  color: var(--el-color-primary);
  background: rgba(26, 107, 79, 0.06);
  border-color: rgba(26, 107, 79, 0.15);
}

.tags-view-item.active {
  color: var(--sidebar-active-text);
  background: var(--sidebar-active-bg);
  border-color: rgba(26, 107, 79, 0.2);
  font-weight: 500;
}

/* 激活标签底部指示线 */
.tags-view-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 2px;
  background: #2E8B6A;
  border-radius: 1px;
}

.tag-close {
  border-radius: 50%;
  transition: all 0.15s ease;
  margin-right: -2px;
}

.tag-close:hover {
  background: rgba(0, 0, 0, 0.08);
  color: var(--text-primary);
}

.tags-view-item.active .tag-close:hover {
  background: rgba(46, 139, 106, 0.2);
  color: var(--sidebar-active-text);
}

/* 右键菜单 */
.context-menu {
  position: fixed;
  z-index: 9999;
  background: #ffffff;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-color);
  padding: 4px;
  min-width: 140px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s ease;
}

.context-menu-item:hover {
  background: var(--surface-hover);
  color: var(--el-color-primary);
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
