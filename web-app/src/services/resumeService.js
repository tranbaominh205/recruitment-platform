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