  <template>
  <div class="dashboard">
    <div class="welcome">
      <h1>PPM 数据分析系统</h1>
      <p>供应商不合格品率（PPM）数据导入、计算、统计与综合查询</p>
    </div>
    <el-row :gutter="24" class="cards">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="card" @click="$router.push('/import')">
          <div class="card-inner">
            <el-icon class="icon" color="#38bdf8"><Upload /></el-icon>
            <div class="txt">
              <div class="label">数据导入</div>
              <div class="desc">Excel 导入供应商 PPM、可疑物料、供货量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="card" @click="$router.push('/ppm')">
          <div class="card-inner">
            <el-icon class="icon" color="#34d399"><DataLine /></el-icon>
            <div class="txt">
              <div class="label">PPM 管理</div>
              <div class="desc">计算 PPM、查询与结果管理</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="card" @click="$router.push('/statistics')">
          <div class="card-inner">
            <el-icon class="icon" color="#f59e0b"><PieChart /></el-icon>
            <div class="txt">
              <div class="label">统计与分析</div>
              <div class="desc">按月份、基地、供应商统计分析</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="card" @click="$router.push('/query')">
          <div class="card-inner">
            <el-icon class="icon" color="#a78bfa"><Search /></el-icon>
            <div class="txt">
              <div class="label">综合查询</div>
              <div class="desc">可疑物料、供货量多条件查询</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="stats-card" shadow="never">
      <template #header>
        <span>可疑物料统计</span>
        <span class="total">共 {{ stats.totalCount }} 条</span>
      </template>
      <el-form :inline="true" class="form">
        <el-form-item label="范围">
          <el-radio-group v-model="filterMode" @change="onFilterChange">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="month">指定月份</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="filterMode === 'month'" label="月份">
          <el-date-picker
            v-model="filterMonth"
            type="month"
            placeholder="选择月份"
            value-format="YYYYMM"
            @change="loadStats"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loadingStats" @click="loadStats">查询</el-button>
        </el-form-item>
      </el-form>
      <el-row :gutter="24" class="chart-row">
        <el-col :xs="24" :lg="12">
          <div class="chart-wrap">
            <div class="chart-title">按故障类别</div>
            <div ref="chartFaultRef" class="chart"></div>
          </div>
        </el-col>
        <el-col :xs="24" :lg="12">
          <div class="chart-wrap">
            <div class="chart-title">按区域工厂</div>
            <div ref="chartPlantRef" class="chart"></div>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="24" class="chart-row">
        <el-col :xs="24" :lg="showMonthTrend ? 12 : 24">
          <div class="chart-wrap">
            <div class="chart-title">按供应商 TOP 15</div>
            <div ref="chartSupplierRef" class="chart"></div>
          </div>
        </el-col>
        <el-col v-if="showMonthTrend" :xs="24" :lg="12">
          <div class="chart-wrap">
            <div class="chart-title">按月份趋势</div>
            <div ref="chartMonthRef" class="chart"></div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { Upload, DataLine, PieChart, Search } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getSuspiciousMaterialStats } from '@/api/queryApi'
import dayjs from 'dayjs'

const filterMode = ref('all')
const filterMonth = ref(dayjs().format('YYYYMM'))
const loadingStats = ref(false)
const stats = reactive({
  byPlant: [],
  byFaultType: [],
  bySupplier: [],
  byMonth: [],
  totalCount: 0,
})

const chartFaultRef = ref(null)
const chartPlantRef = ref(null)
const chartSupplierRef = ref(null)
const chartMonthRef = ref(null)

let chartFault = null
let chartPlant = null
let chartSupplier = null
let chartMonth = null

const showMonthTrend = computed(() => filterMode.value === 'all' && stats.byMonth?.length > 0)

function buildParams() {
  if (filterMode.value === 'all') return {}
  const m = filterMonth.value
  if (!m) return {}
  const y = m.slice(0, 4)
  const mon = m.slice(4, 6)
  return {
    recordDateFrom: `${y}-${mon}-01`,
    recordDateTo: dayjs(`${y}-${mon}-01`).endOf('month').format('YYYY-MM-DD'),
  }
}

async function loadStats() {
  loadingStats.value = true
  try {
    const res = await getSuspiciousMaterialStats(buildParams())
    stats.byPlant = res?.byPlant ?? []
    stats.byFaultType = res?.byFaultType ?? []
    stats.bySupplier = res?.bySupplier ?? []
    stats.byMonth = res?.byMonth ?? []
    stats.totalCount = res?.totalCount ?? 0
    await nextTick()
    renderCharts()
    await nextTick()
    onResize()
  } finally {
    loadingStats.value = false
  }
}

function onFilterChange() {
  if (filterMode.value === 'month' && !filterMonth.value) {
    filterMonth.value = dayjs().format('YYYYMM')
  }
  loadStats()
}

function renderCharts() {
  const pieColors = ['#38bdf8', '#34d399', '#f59e0b', '#a78bfa', '#f472b6', '#94a3b8']

  if (!stats.byFaultType?.length) {
    chartFault?.dispose()
    chartFault = null
  } else if (chartFaultRef.value) {
    chartFault?.dispose()
    chartFault = echarts.init(chartFaultRef.value)
    // 按数量降序，仅前 5 名在图表上显示标签，其余悬停时通过 tooltip 展示
    const TOP_FAULT_LABEL = 5
    const sortedFault = [...stats.byFaultType].sort((a, b) => (b.value || 0) - (a.value || 0))
    chartFault.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)',
      },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        label: {
          formatter: '{b}\n{c}',
        },
        labelLine: {
          length: 12,
          length2: 8,
        },
        data: sortedFault.map((d, i) => ({
          name: d.name || '未知',
          value: d.value,
          itemStyle: { color: pieColors[i % pieColors.length] },
          label: { show: i < TOP_FAULT_LABEL },
          labelLine: { show: i < TOP_FAULT_LABEL },
        })),
      }],
    })
  }

  if (!stats.byPlant?.length) {
    chartPlant?.dispose()
    chartPlant = null
  } else if (chartPlantRef.value) {
    chartPlant?.dispose()
    chartPlant = echarts.init(chartPlantRef.value)
    chartPlant.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: stats.byPlant.map((d, i) => ({
          name: d.name,
          value: d.value,
          itemStyle: { color: pieColors[i % pieColors.length] },
        })),
      }],
    })
  }

  if (!stats.bySupplier?.length) {
    chartSupplier?.dispose()
    chartSupplier = null
  } else if (chartSupplierRef.value) {
    chartSupplier?.dispose()
    chartSupplier = echarts.init(chartSupplierRef.value)
    chartSupplier.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '12%', right: '4%', top: '12%', bottom: '22%', containLabel: true },
      xAxis: {
        type: 'category',
        data: stats.bySupplier.map((d) => (d.name || '').slice(0, 12)),
        axisLabel: { rotate: 35, interval: 0, fontSize: 11 },
      },
      yAxis: { type: 'value', name: '数量', axisLabel: { fontSize: 11 } },
      series: [{
        type: 'bar',
        data: stats.bySupplier.map((d) => d.value),
        itemStyle: { color: '#34d399' },
        barMaxWidth: 28,
      }],
    })
  }

  if (!showMonthTrend.value) {
    chartMonth?.dispose()
    chartMonth = null
  } else if (chartMonthRef.value && stats.byMonth?.length) {
    chartMonth?.dispose()
    chartMonth = echarts.init(chartMonthRef.value)
    chartMonth.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '12%', right: '4%', top: '12%', bottom: '22%', containLabel: true },
      xAxis: {
        type: 'category',
        data: stats.byMonth.map((d) => d.name),
        axisLabel: { rotate: 35, interval: 0, fontSize: 11 },
      },
      yAxis: { type: 'value', name: '数量', axisLabel: { fontSize: 11 } },
      series: [{
        type: 'bar',
        data: stats.byMonth.map((d) => d.value),
        itemStyle: { color: '#38bdf8' },
        barMaxWidth: 28,
      }],
    })
  }
}

function onResize() {
  chartFault?.resize()
  chartPlant?.resize()
  chartSupplier?.resize()
  chartMonth?.resize()
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chartFault?.dispose()
  chartPlant?.dispose()
  chartSupplier?.dispose()
  chartMonth?.dispose()
})
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome {
  margin-bottom: 32px;
}

.welcome h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.welcome p {
  color: #64748b;
  margin: 0;
  font-size: 15px;
}

.cards {
  margin-top: 24px;
}

.card {
  cursor: pointer;
  margin-bottom: 24px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.card-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon {
  font-size: 36px;
  flex-shrink: 0;
}

.txt .label {
  font-weight: 600;
  font-size: 16px;
  color: #1e293b;
  margin-bottom: 4px;
}

.txt .desc {
  font-size: 13px;
  color: #64748b;
}

.stats-card {
  margin-top: 24px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.stats-card :deep(.el-card__header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total {
  font-size: 14px;
  color: #64748b;
}

.form {
  margin-bottom: 20px;
}

.chart-row {
  margin-bottom: 16px;
}

.chart-wrap {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  min-height: 320px;
  overflow: hidden;
}

.chart-title {
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
  font-size: 14px;
}

.chart {
  height: 300px;
  width: 100%;
  min-width: 0;
}
</style>
