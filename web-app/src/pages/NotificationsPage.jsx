import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../services/apiError'
import { getMyNotifications } from '../services/notificationService'

const notificationTypeLabels = {
  APPLICATION_STATUS_CHANGED: 'Application status changed',
  INTERVIEW_SCHEDULED: 'Interview scheduled',
}

function NotificationsPage() {
  const [notifications, setNotifications] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadNotifications() {
      try {
        setNotifications(await getMyNotifications())
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load your notifications.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadNotifications()
  }, [])

  return (
    <main className="workspace-page">
      <p className="eyebrow">Your activity</p>
      <h1>Notifications</h1>
      {error && <p className="error-message">{error}</p>}
      {isLoading ? <p className="page-state">Loading notifications...</p> : notifications.length === 0 ? <p className="empty-state">You have no notifications yet.</p> : <div className="item-list">{notifications.map((notification) => <article className={`list-item notification-item${notification.read ? '' : ' notification-unread'}`} key={notification.id}><div><p className="eyebrow">{notificationTypeLabels[notification.type] || notification.type}</p><h2>{notification.title}</h2><p>{notification.message}</p><p>{new Date(notification.createdAt).toLocaleString()} · {notification.read ? 'Read' : 'Unread'}</p></div></article>)}</div>}
    </main>
  )
}

export default NotificationsPage