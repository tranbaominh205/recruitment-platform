import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function registerAccount(payload) {
  return apiClient.post('/identity/auth/register', payload).then(getResult)
}

export function loginAccount(payload) {
  return apiClient.post('/identity/auth/login', payload).then(getResult)
}

export function getCurrentAccount() {
  return apiClient.get('/identity/me').then(getResult)
}