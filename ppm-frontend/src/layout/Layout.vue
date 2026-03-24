<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">PPM 数据分析</div>
      <el-menu
        :default-active="activeMenu"
        router
        class="menu"
        background-color="#1a1d24"
        text-color="#a0aec0"
        active-text-color="#38bdf8"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/import">
          <el-icon><Upload /></el-icon>
          <span>数据导入</span>
        </el-menu-item>
        <el-menu-item index="/ppm">
          <el-icon><DataLine /></el-icon>
          <span>PPM 管理</span>
        </el-menu-item>
        <el-menu-item index="/statistics">
          <el-icon><PieChart /></el-icon>
          <span>统计与分析</span>
        </el-menu-item>
        <el-menu-item index="/query">
          <el-icon><Search /></el-icon>
          <span>综合查询</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="main-wrap">
      <el-header class="header">
        <span class="title">{{ pageTitle }}</span>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Monitor, Upload, DataLine, PieChart, Search } from '@element-plus/icons-vue'

const route = useRoute()

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => route.meta?.title ?? 'PPM 数据分析系统')
</script>

<style scoped>
.layout {
  height: 100vh;
  overflow: hidden;
}

.aside {
  background: #1a1d24;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
}

.logo {
  height: 56px;
  line-height: 56px;
  padding-left: 20px;
  font-size: 16px;
  font-weight: 600;
  color: #e2e8f0;
  border-bottom: 1px solid #2d3748;
}

.menu {
  border-right: none;
}

.main-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #f1f5f9;
}

.header {
  height: 56px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.main {
  flex: 1;
  padding: 24px;
  overflow: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
