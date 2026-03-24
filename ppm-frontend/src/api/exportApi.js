/**
 * 导出与模板下载：使用 axios 下载文件流，确保代理和错误处理正确。
 */
import request from './request'
import { ElMessage } from 'element-plus'

function toQueryString(params) {
  const clean = {}
  for (const [k, v] of Object.entries(params)) {
    if (v != null && v !== '' && v !== undefined) {
      clean[k] = String(v)
    }
  }
  const q = new URLSearchParams(clean).toString()
  return q ? '?' + q : ''
}

async function downloadBlob(urlSuffix, filenameHint) {
  try {
    const res = await request.get(urlSuffix, { responseType: 'blob' })
    const blob = res instanceof Blob ? res : (res?.data ?? res)
    if (!(blob instanceof Blob)) {
      ElMessage.error('导出失败：响应格式异常')
      return
    }
    if (blob.size === 0) {
      ElMessage.warning('导出结果为空，请检查筛选条件或数据是否存在')
      return
    }
    const contentType = blob.type || ''
    if (contentType.includes('application/json')) {
      const text = await blob.text()
      let msg = '导出失败'
      try {
        const json = JSON.parse(text)
        msg = json.message || msg
      } catch (_) {}
      ElMessage.error(msg)
      return
    }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filenameHint || 'export_' + Date.now() + '.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (err) {
    const msg = err.response?.data
    if (msg instanceof Blob) {
      try {
        const text = await msg.text()
        const json = JSON.parse(text)
        ElMessage.error(json.message || '导出失败')
      } catch (_) {
        ElMessage.error(err.message || '导出失败')
      }
    } else {
      ElMessage.error(err.response?.data?.message || err.message || '导出失败')
    }
  }
}

export function exportPpmSummary(params = {}) {
  downloadBlob('/export/ppm-summary' + toQueryString(params), 'PPM汇总_' + Date.now() + '.xlsx')
}

export function exportSuspiciousMaterial(params = {}) {
  downloadBlob('/export/suspicious-material' + toQueryString(params), '可疑物料_' + Date.now() + '.xlsx')
}

export function exportSupplyVolume(params = {}) {
  downloadBlob('/export/supply-volume' + toQueryString(params), '供货量_' + Date.now() + '.xlsx')
}

/** type: supplier-ppm | suspicious-material | supply-volume */
export function downloadTemplate(type) {
  downloadBlob('/import/template/' + encodeURIComponent(type), type + '_模板.xlsx')
}
