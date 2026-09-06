import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function submitApplication(payload) {
  return apiClient.post('/recruitment/application', payload).then(getResult)
}

export function getMyApplications(params) {
  return apiClient.get('/recruitment/application/mine', { params }).then(getResult)
}

export function getApplication(applicationId) {
  return apiClient.get(`/recruitment/application/${applicationId}`).then(getResult)
}

export function withdrawApplication(applicationId) {
  return apiClient.patch(`/recruitment/application/${applicationId}/withdraw`).then(getResult)
}

export function getInterview(applicationId) {
  return apiClient.get(`/recruitment/application/${applicationId}/interview`).then(getResult)
}