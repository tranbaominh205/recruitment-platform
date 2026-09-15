import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminCompanies } from '../services/employerService'

const defaultPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

function AdminCompaniesPage() {
  const [filters, setFilters] = useState({ keyword: '' })
  const [resultPage, setResultPage] = useState(defaultPage)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadCompanies(nextPage = 0, nextFilters = filters) {
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

      const response = await getAdminCompanies(params)
      setResultPage({
        content: response.content || [],
        page: response.page || 0,
        size: response.size || 20,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })
    } catch (requestError) {
      setResultPage(defaultPage)
      setError(getApiErrorMessage(requestError, 'Unable to load companies.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadCompanies(0)
  }, [])

  function updateFilter(event) {
    setFilters({ ...filters, [event.target.name]: event.target.value })
    setResultPage({ ...resultPage, page: 0 })
  }

  function handleSearch(event) {
    event.preventDefault()
    loadCompanies(0)
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Companies</p>
      <h1>Manage companies</h1>
      {error && <p className="error-message">{error}</p>}
      <form className="search-grid" onSubmit={handleSearch}>
        <label>
          Keyword
          <input
            name="keyword"
            value={filters.keyword}
            onChange={updateFilter}
            placeholder="Company name"
          />
        </label>
        <button type="submit">Search companies</button>
      </form>

      {isLoading ? (
        <p className="page-state">Loading companies...</p>
      ) : resultPage.content.length === 0 ? (
        <p className="empty-state">No companies match your filters.</p>
      ) : (
        <div className="item-list">
          {resultPage.content.map((company) => (
            <article className="list-item" key={company.id}>
              <div>
                <p className="eyebrow">{company.industry || 'Industry not set'}</p>
                <h2>{company.name}</h2>
                <p>{company.location || 'Location not set'}</p>
                <p>{company.id}</p>
              </div>
              <Link className="button-link" to={`/admin/companies/${company.id}`}>View detail</Link>
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
            onClick={() => loadCompanies(resultPage.page - 1)}
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
            onClick={() => loadCompanies(resultPage.page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </main>
  )
}

export default AdminCompaniesPage
