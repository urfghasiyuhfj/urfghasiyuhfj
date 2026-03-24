import request from './request'

export function querySuspiciousMaterial(params) {
  return request.get('/query/suspicious-material', { params })
}

export function querySupplyVolume(params) {
  return request.get('/query/supply-volume', { params })
}

export function getSuspiciousMaterialStats(params) {
  return request.get('/query/suspicious-material/stats', { params })
}
