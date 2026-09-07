import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function getMyNotifications() {
  return apiClient.get('/notification').then(getResult)
}