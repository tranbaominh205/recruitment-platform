import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function searchJobs(params) {
  return apiClient.get('/job/search', { params }).then(getResult)
}

export function getPublishedJob(jobId) {
  return apiClient.get(`/job/${jobId}`).then(getResult)
}