import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function getMyNotifications() {
  return apiClient.get('/notification').then(getResult)
}

export function getAdminNotificationStatistics() {
  return apiClient.get('/notification/admin/statistics').then(getResult)
}

export function getAdminNotifications(params) {
  return apiClient.get('/notification/admin/notifications', { params }).then(getResult)
}

export function getAdminNotification(notificationId) {
  return apiClient.get(`/notification/admin/notifications/${notificationId}`).then(getResult)
}