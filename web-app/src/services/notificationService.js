import apiClient from '../configurations/apiClient'

const getResult = (response) => response.data.result

export function getMyNotifications() {
  return apiClient.get('/notification').then(getResult)
}

export function getUnreadNotificationCount() {
  return apiClient.get('/notification/unread-count').then(getResult)
}

export function markNotificationAsRead(notificationId) {
  return apiClient.patch(`/notification/${notificationId}/read`).then(getResult)
}

export function markNotificationAsUnread(notificationId) {
  return apiClient.patch(`/notification/${notificationId}/unread`).then(getResult)
}

export function markAllNotificationsAsRead() {
  return apiClient.patch('/notification/read-all').then(getResult)
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