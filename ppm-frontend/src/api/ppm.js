import request from './request'

export function getPpmSummary(params) {
  return request.get('/ppm/summary', { params })
}

export function getPpmListByMonth(ppmMonth) {
  return request.get('/ppm/list', { params: { ppmMonth } })
}

export function getPpmTrend(limitMonths = 12) {
  return request.get('/ppm/trend', { params: { limitMonths } })
}

export function getPpmTrendByBase(baseCode, limitMonths = 12) {
  return request.get('/ppm/trend/by-base', { params: { baseCode, limitMonths } })
}

export function getPpmGlobalMonthly(limitMonths = 12) {
  return request.get('/ppm/global-monthly', { params: { limitMonths } })
}

export function getPpmSupplierMonthlyTrend(limitMonths = 12) {
  return request.get('/ppm/supplier-monthly-trend', { params: { limitMonths } })
}

export function calculatePpm(ppmMonth) {
  return request.post('/ppm/calculate', null, { params: { ppmMonth } })
}

export function getAvailableMonths() {
  return request.get('/ppm/available-months')
}
