import request from './request'

export function importSupplierPpm(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/import/supplier-ppm', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function importSuspiciousMaterial(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/import/suspicious-material', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function importSupplyVolume(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/import/supply-volume', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 检测供货量导入冲突
 */
export function checkSupplyVolumeConflicts(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/import/supply-volume/check-conflicts', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 覆盖导入供货量
 */
export function importSupplyVolumeWithOverride(file, overwrite = true) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('overwrite', overwrite)
  return request.post('/import/supply-volume/override', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function importFromPpmFolder(path) {
  return request.post('/import/from-ppm-folder', null, {
    params: path ? { path } : {},
  })
}
