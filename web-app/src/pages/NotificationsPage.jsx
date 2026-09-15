import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../configurations/useAuth'
import { subscribeToNotificationCreated } from '../services/accountSseService'
import { getApiErrorMessage } from '../services/apiError'
import {
  getMyNotifications,
  getUnreadNotificationCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  markNotificationAsUnread,
} from '../services/notificationService'

const notificationTypeLabels = {
  APPLICATION_STATUS_CHANGED: 'Application status changed',
  INTERVIEW_SCHEDULED: 'Interview scheduled',
}

function NotificationsPage() {
  const { account } = useAuth()
  const navigate = useNavigate()
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyNotificationId, setBusyNotificationId] = useState(null)
  const [isMarkingAll, setIsMarkingAll] = useState(false)

  const loadNotifications = useCallback(async () => {
    const [loadedNotifications, unreadSummary] = await Promise.all([
      getMyNotifications(),
      getUnreadNotificationCount(),
    ])
    setNotifications(loadedNotifications)
    setUnreadCount(unreadSummary?.unreadCount ?? 0)
    setError('')
  }, [])

  useEffect(() => {
    let active = true

    async function loadInitialNotifications() {
      try {
        await loadNotifications()
      } catch (requestError) {
        if (active) {
          setError(getApiErrorMessage(requestError, 'Unable to load your notifications.'))
        }
      } finally {
        if (active) {
          setIsLoading(false)
        }
      }
    }

    const unsubscribe = subscribeToNotificationCreated(async () => {
      if (!active) {
        return
      }
      try {
        await loadNotifications()
      } catch (requestError) {
        if (active) {
          setError(getApiErrorMessage(requestError, 'Unable to refresh your notifications.'))
        }
      }
    })

    loadInitialNotifications()

    return () => {
      active = false
      unsubscribe()
    }
  }, [loadNotifications])

  async function refreshUnreadCount() {
    const unreadSummary = await getUnreadNotificationCount()
    setUnreadCount(unreadSummary?.unreadCount ?? 0)
  }

  function replaceNotification(updatedNotification) {
    setNotifications((previousNotifications) =>
      previousNotifications.map((notification) =>
        notification.id === updatedNotification.id ? updatedNotification : notification,
      ),
    )
  }

  function getApplicationDetailRoute(referenceId) {
    if (account?.role === 'CANDIDATE') {
      return `/candidate/applications/${referenceId}`
    }

    if (account?.role === 'RECRUITER') {
      return `/recruiter/applications/${referenceId}`
    }

    return ''
  }

  async function handleToggleRead(notification) {
    setError('')
    setBusyNotificationId(notification.id)
    try {
      const updatedNotification = notification.read
        ? await markNotificationAsUnread(notification.id)
        : await markNotificationAsRead(notification.id)
      replaceNotification(updatedNotification)
      await refreshUnreadCount()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to update notification state.'))
    } finally {
      setBusyNotificationId(null)
    }
  }

  async function handleMarkAllAsRead() {
    setError('')
    setIsMarkingAll(true)
    try {
      const unreadSummary = await markAllNotificationsAsRead()
      setNotifications((previousNotifications) =>
        previousNotifications.map((notification) =>
          notification.read ? notification : { ...notification, read: true },
        ),
      )
      setUnreadCount(unreadSummary?.unreadCount ?? 0)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to mark all notifications as read.'))
    } finally {
      setIsMarkingAll(false)
    }
  }

  async function handleViewApplication(notification) {
    const referenceId = notification.referenceId
    if (!referenceId) return

    const destination = getApplicationDetailRoute(referenceId)
    if (!destination) {
      setError('Unable to open this application from your current account role.')
      return
    }

    if (notification.read) {
      navigate(destination)
      return
    }

    setError('')
    setBusyNotificationId(notification.id)
    try {
      const updatedNotification = await markNotificationAsRead(notification.id)
      replaceNotification(updatedNotification)
      await refreshUnreadCount()
      navigate(destination)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to open this application right now.'))
    } finally {
      setBusyNotificationId(null)
    }
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Your activity</p>
      <div className="page-heading">
        <h1>Notifications</h1>
        <button
          type="button"
          className="button-secondary"
          onClick={handleMarkAllAsRead}
          disabled={unreadCount === 0 || isLoading || isMarkingAll}
        >
          {isMarkingAll ? 'Marking...' : 'Mark all as read'}
        </button>
      </div>
      <p className="muted">{unreadCount} unread</p>
      {error && <p className="error-message">{error}</p>}
      {isLoading ? (
        <p className="page-state">Loading notifications...</p>
      ) : notifications.length === 0 ? (
        <p className="empty-state">You have no notifications yet.</p>
      ) : (
        <div className="item-list">
          {notifications.map((notification) => {
            const isBusy = busyNotificationId === notification.id
            return (
              <article
                className={`list-item notification-item${notification.read ? '' : ' notification-unread'}`}
                key={notification.id}
              >
                <div>
                  <p className="eyebrow">{notificationTypeLabels[notification.type] || notification.type}</p>
                  <h2>{notification.title}</h2>
                  <p>{notification.message}</p>
                  <p>
                    {new Date(notification.createdAt).toLocaleString()} · {notification.read ? 'Read' : 'Unread'}
                  </p>
                  <div className="action-row">
                    <button
                      type="button"
                      className="button-secondary"
                      onClick={() => handleToggleRead(notification)}
                      disabled={isBusy || isMarkingAll}
                    >
                      {isBusy ? 'Updating...' : notification.read ? 'Mark as unread' : 'Mark as read'}
                    </button>
                    {notification.referenceId && (
                      <button
                        type="button"
                        className="button-secondary"
                        onClick={() => handleViewApplication(notification)}
                        disabled={isBusy || isMarkingAll}
                      >
                        View application
                      </button>
                    )}
                  </div>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </main>
  )
}

export default NotificationsPage