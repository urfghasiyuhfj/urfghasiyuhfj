<template>
  <div class="query-page">
    <el-tabs v-model="activeTab" type="border-card" class="tabs">
      <el-tab-pane label="可疑物料" name="suspicious">
        <el-card shadow="never" class="card">
          <el-form :inline="true" :model="suspForm" class="form">
            <el-form-item label="区域工厂">
              <el-input v-model="suspForm.plant" placeholder="不填查全部" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="供应商代码">
              <el-input v-model="suspForm.supplierCode" placeholder="不填查全部" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="供应商名称">
              <el-input v-model="suspForm.supplierName" placeholder="不填查全部" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="零件号">
              <el-input v-model="suspForm.partCode" placeholder="不填查全部" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="零件名称">
              <el-input v-model="suspForm.partName" placeholder="不填查全部" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="开单日期">
              <el-date-picker
                v-model="suspForm.orderDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="不选查全部"
                end-placeholder="不选查全部"
                value-format="YYYY-MM-DD"
                clearable
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading.susp" @click="handleSuspiciousQuery">查询</el-button>
              <el-button type="warning" @click="exportSuspicious">导出</el-button>
            </el-form-item>
          </el-form>
          <el-table
            v-loading="loading.suspTable"
            :data="suspData.list"
            stripe
            border
            max-height="480"
            style="width: 100%"
          >
            <el-table-column prop="plant" label="区域工厂" width="120" show-overflow-tooltip />
            <el-table-column prop="recordDate" label="录入日期" width="110" />
            <el-table-column prop="partCode" label="零件号" width="110" show-overflow-tooltip />
            <el-table-column prop="partName" label="零件名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="supplierCode" label="供应商代码" width="100" />
            <el-table-column prop="supplierName" label="供应商名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="functionModule" label="功能模块" width="100" show-overflow-tooltip />
            <el-table-column prop="faultType" label="故障类别" width="100" show-overflow-tooltip />
            <el-table-column prop="failureDesc" label="失效描述" min-width="150" show-overflow-tooltip />
            <el-table-column prop="modelMachine" label="车型/机型" width="100" show-overflow-tooltip />
            <el-table-column prop="quantity" label="数量" width="80" align="right" />
            <el-table-column prop="defectCount" label="可疑物料数量" width="110" align="right" />
            <el-table-column prop="brand" label="品牌" width="80" show-overflow-tooltip />
            <el-table-column prop="prodArea" label="产生区域" width="100" show-overflow-tooltip />
            <el-table-column prop="shiftSection" label="班次/工段" width="100" show-overflow-tooltip />
            <el-table-column prop="orderDate" label="开单日期" width="110" />
            <el-table-column prop="supplierResp" label="供应商责任" width="100" />
            <el-table-column prop="recorder" label="录入人员" width="100" />
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
          </el-table>
          <el-pagination
            v-model:current-page="suspForm.page"
            v-model:page-size="suspForm.size"
            :page-sizes="[10, 20, 50]"
            :total="suspData.total"
            layout="total, sizes, prev, pager, next"
            class="pagination"
            @size-change="querySuspicious"
            @current-change="querySuspicious"
          />
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="供货量" name="supply">
        <el-card shadow="never" class="card">
          <el-form :inline="true" :model="suppForm" class="form">
            <el-form-item label="月份">
              <el-date-picker
                v-model="suppForm.ppmMonth"
                type="month"
                placeholder="不选查全部"
                value-format="YYYYMM"
                clearable
                style="width: 140px"
              />
            </el-form-item>
            <el-form-item label="基地">
              <el-select v-model="suppForm.baseCode" placeholder="不选查全部" clearable style="width: 120px">
                <el-option label="河西" value="河西" />
                <el-option label="宝骏" value="宝骏" />
                <el-option label="青岛" value="青岛" />
                <el-option label="重庆" value="重庆" />
              </el-select>
            </el-form-item>
            <el-form-item label="工厂编码">
              <el-input v-model="suppForm.plantId" placeholder="不填查全部" clearable style="width: 120px" />
            </el-form-item>
            <el-form-item label="供应商代码">
              <el-input v-model="suppForm.supplierCode" placeholder="不填查全部" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="供应商名称">
              <el-input v-model="suppForm.supplierName" placeholder="不填查全部" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="零件号">
              <el-input v-model="suppForm.partCode" placeholder="不填查全部" clearable style="width: 120px" />
            </el-form-item>
            <el-form-item label="零件名称">
              <el-input v-model="suppForm.partName" placeholder="不填查全部" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading.supp" @click="querySupply">查询</el-button>
              <el-button type="warning" @click="exportSupply">导出</el-button>
            </el-form-item>
          </el-form>
          <el-table
            v-loading="loading.suppTable"
            :data="suppData.list"
            stripe
            border
            max-height="480"
            style="width: 100%"
            @sort-change="handleSupplySort"
          >
            <el-table-column prop="fiscalYear" label="财年" width="90" sortable="custom" />
            <el-table-column prop="baseCode" label="基地" width="90" sortable="custom" />
            <el-table-column prop="plantId" label="工厂编码" width="90" sortable="custom" />
            <el-table-column prop="supplierCode" label="供应商代码" width="110" sortable="custom" />
            <el-table-column prop="supplierName" label="供应商名称" min-width="140" show-overflow-tooltip sortable="custom" />
            <el-table-column prop="partCode" label="零件号" width="110" show-overflow-tooltip sortable="custom" />
            <el-table-column prop="partName" label="零件名称" min-width="140" show-overflow-tooltip sortable="custom" />
            <el-table-column prop="supplyQty" label="供货量" width="100" align="right" sortable="custom" />
          </el-table>
          <el-pagination
            v-model:current-page="suppForm.page"
            v-model:page-size="suppForm.size"
            :page-sizes="[10, 20, 50]"
            :total="suppData.total"
            layout="total, sizes, prev, pager, next"
            class="pagination"
            @size-change="querySupply"
            @current-change="querySupply"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { querySuspiciousMaterial, querySupplyVolume } from '@/api/queryApi'
import { exportSuspiciousMaterial, exportSupplyVolume } from '@/api/exportApi'

const activeTab = ref('suspicious')

const loading = reactive({ susp: false, suspTable: false, supp: false, suppTable: false })

const suspForm = reactive({
  plant: '',
  supplierCode: '',
  supplierName: '',
  partCode: '',
  partName: '',
  orderDateRange: null,
  page: 1,
  size: 20,
})
const suspData = reactive({ list: [], total: 0 })

const suppForm = reactive({
  ppmMonth: '',
  baseCode: '',
  plantId: '',
  supplierCode: '',
  supplierName: '',
  partCode: '',
  partName: '',
  sortField: '',
  sortOrder: '',
  page: 1,
  size: 20,
})
const suppData = reactive({ list: [], total: 0 })

/** 将日期统一格式化为 YYYY-MM-DD，确保后端能正确解析 */
function toDateStr(v) {
  if (v == null || v === '') return undefined
  if (typeof v === 'string') return v.substring(0, 10)
  if (v instanceof Date) return v.toISOString().slice(0, 10)
  return undefined
}

function handleSuspiciousQuery() {
  suspForm.page = 1
  querySuspicious()
}

async function querySuspicious() {
  loading.susp = true
  loading.suspTable = true
  try {
    const range = suspForm.orderDateRange
    const from = Array.isArray(range) ? toDateStr(range[0]) : undefined
    const to = Array.isArray(range) ? toDateStr(range[1]) : undefined
    const params = {
      plant: suspForm.plant || undefined,
      supplierCode: suspForm.supplierCode || undefined,
      supplierName: suspForm.supplierName || undefined,
      partCode: suspForm.partCode || undefined,
      partName: suspForm.partName || undefined,
      orderDateFrom: from,
      orderDateTo: to,
      page: suspForm.page,
      size: suspForm.size,
    }
    const res = await querySuspiciousMaterial(params)
    suspData.list = res?.list ?? []
    suspData.total = res?.total ?? 0
  } finally {
    loading.susp = false
    loading.suspTable = false
  }
}

async function querySupply() {
  loading.supp = true
  loading.suppTable = true
  try {
    const params = {
      ppmMonth: suppForm.ppmMonth || undefined,
      baseCode: suppForm.baseCode || undefined,
      plantId: suppForm.plantId || undefined,
      supplierCode: suppForm.supplierCode || undefined,
      supplierName: suppForm.supplierName || undefined,
      partCode: suppForm.partCode || undefined,
      partName: suppForm.partName || undefined,
      sortField: suppForm.sortField || undefined,
      sortOrder: suppForm.sortOrder || undefined,
      page: suppForm.page,
      size: suppForm.size,
    }
    const res = await querySupplyVolume(params)
    suppData.list = res?.list ?? []
    suppData.total = res?.total ?? 0
  } finally {
    loading.supp = false
    loading.suppTable = false
  }
}

/** 排序变化处理 */
function handleSupplySort({ prop, order }) {
  suppForm.sortField = prop || ''
  suppForm.sortOrder = order === 'ascending' ? 'asc' : (order === 'descending' ? 'desc' : '')
  suppForm.page = 1
  querySupply()
}

function exportSuspicious() {
  const range = suspForm.orderDateRange
  const from = Array.isArray(range) ? toDateStr(range[0]) : undefined
  const to = Array.isArray(range) ? toDateStr(range[1]) : undefined
  const params = {}
  if (suspForm.plant) params.plant = suspForm.plant
  if (suspForm.supplierCode) params.supplierCode = suspForm.supplierCode
  if (suspForm.supplierName) params.supplierName = suspForm.supplierName
  if (suspForm.partCode) params.partCode = suspForm.partCode
  if (suspForm.partName) params.partName = suspForm.partName
  if (from) params.recordDateFrom = from
  if (to) params.recordDateTo = to
  exportSuspiciousMaterial(params)
}

function exportSupply() {
  const params = {}
  if (suppForm.ppmMonth) params.ppmMonth = suppForm.ppmMonth
  if (suppForm.baseCode) params.baseCode = suppForm.baseCode
  if (suppForm.plantId) params.plantId = suppForm.plantId
  if (suppForm.supplierCode) params.supplierCode = suppForm.supplierCode
  if (suppForm.supplierName) params.supplierName = suppForm.supplierName
  if (suppForm.partCode) params.partCode = suppForm.partCode
  if (suppForm.partName) params.partName = suppForm.partName
  exportSupplyVolume(params)
}

onMounted(() => {
  querySuspicious()
})
</script>

<style scoped>
.query-page {
  max-width: 1200px;
}

.tabs {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.tabs :deep(.el-tabs__content) {
  padding: 0;
}

.card {
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.form {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
