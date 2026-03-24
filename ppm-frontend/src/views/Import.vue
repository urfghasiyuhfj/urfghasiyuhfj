<template>
  <div class="import-page">
    <el-card class="card" shadow="never">
      <template #header>
        <span>单文件导入</span>
      </template>
      <el-row :gutter="24">
        <el-col :xs="24" :md="8">
          <div class="upload-box">
            <div class="upload-label">供应商 PPM Excel</div>
            <el-button link type="primary" size="small" @click="downloadTemplate('supplier-ppm')">下载模板</el-button>
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept=".xlsx,.xls"
              :on-change="(f) => handleUpload(f, 'supplierPpm')"
              :on-exceed="() => ElMessage.warning('仅支持单个文件')"
            >
              <el-button type="primary" :loading="loading.supplierPpm">选择文件</el-button>
            </el-upload>
          </div>
        </el-col>
        <el-col :xs="24" :md="8">
          <div class="upload-box">
            <div class="upload-label">可疑物料统计 Excel</div>
            <el-button link type="primary" size="small" @click="downloadTemplate('suspicious-material')">下载模板</el-button>
            <span class="upload-hint">导入后自动计算当月 PPM</span>
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept=".xlsx,.xls"
              :on-change="(f) => handleUpload(f, 'suspicious')"
              :on-exceed="() => ElMessage.warning('仅支持单个文件')"
            >
              <el-button type="primary" :loading="loading.suspicious">选择文件</el-button>
            </el-upload>
          </div>
        </el-col>
        <el-col :xs="24" :md="8">
          <div class="upload-box">
            <div class="upload-label">供货量 Excel</div>
            <el-button link type="primary" size="small" @click="downloadTemplate('supply-volume')">下载模板</el-button>
            <span class="upload-hint">导入后自动计算当月 PPM</span>
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept=".xlsx,.xls"
              :on-change="(f) => handleUpload(f, 'supply')"
              :on-exceed="() => ElMessage.warning('仅支持单个文件')"
            >
              <el-button type="primary" :loading="loading.supply">选择文件</el-button>
            </el-upload>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="card" shadow="never">
      <template #header>
        <span>从 ppm 目录批量导入</span>
      </template>
      <div class="folder-import">
        <el-input
          v-model="folderPath"
          placeholder="留空使用默认目录（如 D:\codespace\ppm\ppm）"
          clearable
          style="max-width: 480px; margin-right: 12px;"
        />
        <el-button type="success" :loading="loading.folder" @click="handleFolderImport">
          一键导入
        </el-button>
      </div>
      <div v-if="folderResult" class="folder-result">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="供应商 PPM"> {{ folderResult.supplierPpmRows }} 行 </el-descriptions-item>
          <el-descriptions-item label="可疑物料"> {{ folderResult.suspiciousMaterialRows }} 行 </el-descriptions-item>
          <el-descriptions-item label="供货量"> {{ folderResult.supplyVolumeRows }} 行 </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  importSupplierPpm,
  importSuspiciousMaterial,
  importSupplyVolume,
  importFromPpmFolder,
} from '@/api/importApi'
import { downloadTemplate } from '@/api/exportApi'

const folderPath = ref('')
const folderResult = ref(null)

const loading = reactive({
  supplierPpm: false,
  suspicious: false,
  supply: false,
  folder: false,
})

function showValidationErrors(errors) {
  const lines = errors.slice(0, 10).map((e) => {
    const src = e.sourceFile ? ` [${e.sourceFile}]` : ''
    return `第 ${e.row} 行${src} [${e.field}]: ${e.message}`
  })
  ElMessageBox.alert(
    `以下 ${errors.length} 行校验未通过（已跳过）：\n${lines.join('\n')}${errors.length > 10 ? `\n... 等共 ${errors.length} 处` : ''}`,
    '导入校验提示',
    { type: 'warning', confirmButtonText: '知道了' },
  )
}

function showPpmDiffWarning(ppmRecalculations) {
  const withDiff = (ppmRecalculations || []).filter((r) => r.hasDifferences && r.differences?.length)
  if (withDiff.length === 0) return
  const lines = []
  for (const r of withDiff) {
    lines.push(`【${r.ppmMonth}】${r.differences.length} 项与库中数据不一致：`)
    for (const d of r.differences.slice(0, 5)) {
      const oldVal = d.oldPpm != null ? Number(d.oldPpm).toFixed(2) : '-'
      const newVal = d.newPpm != null ? Number(d.newPpm).toFixed(2) : '-'
      lines.push(`  ${d.baseName}-${d.supplierName}：PPM ${oldVal} → ${newVal}`)
    }
    if (r.differences.length > 5) lines.push(`  ... 等共 ${r.differences.length} 项`)
  }
  ElMessageBox.alert(lines.join('\n'), 'PPM 数据差异提醒', {
    type: 'warning',
    confirmButtonText: '知道了',
  })
}

async function handleUpload({ raw }, type) {
  if (!raw) return
  const key = type === 'supplierPpm' ? 'supplierPpm' : type === 'suspicious' ? 'suspicious' : 'supply'
  loading[key] = true
  try {
    let res
    if (type === 'supplierPpm') {
      res = await importSupplierPpm(raw)
    } else if (type === 'suspicious') {
      res = await importSuspiciousMaterial(raw)
    } else {
      res = await importSupplyVolume(raw)
    }
    const n = res?.rowsImported ?? (typeof res === 'number' ? res : 0)
    ElMessage.success(`导入成功，共 ${n} 行`)
    if (res?.validationErrors?.length) {
      showValidationErrors(res.validationErrors)
    }
    if (res?.ppmRecalculations?.length) {
      showPpmDiffWarning(res.ppmRecalculations)
    }
  } finally {
    loading[key] = false
  }
}

async function handleFolderImport() {
  loading.folder = true
  folderResult.value = null
  try {
    const res = await importFromPpmFolder(folderPath.value || undefined)
    folderResult.value = res
    const total =
      (res.supplierPpmRows || 0) + (res.suspiciousMaterialRows || 0) + (res.supplyVolumeRows || 0)
    ElMessage.success(`批量导入完成，共 ${total} 行`)
    if (res?.validationErrors?.length) {
      showValidationErrors(res.validationErrors)
    }
    if (res?.ppmRecalculations?.length) {
      showPpmDiffWarning(res.ppmRecalculations)
    }
  } finally {
    loading.folder = false
  }
}
</script>

<style scoped>
.import-page {
  max-width: 960px;
}

.card {
  margin-bottom: 24px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.upload-box {
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 16px;
}

.upload-label {
  font-weight: 500;
  color: #475569;
  margin-bottom: 12px;
  font-size: 14px;
}

.upload-hint {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.folder-import {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.folder-result {
  margin-top: 20px;
  max-width: 520px;
}
</style>
