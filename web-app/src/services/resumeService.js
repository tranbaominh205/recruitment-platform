import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function getMyResumes() {
  return apiClient.get('/resume').then(getResult)
}

export function uploadResume(file) {
  const formData = new FormData()
  formData.append('file', file)

  return apiClient.post('/resume', formData).then(getResult)
}

export function getMyResume(resumeId) {
  return apiClient.get(`/resume/${resumeId}`).then(getResult)
}

export function downloadMyResume(resumeId) {
  return apiClient.get(`/resume/${resumeId}/download`, { responseType: 'blob' }).then((response) => ({
    blob: response.data,
    fileName: response.headers['content-disposition'] || 'resume.pdf',
  }))
}

export function getAdminResumeStatistics() {
  return apiClient.get('/resume/admin/statistics').then(getResult)
}

export function getAdminResumes(params) {
  return apiClient.get('/resume/admin/resumes', { params }).then(getResult)
}

export function getAdminResume(resumeId) {
  return apiClient.get(`/resume/admin/resumes/${resumeId}`).then(getResult)
}