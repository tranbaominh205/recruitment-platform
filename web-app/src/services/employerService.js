import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function getMyCompany() { return apiClient.get('/employer/company').then(getResult) }
export function createCompany(payload) { return apiClient.post('/employer/company', payload).then(getResult) }
export function updateCompany(payload) { return apiClient.put('/employer/company', payload).then(getResult) }
export function getAdminEmployerStatistics() { return apiClient.get('/employer/admin/statistics').then(getResult) }
export function getAdminCompanies(params) { return apiClient.get('/employer/admin/companies', { params }).then(getResult) }
export function getAdminCompany(companyId) { return apiClient.get(`/employer/admin/companies/${companyId}`).then(getResult) }