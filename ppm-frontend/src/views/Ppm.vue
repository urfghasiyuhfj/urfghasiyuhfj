<template>
  <div class="ppm-page">
    <el-card class="card" shadow="never">
      <template #header>
        <span>筛选与计算</span>
      </template>
      <el-form :inline="true" :model="form" class="form">
        <el-form-item label="PPM 月份">
          <el-date-picker
            v-model="form.month"
            type="month"
            placeholder="选择月份"
            value-format="YYYYMM"
            clearable
          />
        </el-form-item>
        <el-form-item label="供应商编码">
          <el-input v-model="form.supplierCode" placeholder="支持模糊" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="供应商名称">
          <el-input v-model="form.supplierName" placeholder="支持模糊" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading.query" @click="fetch">查询</el-button>
          <el-button type="success" :loading="loading.calc" :disabled="!form.month" @click="calculate">
            触发计算
          </el-button>
          <el-button type="warning" :disabled="!form.month" @click="handleExport">导出 Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="card" shadow="never">
      <template #header>
        <span>PPM 汇总</span>
      </template>
      <el-table
        v-loading="loading.table"
        :data="tableData.list"
        stripe
        border
        style="width: 100%"
        max-height="520"
      >
        <el-table-column prop="ppmMonth" label="月份" width="100" />
        <el-table-column prop="supplierCode" label="供应商编码" width="120" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="defectCount" label="不合格数" width="100" align="right" />
        <el-table-column prop="supplyQty" label="供货量" width="100" align="right" />
        <el-table-column prop="ppm" label="PPM" width="120" align="right">
          <template #default="{ row }">
            {{ row.ppm != null ? Number(row.ppm).toFixed(2) : '-' }}
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="form.page"
        v-model:page-size="form.size"
        :page-sizes="[10, 20, 50]"
        :total="tableData.total"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="fetch"
        @current-change="fetch"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPpmSummary, calculatePpm } from '@/api/ppm'
import { exportPpmSummary } from '@/api/exportApi'
const loading = reactive({ query: false, calc: false, table: false })
const form = reactive({
  month: '',
  supplierCode: '',
  supplierName: '',
  page: 1,
  size: 20,
})

const tableData = reactive({ list: [], total: 0 })

async function fetch() {
  loading.query = true
  loading.table = true
  try {
    const params = {
      page: form.page,
      size: form.size,
      ppmMonth: form.month || undefined,
      supplierCode: form.supplierCode || undefined,
      supplierName: form.supplierName || undefined,
    }
    const res = await getPpmSummary(params)
    tableData.list = res?.list ?? []
    tableData.total = res?.total ?? 0
  } finally {
    loading.query = false
    loading.table = false
  }
}

function handleExport() {
  if (!form.month) return
  exportPpmSummary({ ppmMonth: form.month, supplierCode: form.supplierCode, supplierName: form.supplierName })
}

async function calculate() {
  loading.calc = true
  try {
    const res = await calculatePpm(form.month)
    ElMessage.success(`计算完成，写入 ${res?.savedCount ?? 0} 条`)
    if (res?.hasDifferences && res?.differences?.length) {
      const fmt = (v) => (v != null ? Number(v).toFixed(2) : '-')
      const lines = res.differences.slice(0, 5).map(
        (d) => `  ${d.baseName}-${d.supplierName}：PPM ${fmt(d.oldPpm)} → ${fmt(d.newPpm)}`,
      )
      ElMessageBox.alert(
        `以下 ${res.differences.length} 项与库中数据不一致：\n${lines.join('\n')}${res.differences.length > 5 ? `\n  ... 等共 ${res.differences.length} 项` : ''}`,
        'PPM 数据差异提醒',
        { type: 'warning', confirmButtonText: '知道了' },
      )
    }
    await fetch()
  } finally {
    loading.calc = false
  }
}

onMounted(async () => {
  await fetch()
})
</script>

<style scoped>
.ppm-page {
  max-width: 1200px;
}

.card {
  margin-bottom: 24px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.form {
  margin-bottom: 0;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
