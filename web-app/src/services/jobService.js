import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function searchJobs(params) {
  return apiClient.get('/job/search', { params }).then(getResult)
}

export function getPublishedJob(jobId) {
  return apiClient.get(`/job/${jobId}`).then(getResult)
}

export function getMyJobs(params) { return apiClient.get('/job/mine', { params }).then(getResult) }
export function getOwnedJob(jobId) { return apiClient.get(`/job/${jobId}/ownership`).then(getResult) }
export function createJob(payload) { return apiClient.post('/job', payload).then(getResult) }
export function updateJob(jobId, payload) { return apiClient.put(`/job/${jobId}`, payload).then(getResult) }
export function publishJob(jobId) { return apiClient.post(`/job/${jobId}/publish`).then(getResult) }
export function closeJob(jobId) { return apiClient.post(`/job/${jobId}/close`).then(getResult) }