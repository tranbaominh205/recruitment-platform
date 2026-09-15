import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminResumes } from '../services/resumeService'

const defaultPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

function AdminResumesPage() {
  const [filters, setFilters] = useState({
    status: '',
    ownerAccountId: '',
  })
  const [resultPage, setResultPage] = useState(defaultPage)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadResumes(nextPage = 0, nextFilters = filters) {
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

      if (nextFilters.ownerAccountId.trim()) {
        params.ownerAccountId = nextFilters.ownerAccountId.trim()
      }

      const response = await getAdminResumes(params)
      setResultPage({
        content: response.content || [],
        page: response.page || 0,
        size: response.size || 20,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })
    } catch (requestError) {
      setResultPage(defaultPage)
      setError(getApiErrorMessage(requestError, 'Unable to load resumes.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadResumes(0)
  }, [])

  function updateFilter(event) {
    setFilters({ ...filters, [event.target.name]: event.target.value })
    setResultPage({ ...resultPage, page: 0 })
  }

  function handleSearch(event) {
    event.preventDefault()
    loadResumes(0)
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Resumes</p>
      <h1>Manage resumes</h1>
      {error && <p className="error-message">{error}</p>}
      <form className="search-grid" onSubmit={handleSearch}>
        <label>
          Status
          <input
            name="status"
            value={filters.status}
            onChange={updateFilter}
            placeholder="ACTIVE"
          />
        </label>
        <label>
          Owner Account ID
          <input
            name="ownerAccountId"
            value={filters.ownerAccountId}
            onChange={updateFilter}
            placeholder="Identity account UUID"
          />
        </label>
        <button type="submit">Search resumes</button>
      </form>

      {isLoading ? (
        <p className="page-state">Loading resumes...</p>
      ) : resultPage.content.length === 0 ? (
        <p className="empty-state">No resumes match your filters.</p>
      ) : (
        <div className="item-list">
          {resultPage.content.map((resume) => (
            <article className="list-item" key={resume.id}>
              <div>
                <p className="eyebrow">{resume.status}</p>
                <h2>{resume.displayName}</h2>
                <p>
                  id: {resume.id}
                  <br />
                  ownerAccountId: {resume.ownerAccountId}
                  <br />
                  originalFileName: {resume.originalFileName}
                </p>
              </div>
              <Link className="button-link" to={`/admin/resumes/${resume.id}`}>View detail</Link>
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
            onClick={() => loadResumes(resultPage.page - 1)}
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
            onClick={() => loadResumes(resultPage.page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </main>
  )
}

export default AdminResumesPage
