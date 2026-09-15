import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getMyJobRecommendations } from '../services/candidateService'

const defaultPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

const reasonLabels = {
  TITLE_MATCH: 'Title match',
  LOCATION_MATCH: 'Location match',
  EMPLOYMENT_TYPE_MATCH: 'Employment type match',
  WORKPLACE_TYPE_MATCH: 'Workplace type match',
}

function toReasonText(reason) {
  if (!reason) {
    return 'Unknown match'
  }

  return reasonLabels[reason] || reason
}

function formatScore(score) {
  if (score === null || score === undefined || Number.isNaN(Number(score))) {
    return '0.00%'
  }

  return `${Number(score).toFixed(2)}%`
}

function CandidateRecommendationsPage() {
  const [recommendations, setRecommendations] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadRecommendations(nextPage = 0) {
    setIsLoading(true)
    setError('')

    try {
      const result = await getMyJobRecommendations({ page: nextPage, size: 20 })
      const normalized = result || defaultPage
      setRecommendations(normalized.content || [])
      setPage(normalized.page || 0)
      setTotalPages(normalized.totalPages || 0)
      setTotalElements(normalized.totalElements || 0)
    } catch (requestError) {
      setRecommendations([])
      setPage(0)
      setTotalPages(0)
      setTotalElements(0)
      setError(getApiErrorMessage(requestError, 'Unable to load recommended jobs.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadRecommendations(0)
  }, [])

  return (
    <main className="workspace-page">
      <p className="eyebrow">Recommendations</p>
      <h1>Recommended jobs</h1>
      <p className="muted">Jobs ranked from your profile preferences.</p>
      {error && <p className="error-message">{error}</p>}

      {isLoading ? (
        <p className="page-state">Loading recommended jobs...</p>
      ) : recommendations.length === 0 ? (
        <p className="empty-state">No recommended jobs are available right now.</p>
      ) : (
        <div className="item-list">
          {recommendations.map((item) => (
            <article className="list-item" key={item.id}>
              <div>
                <p className="eyebrow">{formatScore(item.recommendationScore)}</p>
                <h2>{item.title || 'Untitled job'}</h2>
                <p>
                  {item.location || 'Location not specified'} · {item.employmentType || 'Employment type not specified'} ·{' '}
                  {item.workplaceType || 'Workplace type not specified'}
                </p>
                {Array.isArray(item.recommendationReasons) && item.recommendationReasons.length > 0 ? (
                  <p>Reasons: {item.recommendationReasons.map(toReasonText).join(', ')}</p>
                ) : (
                  <p className="muted">No preference matches yet</p>
                )}
              </div>
              <Link className="button-link" to={`/candidate/jobs/${item.id}`}>
                View job
              </Link>
            </article>
          ))}
        </div>
      )}

      {!isLoading && totalPages > 0 && (
        <div className="pager">
          <button
            type="button"
            className="button-secondary"
            disabled={page <= 0}
            onClick={() => loadRecommendations(page - 1)}
          >
            Previous
          </button>
          <span>
            Page {page + 1} of {totalPages} ({totalElements} total)
          </span>
          <button
            type="button"
            className="button-secondary"
            disabled={page >= totalPages - 1}
            onClick={() => loadRecommendations(page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </main>
  )
}

export default CandidateRecommendationsPage
