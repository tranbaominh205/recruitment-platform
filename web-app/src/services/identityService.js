import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function registerAccount(payload) {
  return apiClient.post('/identity/auth/register', payload).then(getResult)
}

export function loginAccount(payload) {
  return apiClient.post('/identity/auth/login', payload).then(getResult)
}

export function logoutAccount() {
  return apiClient.post('/identity/auth/logout').then(getResult)
}

export function getCurrentAccount() {
  return apiClient.get('/identity/me').then(getResult)
}

export function getAdminAccounts(params) {
  return apiClient.get('/identity/admin/accounts', { params }).then(getResult)
}

export function getAdminAccount(accountId) {
  return apiClient.get(`/identity/admin/accounts/${accountId}`).then(getResult)
}

export function updateAdminAccountEnabled(accountId, enabled) {
  return apiClient.patch(`/identity/admin/accounts/${accountId}/enabled`, { enabled }).then(getResult)
}

export function getAdminAccountStatistics() {
  return apiClient.get('/identity/admin/statistics').then(getResult)
}