import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminAccounts } from '../services/identityService'

const defaultPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

function AdminAccountsPage() {
  const [filters, setFilters] = useState({ keyword: '', role: '', enabled: '' })
  const [resultPage, setResultPage] = useState(defaultPage)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadAccounts(nextPage = 0, nextFilters = filters) {
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

      if (nextFilters.role) {
        params.role = nextFilters.role
      }

      if (nextFilters.enabled) {
        params.enabled = nextFilters.enabled === 'true'
      }

      const response = await getAdminAccounts(params)
      setResultPage({
        content: response.content || [],
        page: response.page || 0,
        size: response.size || 20,
        totalElements: response.totalElements || 0,
        totalPages: response.totalPages || 0,
      })
    } catch (requestError) {
      setResultPage(defaultPage)
      setError(getApiErrorMessage(requestError, 'Unable to load admin accounts.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadAccounts(0)
  }, [])

  function updateFilter(event) {
    setFilters({ ...filters, [event.target.name]: event.target.value })
    setResultPage({ ...resultPage, page: 0 })
  }

  function handleSearch(event) {
    event.preventDefault()
    loadAccounts(0)
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Accounts</p>
      <h1>Manage accounts</h1>
      {error && <p className="error-message">{error}</p>}
      <form className="search-grid" onSubmit={handleSearch}>
        <label>
          Keyword
          <input
            name="keyword"
            value={filters.keyword}
            onChange={updateFilter}
            placeholder="Email"
          />
        </label>
        <label>
          Role
          <select name="role" value={filters.role} onChange={updateFilter}>
            <option value="">All roles</option>
            <option value="CANDIDATE">CANDIDATE</option>
            <option value="RECRUITER">RECRUITER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </label>
        <label>
          Enabled
          <select name="enabled" value={filters.enabled} onChange={updateFilter}>
            <option value="">All</option>
            <option value="true">Enabled</option>
            <option value="false">Disabled</option>
          </select>
        </label>
        <button type="submit">Search accounts</button>
      </form>

      {isLoading ? (
        <p className="page-state">Loading accounts...</p>
      ) : resultPage.content.length === 0 ? (
        <p className="empty-state">No accounts match your filters.</p>
      ) : (
        <div className="item-list">
          {resultPage.content.map((account) => (
            <article className="list-item" key={account.id}>
              <div>
                <p className="eyebrow">{account.role}</p>
                <h2>{account.email}</h2>
                <p>
                  {account.enabled ? 'Enabled' : 'Disabled'} · Created{' '}
                  {new Date(account.createdAt).toLocaleString()}
                </p>
                <p>{account.id}</p>
              </div>
              <Link className="button-link" to={`/admin/accounts/${account.id}`}>View detail</Link>
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
            onClick={() => loadAccounts(resultPage.page - 1)}
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
            onClick={() => loadAccounts(resultPage.page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </main>
  )
}

export default AdminAccountsPage
