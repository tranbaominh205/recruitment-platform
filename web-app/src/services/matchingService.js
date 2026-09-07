import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function getMatchResult(applicationId) { return apiClient.get(`/matching/application/${applicationId}`).then(getResult) }
export function runMatching(applicationId) { return apiClient.post(`/matching/application/${applicationId}`).then(getResult) }