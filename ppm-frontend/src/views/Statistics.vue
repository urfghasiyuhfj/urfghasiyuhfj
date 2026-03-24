<template>
  <div class="statistics-page">
    <!-- 1. 总体PPM每月变化趋势（公司总体 + 四大基地） -->
    <el-card class="card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">PPM 月度变化趋势</span>
          <el-form :inline="true" class="header-form">
            <el-form-item label="显示月数">
              <el-select v-model="trendMonths" style="width: 100px" @change="loadTrendData">
                <el-option :value="6" label="最近 6 月" />
                <el-option :value="12" label="最近 12 月" />
                <el-option :value="24" label="最近 24 月" />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
      </template>
      <div ref="chartTrendRef" class="chart chart-large"></div>
      <el-table :data="trendTableData" stripe size="small" class="trend-table">
        <el-table-column prop="month" label="月份" width="100">
          <template #default="{ row }">{{ formatMonth(row.month) }}</template>
        </el-table-column>
        <el-table-column prop="companyPpm" label="公司总体 PPM" width="120">
          <template #default="{ row }">{{ row.companyPpm != null ? Number(row.companyPpm).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column v-for="base in baseNames" :key="base" :prop="base" :label="base + ' PPM'" width="100">
          <template #default="{ row }">
            {{ row.baseData && row.baseData[base] != null ? Number(row.baseData[base]).toFixed(2) : '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 2. 可疑物料分析（失效模式 + 故障类别） -->
    <el-card class="card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">可疑物料分析</span>
          <el-form :inline="true" class="header-form">
            <el-form-item label="筛选范围">
              <el-radio-group v-model="statsFilterMode" @change="loadSuspiciousStats">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="month">指定月份</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="statsFilterMode === 'month'" label="月份">
              <el-date-picker
                v-model="statsMonth"
                type="month"
                placeholder="选择月份"
                value-format="YYYYMM"
                @change="loadSuspiciousStats"
              />
            </el-form-item>
          </el-form>
        </div>
      </template>
      <el-row :gutter="24">
        <el-col :xs="24" :lg="12">
          <div class="pie-section">
            <div class="section-title">故障类别分布</div>
            <div ref="chartFaultTypeRef" class="chart chart-pie"></div>
            <el-table :data="faultTypeTableData" stripe size="small" max-height="200">
              <el-table-column prop="name" label="故障类别" min-width="120" />
              <el-table-column prop="value" label="数量" width="80" />
              <el-table-column label="占比" width="80">
                <template #default="{ row }">{{ calcPercent(row.value, statsTotalCount) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
        <el-col :xs="24" :lg="12">
          <div class="pie-section">
            <div class="section-title">失效模式分布</div>
            <div ref="chartFailureDescRef" class="chart chart-pie"></div>
            <el-table :data="failureDescTableData" stripe size="small" max-height="200">
              <el-table-column prop="name" label="失效模式" min-width="120">
                <template #default="{ row }">{{ (row.name || '').slice(0, 30) }}</template>
              </el-table-column>
              <el-table-column prop="value" label="数量" width="80" />
              <el-table-column label="占比" width="80">
                <template #default="{ row }">{{ calcPercent(row.value, statsTotalCount) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 3. 各供应商PPM展示 -->
    <el-card class="card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">供应商 PPM 明细</span>
          <el-form :inline="true" class="header-form">
            <el-form-item label="月份">
              <el-select v-model="supplierMonth" placeholder="选择月份" style="width: 140px" @change="loadSupplierPpm">
                <el-option v-for="m in availableMonths" :key="m" :label="formatMonth(m)" :value="m" />
              </el-select>
            </el-form-item>
            <el-form-item label="供应商">
              <el-input
                v-model="supplierSearchKey"
                placeholder="搜索供应商名称/代码"
                clearable
                style="width: 200px"
                @input="filterSupplierList"
              />
            </el-form-item>
          </el-form>
        </div>
      </template>
      <div ref="chartSupplierPpmRef" class="chart chart-bar"></div>
      <el-table :data="supplierPpmFiltered" stripe size="small" class="supplier-table">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="150">
          <template #default="{ row }">
            <span :title="row.supplierName">{{ (row.supplierName || '').slice(0, 20) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="supplierCode" label="供应商代码" width="120" />
        <el-table-column prop="defectCount" label="不合格数" width="100" />
        <el-table-column prop="supplyQty" label="供货量" width="100" />
        <el-table-column prop="ppm" label="PPM" width="100" sortable>
          <template #default="{ row }">
            <span :class="getPpmClass(row.ppm)">{{ row.ppm != null ? Number(row.ppm).toFixed(2) : '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="supplierPage"
        :page-size="20"
        :total="supplierPpmFilteredTotal"
        layout="total, prev, pager, next"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { getPpmTrend, getPpmListByMonth, getAvailableMonths } from '@/api/ppm'
import { getSuspiciousMaterialStats } from '@/api/queryApi'
import dayjs from 'dayjs'

// ============ 工具函数 ============
function formatMonth(ym) {
  if (!ym || ym.length < 6) return ym
  return ym.slice(0, 4) + '-' + ym.slice(4)
}

function calcPercent(value, total) {
  if (!total || total === 0) return '0%'
  return (value / total * 100).toFixed(1) + '%'
}

function getPpmClass(ppm) {
  if (ppm == null) return ''
  const v = Number(ppm)
  if (v >= 1000) return 'ppm-high'
  if (v >= 500) return 'ppm-medium'
  return 'ppm-low'
}

// ============ 1. PPM趋势数据 ============
const trendMonths = ref(12)
const trendData = ref(null)
const baseNames = ref(['河西', '宝骏', '青岛', '重庆'])
const chartTrendRef = ref(null)
let chartTrend = null

const trendTableData = computed(() => {
  const data = trendData.value
  if (!data?.months?.length) return []
  
  return data.months.map((m, idx) => {
    const row = {
      month: m,
      companyPpm: data.avgPpmByMonth?.[idx] ?? null,
      baseData: {}
    }
    data.byBase?.forEach(b => {
      row.baseData[b.baseName] = b.ppmValues?.[idx] ?? null
    })
    return row
  })
})

async function loadTrendData() {
  try {
    trendData.value = await getPpmTrend(trendMonths.value)
    renderTrendChart()
  } catch (e) {
    console.error('加载趋势数据失败', e)
    trendData.value = null
  }
}

function renderTrendChart() {
  const data = trendData.value
  if (!chartTrendRef.value) return
  
  chartTrend?.dispose()
  
  if (!data?.months?.length) {
    chartTrend = echarts.init(chartTrendRef.value)
    chartTrend.setOption({ title: { text: '暂无数据', left: 'center', top: 'center' } })
    return
  }
  
  chartTrend = echarts.init(chartTrendRef.value)
  const xLabels = data.months.map(m => formatMonth(m))
  const colors = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444']
  
  const series = []
  // 公司总体
  if (data.avgPpmByMonth?.length) {
    series.push({
      name: '公司总体',
      type: 'bar',
      data: data.avgPpmByMonth.map(v => v != null ? Number(v) : 0),
      itemStyle: { color: colors[0] },
      barGap: '10%'
    })
  }
  // 四大基地
  data.byBase?.forEach((b, i) => {
    series.push({
      name: b.baseName,
      type: 'bar',
      data: b.ppmValues?.map(v => v != null ? Number(v) : 0) ?? [],
      itemStyle: { color: colors[(i + 1) % colors.length] },
      barGap: '10%'
    })
  })
  
  chartTrend.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: function(params) {
        let html = `<div style="font-weight:bold;margin-bottom:4px;">${params[0].axisValue}</div>`
        params.forEach(p => {
          html += `<div style="display:flex;justify-content:space-between;gap:20px;">
            <span>${p.marker} ${p.seriesName}</span>
            <span style="font-weight:bold;">${p.value?.toFixed(2) ?? '-'}</span>
          </div>`
        })
        return html
      }
    },
    legend: {
      data: series.map(s => s.name),
      top: 0,
      type: 'scroll'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xLabels,
      axisLabel: { rotate: 30 }
    },
    yAxis: {
      type: 'value',
      name: 'PPM'
    },
    series
  })
}

// ============ 2. 可疑物料分析 ============
const statsFilterMode = ref('all')
const statsMonth = ref(dayjs().format('YYYYMM'))
const statsData = ref({ byFaultType: [], byFailureDesc: [] })
const statsTotalCount = ref(0)
const chartFaultTypeRef = ref(null)
const chartFailureDescRef = ref(null)
let chartFaultType = null
let chartFailureDesc = null

const faultTypeTableData = computed(() => {
  return statsData.value.byFaultType?.slice(0, 10) ?? []
})

const failureDescTableData = computed(() => {
  return statsData.value.byFailureDesc?.slice(0, 10) ?? []
})

function buildStatsParams() {
  if (statsFilterMode.value === 'all') return {}
  const m = statsMonth.value
  if (!m) return {}
  const y = m.slice(0, 4)
  const mon = m.slice(4, 6)
  return {
    recordDateFrom: `${y}-${mon}-01`,
    recordDateTo: dayjs(`${y}-${mon}-01`).endOf('month').format('YYYY-MM-DD')
  }
}

async function loadSuspiciousStats() {
  try {
    const res = await getSuspiciousMaterialStats(buildStatsParams())
    statsData.value = {
      byFaultType: res?.byFaultType ?? [],
      byFailureDesc: res?.byFailureDesc ?? []
    }
    statsTotalCount.value = res?.totalCount ?? 0
    await nextTick()
    renderPieCharts()
  } catch (e) {
    console.error('加载可疑物料统计失败', e)
  }
}

function renderPieCharts() {
  const pieColors = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4', '#84cc16', '#f97316']
  
  // 故障类别饼图
  if (chartFaultTypeRef.value) {
    chartFaultType?.dispose()
    const byFaultType = statsData.value.byFaultType ?? []
    
    if (!byFaultType.length) {
      chartFaultType = echarts.init(chartFaultTypeRef.value)
      chartFaultType.setOption({ title: { text: '暂无数据', left: 'center', top: 'center' } })
    } else {
      chartFaultType = echarts.init(chartFaultTypeRef.value)
      const sorted = [...byFaultType].sort((a, b) => (b.value || 0) - (a.value || 0))
      chartFaultType.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        series: [{
          type: 'pie',
          radius: ['35%', '65%'],
          center: ['50%', '50%'],
          data: sorted.map((d, i) => ({
            name: d.name || '未分类',
            value: d.value,
            itemStyle: { color: pieColors[i % pieColors.length] }
          })),
          label: {
            show: true,
            formatter: '{b}\n{d}%',
            fontSize: 11
          },
          labelLine: {
            show: true,
            length: 10,
            length2: 10
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.3)'
            }
          }
        }]
      })
    }
  }
  
  // 失效模式饼图
  if (chartFailureDescRef.value) {
    chartFailureDesc?.dispose()
    const byFailureDesc = statsData.value.byFailureDesc ?? []
    
    if (!byFailureDesc.length) {
      chartFailureDesc = echarts.init(chartFailureDescRef.value)
      chartFailureDesc.setOption({ title: { text: '暂无数据', left: 'center', top: 'center' } })
    } else {
      chartFailureDesc = echarts.init(chartFailureDescRef.value)
      const sorted = [...byFailureDesc].sort((a, b) => (b.value || 0) - (a.value || 0)).slice(0, 15)
      chartFailureDesc.setOption({
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        series: [{
          type: 'pie',
          radius: ['35%', '65%'],
          center: ['50%', '50%'],
          data: sorted.map((d, i) => ({
            name: (d.name || '未分类').slice(0, 15),
            value: d.value,
            itemStyle: { color: pieColors[i % pieColors.length] }
          })),
          label: {
            show: true,
            formatter: '{b}\n{d}%',
            fontSize: 10
          },
          labelLine: {
            show: true,
            length: 8,
            length2: 8
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.3)'
            }
          }
        }]
      })
    }
  }
}

// ============ 3. 供应商PPM展示 ============
const supplierMonth = ref('')
const availableMonths = ref([])
const supplierPpmList = ref([])
const supplierSearchKey = ref('')
const supplierPage = ref(1)
const chartSupplierPpmRef = ref(null)
let chartSupplierPpm = null

const supplierPpmFiltered = computed(() => {
  let list = supplierPpmList.value
  const key = supplierSearchKey.value?.trim().toLowerCase()
  if (key) {
    list = list.filter(s =>
      (s.supplierName || '').toLowerCase().includes(key) ||
      (s.supplierCode || '').toLowerCase().includes(key)
    )
  }
  return list.slice((supplierPage.value - 1) * 20, supplierPage.value * 20)
})

const supplierPpmFilteredTotal = computed(() => {
  let list = supplierPpmList.value
  const key = supplierSearchKey.value?.trim().toLowerCase()
  if (key) {
    list = list.filter(s =>
      (s.supplierName || '').toLowerCase().includes(key) ||
      (s.supplierCode || '').toLowerCase().includes(key)
    )
  }
  return list.length
})

async function loadAvailableMonths() {
  try {
    const res = await getAvailableMonths()
    availableMonths.value = res ?? []
    // 默认选择最新月份
    if (availableMonths.value.length > 0) {
      supplierMonth.value = availableMonths.value[0]
    } else {
      supplierMonth.value = dayjs().format('YYYYMM')
    }
  } catch (e) {
    console.error('加载可用月份失败', e)
    // 降级处理
    supplierMonth.value = dayjs().format('YYYYMM')
  }
}

async function loadSupplierPpm() {
  if (!supplierMonth.value) return
  try {
    const res = await getPpmListByMonth(supplierMonth.value)
    supplierPpmList.value = (res ?? []).sort((a, b) => (Number(b.ppm) || 0) - (Number(a.ppm) || 0))
    supplierPage.value = 1
    renderSupplierChart()
  } catch (e) {
    console.error('加载供应商PPM失败', e)
    supplierPpmList.value = []
  }
}

function filterSupplierList() {
  supplierPage.value = 1
  renderSupplierChart()
}

function renderSupplierChart() {
  if (!chartSupplierPpmRef.value) return
  
  chartSupplierPpm?.dispose()
  
  // 取TOP15展示
  let list = supplierPpmList.value
  const key = supplierSearchKey.value?.trim().toLowerCase()
  if (key) {
    list = list.filter(s =>
      (s.supplierName || '').toLowerCase().includes(key) ||
      (s.supplierCode || '').toLowerCase().includes(key)
    )
  }
  const top15 = list.slice(0, 15)
  
  if (!top15.length) {
    chartSupplierPpm = echarts.init(chartSupplierPpmRef.value)
    chartSupplierPpm.setOption({ title: { text: '暂无数据', left: 'center', top: 'center' } })
    return
  }
  
  chartSupplierPpm = echarts.init(chartSupplierPpmRef.value)
  const labels = top15.map(s => (s.supplierName || s.supplierCode || '').slice(0, 10))
  const values = top15.map(s => Number(s.ppm) || 0)
  
  chartSupplierPpm.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: function(params) {
        const d = top15[params[0].dataIndex]
        return `<div style="font-weight:bold;margin-bottom:4px;">${d.supplierName || d.supplierCode}</div>
          <div>PPM: <b>${d.ppm?.toFixed(2) ?? '-'}</b></div>
          <div>不合格数: ${d.defectCount ?? '-'}</div>
          <div>供货量: ${d.supplyQty ?? '-'}</div>`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '8%',
      top: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { rotate: 30, fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: 'PPM'
    },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: {
        color: function(params) {
          const v = params.value
          if (v >= 1000) return '#ef4444'
          if (v >= 500) return '#f59e0b'
          return '#10b981'
        }
      },
      barWidth: '60%'
    }]
  })
}

// ============ 生命周期 ============
function onResize() {
  chartTrend?.resize()
  chartFaultType?.resize()
  chartFailureDesc?.resize()
  chartSupplierPpm?.resize()
}

onMounted(async () => {
  // 先加载可用月份
  await loadAvailableMonths()
  // 并行加载其他数据
  await Promise.all([
    loadTrendData(),
    loadSuspiciousStats(),
    loadSupplierPpm()
  ])
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chartTrend?.dispose()
  chartFaultType?.dispose()
  chartFailureDesc?.dispose()
  chartSupplierPpm?.dispose()
})
</script>

<style scoped>
.statistics-page {
  max-width: 1400px;
  margin: 0 auto;
}

.card {
  margin-bottom: 24px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.header-form {
  margin-bottom: 0;
}

.header-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 12px;
}

.header-form :deep(.el-form-item:last-child) {
  margin-right: 0;
}

.chart {
  width: 100%;
}

.chart-large {
  height: 380px;
}

.chart-pie {
  height: 280px;
}

.chart-bar {
  height: 300px;
}

.trend-table {
  margin-top: 16px;
}

.pie-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #3b82f6;
}

.supplier-table {
  margin-top: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

/* PPM 颜色标记 */
.ppm-high {
  color: #ef4444;
  font-weight: 600;
}

.ppm-medium {
  color: #f59e0b;
  font-weight: 500;
}

.ppm-low {
  color: #10b981;
}

/* 响应式 */
@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .chart-large {
    height: 300px;
  }
  
  .chart-pie {
    height: 240px;
  }
}
</style>
