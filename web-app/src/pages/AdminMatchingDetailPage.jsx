import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminMatchResult } from '../services/matchingService'

function formatListField(value) {
  if (Array.isArray(value)) {
    return value.length > 0 ? value.join(', ') : 'None'
  }

  if (typeof value === 'string') {
    return value || 'None'
  }

  return 'None'
}

function AdminMatchingDetailPage() {
  const { applicationId } = useParams()
  const [result, setResult] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadResult() {
      setIsLoading(true)
      setError('')
      try {
        setResult(await getAdminMatchResult(applicationId))
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load matching detail.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadResult()
  }, [applicationId])

  if (isLoading) {
    return <main className="workspace-page"><p className="page-state">Loading matching detail...</p></main>
  }

  if (!result) {
    return (
      <main className="workspace-page">
        <Link to="/admin/matching">Back to matching results</Link>
        <p className="error-message">{error || 'Matching result not found.'}</p>
      </main>
    )
  }

  return (
    <main className="workspace-page">
      <Link to="/admin/matching">Back to matching results</Link>
      <p className="eyebrow">Matching detail</p>
      <h1>{result.applicationId}</h1>
      {error && <p className="error-message">{error}</p>}
      <div className="detail-grid">
        <p><strong>applicationId</strong>{result.applicationId}</p>
        <p><strong>candidateId</strong>{result.candidateId}</p>
        <p><strong>jobId</strong>{result.jobId}</p>
        <p><strong>resumeId</strong>{result.resumeId}</p>
        <p><strong>totalScore</strong>{result.totalScore}</p>
        <p><strong>skillsScore</strong>{result.skillsScore}</p>
        <p><strong>experienceScore</strong>{result.experienceScore}</p>
        <p><strong>educationScore</strong>{result.educationScore}</p>
        <p><strong>titleScore</strong>{result.titleScore}</p>
        <p><strong>domainScore</strong>{result.domainScore}</p>
        <p><strong>scoringVersion</strong>{result.scoringVersion}</p>
        <p><strong>scoredAt</strong>{new Date(result.scoredAt).toLocaleString()}</p>
        <p className="full-width"><strong>matchedSkills</strong>{formatListField(result.matchedSkills)}</p>
        <p className="full-width"><strong>missingSkills</strong>{formatListField(result.missingSkills)}</p>
      </div>
      <section className="content-section">
        <h2>Explanation</h2>
        {result.explanation ? (
          <div className="detail-grid">
            <p className="full-width"><strong>summary</strong>{result.explanation.summary || 'Not set'}</p>
            <p className="full-width"><strong>strengths</strong>{formatListField(result.explanation.strengths)}</p>
            <p className="full-width"><strong>gaps</strong>{formatListField(result.explanation.gaps)}</p>
            <p><strong>model</strong>{result.explanation.model || 'Not set'}</p>
            <p>
              <strong>generatedAt</strong>
              {result.explanation.generatedAt ? new Date(result.explanation.generatedAt).toLocaleString() : 'Not set'}
            </p>
          </div>
        ) : (
          <p className="empty-state">Explanation unavailable.</p>
        )}
      </section>
    </main>
  )
}

export default AdminMatchingDetailPage
