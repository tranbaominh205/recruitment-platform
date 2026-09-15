import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminNotification } from '../services/notificationService'

function AdminNotificationDetailPage() {
  const { notificationId } = useParams()
  const [notification, setNotification] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadNotification() {
      setIsLoading(true)
      setError('')
      try {
        setNotification(await getAdminNotification(notificationId))
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load notification detail.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadNotification()
  }, [notificationId])

  if (isLoading) {
    return <main className="workspace-page"><p className="page-state">Loading notification...</p></main>
  }

  if (!notification) {
    return (
      <main className="workspace-page">
        <Link to="/admin/notifications">Back to notifications</Link>
        <p className="error-message">{error || 'Notification not found.'}</p>
      </main>
    )
  }

  return (
    <main className="workspace-page">
      <Link to="/admin/notifications">Back to notifications</Link>
      <p className="eyebrow">Notification detail</p>
      <h1>{notification.title}</h1>
      {error && <p className="error-message">{error}</p>}
      <div className="detail-grid">
        <p><strong>id</strong>{notification.id}</p>
        <p><strong>type</strong>{notification.type}</p>
        <p className="full-width"><strong>message</strong>{notification.message}</p>
        <p><strong>referenceId</strong>{notification.referenceId || 'Not set'}</p>
        <p><strong>read</strong>{String(notification.read)}</p>
        <p><strong>createdAt</strong>{new Date(notification.createdAt).toLocaleString()}</p>
      </div>
    </main>
  )
}

export default AdminNotificationDetailPage
