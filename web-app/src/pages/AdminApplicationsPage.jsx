import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminApplications } from '../services/recruitmentService'

const defaultPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

function AdminApplicationsPage() {
  const [filters, setFilters] = useState({
    status: '',
    jobId: '',
    candidateId: '',
  })
  const [resultPage, setResultPage] = useState(defaultPage)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadApplications(nextPage = 0, nextFilters = filters) {
    setIsLoading(true)
    setError('')
    try {
      const params = {
        page: nextPage,
        size: resultPage.size || 20,
      }

      if (nextFilters.status.trim()) {
        params.status = nextFilters.status.trim()
      }

      if (nextFilters.jobId.trim()) {
        params.jobId = nextFilters.jobId.trim()
      }

      if (nextFilters.candidateId.trim()) {
        params.candidateId = nextFilters.candidateId.trim()
      }

      const response = await getAdminApplications(params)
      setResultPage({
        content: response.content || [],
        page: response.page || 0,
        size: response.size || 20,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })
    } catch (requestError) {
      setResultPage(defaultPage)
      setError(getApiErrorMessage(requestError, 'Unable to load applications.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadApplications(0)
  }, [])

  function updateFilter(event) {
    setFilters({ ...filters, [event.target.name]: event.target.value })
    setResultPage({ ...resultPage, page: 0 })
  }

  function handleSearch(event) {
    event.preventDefault()
    loadApplications(0)
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Applications</p>
      <h1>Manage applications</h1>
      {error && <p className="error-message">{error}</p>}
      <form className="search-grid" onSubmit={handleSearch}>
        <label>
          Status
          <input
            name="status"
            value={filters.status}
            onChange={updateFilter}
            placeholder="SUBMITTED"
          />
        </label>
        <label>
          Job ID
          <input
            name="jobId"
            value={filters.jobId}
            onChange={updateFilter}
            placeholder="Job UUID"
          />
        </label>
        <label>
          Candidate ID
          <input
            name="candidateId"
            value={filters.candidateId}
            onChange={updateFilter}
            placeholder="CandidateProfile UUID"
          />
        </label>
        <button type="submit">Search applications</button>
      </form>

      {isLoading ? (
        <p className="page-state">Loading applications...</p>
      ) : resultPage.content.length === 0 ? (
        <p className="empty-state">No applications match your filters.</p>
      ) : (
        <div className="item-list">
          {resultPage.content.map((application) => (
            <article className="list-item" key={application.id}>
              <div>
                <p className="eyebrow">{application.status}</p>
                <h2>{application.id}</h2>
                <p>
                  candidateId: {application.candidateId}
                  <br />
                  jobId: {application.jobId}
                  <br />
                  resumeId: {application.resumeId}
                  <br />
                  submittedAt: {new Date(application.submittedAt).toLocaleString()}
                </p>
              </div>
              <Link className="button-link" to={`/admin/applications/${application.id}`}>View detail</Link>
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
            onClick={() => loadApplications(resultPage.page - 1)}
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
            onClick={() => loadApplications(resultPage.page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </main>
  )
}

export default AdminApplicationsPage
