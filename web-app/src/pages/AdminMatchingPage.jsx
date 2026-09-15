import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminMatchResults } from '../services/matchingService'

const defaultPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

function AdminMatchingPage() {
  const [filters, setFilters] = useState({
    jobId: '',
    candidateId: '',
    resumeId: '',
  })
  const [resultPage, setResultPage] = useState(defaultPage)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadResults(nextPage = 0, nextFilters = filters) {
    setIsLoading(true)
    setError('')
    try {
      const params = {
        page: nextPage,
        size: resultPage.size || 20,
      }

      if (nextFilters.jobId.trim()) {
        params.jobId = nextFilters.jobId.trim()
      }

      if (nextFilters.candidateId.trim()) {
        params.candidateId = nextFilters.candidateId.trim()
      }

      if (nextFilters.resumeId.trim()) {
        params.resumeId = nextFilters.resumeId.trim()
      }

      const response = await getAdminMatchResults(params)
      setResultPage({
        content: response.content || [],
        page: response.page || 0,
        size: response.size || 20,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })
    } catch (requestError) {
      setResultPage(defaultPage)
      setError(getApiErrorMessage(requestError, 'Unable to load matching results.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadResults(0)
  }, [])

  function updateFilter(event) {
    setFilters({ ...filters, [event.target.name]: event.target.value })
    setResultPage({ ...resultPage, page: 0 })
  }

  function handleSearch(event) {
    event.preventDefault()
    loadResults(0)
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Matching</p>
      <h1>Matching results</h1>
      {error && <p className="error-message">{error}</p>}
      <form className="search-grid" onSubmit={handleSearch}>
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
        <label>
          Resume ID
          <input
            name="resumeId"
            value={filters.resumeId}
            onChange={updateFilter}
            placeholder="Resume UUID"
          />
        </label>
        <button type="submit">Search matching results</button>
      </form>

      {isLoading ? (
        <p className="page-state">Loading matching results...</p>
      ) : resultPage.content.length === 0 ? (
        <p className="empty-state">No matching results found.</p>
      ) : (
        <div className="item-list">
          {resultPage.content.map((result) => (
            <article className="list-item" key={result.applicationId}>
              <div>
                <p className="eyebrow">Score {result.totalScore}</p>
                <h2>{result.applicationId}</h2>
                <p>
                  candidateId: {result.candidateId}
                  <br />
                  jobId: {result.jobId}
                  <br />
                  resumeId: {result.resumeId}
                  <br />
                  scoredAt: {new Date(result.scoredAt).toLocaleString()}
                </p>
              </div>
              <Link className="button-link" to={`/admin/matching/${result.applicationId}`}>View detail</Link>
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
            onClick={() => loadResults(resultPage.page - 1)}
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
            onClick={() => loadResults(resultPage.page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </main>
  )
}

export default AdminMatchingPage
