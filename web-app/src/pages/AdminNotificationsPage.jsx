import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminNotifications } from '../services/notificationService'

const defaultPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

function AdminNotificationsPage() {
  const [filters, setFilters] = useState({
    recipientAccountId: '',
    type: '',
    read: '',
  })
  const [resultPage, setResultPage] = useState(defaultPage)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadNotifications(nextPage = 0, nextFilters = filters) {
    setIsLoading(true)
    setError('')
    try {
      const params = {
        page: nextPage,
        size: resultPage.size || 20,
      }

      if (nextFilters.recipientAccountId.trim()) {
        params.recipientAccountId = nextFilters.recipientAccountId.trim()
      }

      if (nextFilters.type) {
        params.type = nextFilters.type
      }

      if (nextFilters.read) {
        params.read = nextFilters.read === 'true'
      }

      const response = await getAdminNotifications(params)
      setResultPage({
        content: response.content || [],
        page: response.page || 0,
        size: response.size || 20,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })
    } catch (requestError) {
      setResultPage(defaultPage)
      setError(getApiErrorMessage(requestError, 'Unable to load notifications.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadNotifications(0)
  }, [])

  function updateFilter(event) {
    setFilters({ ...filters, [event.target.name]: event.target.value })
    setResultPage({ ...resultPage, page: 0 })
  }

  function handleSearch(event) {
    event.preventDefault()
    loadNotifications(0)
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Notifications</p>
      <h1>Admin notifications</h1>
      {error && <p className="error-message">{error}</p>}
      <form className="search-grid" onSubmit={handleSearch}>
        <label>
          Recipient Account ID
          <input
            name="recipientAccountId"
            value={filters.recipientAccountId}
            onChange={updateFilter}
            placeholder="Identity account UUID"
          />
        </label>
        <label>
          Type
          <select name="type" value={filters.type} onChange={updateFilter}>
            <option value="">All types</option>
            <option value="APPLICATION_STATUS_CHANGED">APPLICATION_STATUS_CHANGED</option>
            <option value="INTERVIEW_SCHEDULED">INTERVIEW_SCHEDULED</option>
          </select>
        </label>
        <label>
          Read
          <select name="read" value={filters.read} onChange={updateFilter}>
            <option value="">All</option>
            <option value="true">Read</option>
            <option value="false">Unread</option>
          </select>
        </label>
        <button type="submit">Search notifications</button>
      </form>

      {isLoading ? (
        <p className="page-state">Loading notifications...</p>
      ) : resultPage.content.length === 0 ? (
        <p className="empty-state">No notifications match your filters.</p>
      ) : (
        <div className="item-list">
          {resultPage.content.map((notification) => (
            <article className="list-item notification-item" key={notification.id}>
              <div>
                <p className="eyebrow">{notification.type}</p>
                <h2>{notification.title}</h2>
                <p>{notification.message}</p>
                <p>
                  id: {notification.id}
                  <br />
                  referenceId: {notification.referenceId || 'Not set'}
                  <br />
                  read: {String(notification.read)}
                  <br />
                  createdAt: {new Date(notification.createdAt).toLocaleString()}
                </p>
              </div>
              <Link className="button-link" to={`/admin/notifications/${notification.id}`}>View detail</Link>
            </article>
          ))}
        </div>
      )}

      {!isLoading && resultPage.totalPages > 0 && (
        <div className="pager">
          <button
            type="button"
            className="button-secondary"
            disabled={resultPage.page <= 0}
            onClick={() => loadNotifications(resultPage.page - 1)}
          >
            Previous
          </button>
          <span>
            Page {resultPage.page + 1} of {resultPage.totalPages} ({resultPage.totalElements} total)
          </span>
          <button
            type="button"
            className="button-secondary"
            disabled={resultPage.page + 1 >= resultPage.totalPages}
            onClick={() => loadNotifications(resultPage.page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </main>
  )
}

export default AdminNotificationsPage
