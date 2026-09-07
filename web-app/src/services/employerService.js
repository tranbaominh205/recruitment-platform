import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function getMyCompany() { return apiClient.get('/employer/company').then(getResult) }
export function createCompany(payload) { return apiClient.post('/employer/company', payload).then(getResult) }
export function updateCompany(payload) { return apiClient.put('/employer/company', payload).then(getResult) }