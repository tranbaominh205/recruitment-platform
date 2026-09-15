import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminInterviews } from '../services/recruitmentService'

const defaultPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

function AdminInterviewsPage() {
  const [resultPage, setResultPage] = useState(defaultPage)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadInterviews(nextPage = 0) {
    setIsLoading(true)
    setError('')
    try {
      const response = await getAdminInterviews({
        page: nextPage,
        size: resultPage.size || 20,
      })
      setResultPage({
        content: response.content || [],
        page: response.page || 0,
        size: response.size || 20,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })
    } catch (requestError) {
      setResultPage(defaultPage)
      setError(getApiErrorMessage(requestError, 'Unable to load interviews.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadInterviews(0)
  }, [])

  return (
    <main className="workspace-page">
      <p className="eyebrow">Interviews</p>
      <h1>Manage interviews</h1>
      {error && <p className="error-message">{error}</p>}
      {isLoading ? (
        <p className="page-state">Loading interviews...</p>
      ) : resultPage.content.length === 0 ? (
        <p className="empty-state">No interviews found.</p>
      ) : (
        <div className="item-list">
          {resultPage.content.map((interview) => (
            <article className="list-item" key={interview.id}>
              <div>
                <p className="eyebrow">Interview</p>
                <h2>{interview.id}</h2>
                <p>
                  applicationId: {interview.applicationId}
                  <br />
                  scheduledAt: {new Date(interview.scheduledAt).toLocaleString()}
                  <br />
                  location: {interview.location || 'Not set'}
                </p>
              </div>
              <Link className="button-link" to={`/admin/interviews/${interview.id}`}>View detail</Link>
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
            onClick={() => loadInterviews(resultPage.page - 1)}
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
            onClick={() => loadInterviews(resultPage.page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </main>
  )
}

export default AdminInterviewsPage
