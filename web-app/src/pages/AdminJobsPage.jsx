import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminJobs } from '../services/jobService'

const defaultPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

function AdminJobsPage() {
  const [filters, setFilters] = useState({ keyword: '', status: '' })
  const [resultPage, setResultPage] = useState(defaultPage)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadJobs(nextPage = 0, nextFilters = filters) {
    setIsLoading(true)
    setError('')
    try {
      const params = {
        page: nextPage,
        size: resultPage.size || 20,
      }

      if (nextFilters.keyword.trim()) {
        params.keyword = nextFilters.keyword.trim()
      }

      if (nextFilters.status) {
        params.status = nextFilters.status
      }

      const response = await getAdminJobs(params)
      setResultPage({
        content: response.content || [],
        page: response.page || 0,
        size: response.size || 20,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })
    } catch (requestError) {
      setResultPage(defaultPage)
      setError(getApiErrorMessage(requestError, 'Unable to load jobs.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadJobs(0)
  }, [])

  function updateFilter(event) {
    setFilters({ ...filters, [event.target.name]: event.target.value })
    setResultPage({ ...resultPage, page: 0 })
  }

  function handleSearch(event) {
    event.preventDefault()
    loadJobs(0)
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Jobs</p>
      <h1>Manage jobs</h1>
      {error && <p className="error-message">{error}</p>}
      <form className="search-grid" onSubmit={handleSearch}>
        <label>
          Keyword
          <input
            name="keyword"
            value={filters.keyword}
            onChange={updateFilter}
            placeholder="Job title"
          />
        </label>
        <label>
          Status
          <select name="status" value={filters.status} onChange={updateFilter}>
            <option value="">All statuses</option>
            <option value="DRAFT">DRAFT</option>
            <option value="PUBLISHED">PUBLISHED</option>
            <option value="CLOSED">CLOSED</option>
          </select>
        </label>
        <button type="submit">Search jobs</button>
      </form>

      {isLoading ? (
        <p className="page-state">Loading jobs...</p>
      ) : resultPage.content.length === 0 ? (
        <p className="empty-state">No jobs match your filters.</p>
      ) : (
        <div className="item-list">
          {resultPage.content.map((job) => (
            <article className="list-item" key={job.id}>
              <div>
                <p className="eyebrow">{job.status}</p>
                <h2>{job.title}</h2>
                <p>{job.domain || 'Domain not set'} · {job.location || 'Location not set'}</p>
                <p>{job.id}</p>
              </div>
              <Link className="button-link" to={`/admin/jobs/${job.id}`}>View detail</Link>
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
            onClick={() => loadJobs(resultPage.page - 1)}
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
            onClick={() => loadJobs(resultPage.page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </main>
  )
}

export default AdminJobsPage
