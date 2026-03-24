import request from './request'

export function getBaseList() {
  return request.get('/base/list')
}
