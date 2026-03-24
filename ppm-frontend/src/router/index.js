import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/Layout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'import', name: 'Import', component: () => import('@/views/Import.vue'), meta: { title: '数据导入' } },
      { path: 'ppm', name: 'Ppm', component: () => import('@/views/Ppm.vue'), meta: { title: 'PPM 管理' } },
      { path: 'statistics', name: 'Statistics', component: () => import('@/views/Statistics.vue'), meta: { title: '统计分析' } },
      { path: 'query', name: 'Query', component: () => import('@/views/Query.vue'), meta: { title: '综合查询' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  document.title = to.meta?.title ? `${to.meta.title} - PPM 数据分析系统` : 'PPM 数据分析系统'
  next()
})

export default router
