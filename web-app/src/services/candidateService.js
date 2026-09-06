import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function getMyProfile() {
  return apiClient.get('/candidate/profile').then(getResult)
}

export function createMyProfile(payload) {
  return apiClient.post('/candidate/profile', payload).then(getResult)
}

export function updateMyProfile(payload) {
  return apiClient.put('/candidate/profile', payload).then(getResult)
}

export function updateMyPreferences(payload) {
  return apiClient.put('/candidate/profile/preferences', payload).then(getResult)
}