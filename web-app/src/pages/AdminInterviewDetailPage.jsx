import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminInterview } from '../services/recruitmentService'

function AdminInterviewDetailPage() {
  const { interviewId } = useParams()
  const [interview, setInterview] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadInterview() {
      setIsLoading(true)
      setError('')
      try {
        setInterview(await getAdminInterview(interviewId))
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load interview detail.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadInterview()
  }, [interviewId])

  if (isLoading) {
    return <main className="workspace-page"><p className="page-state">Loading interview...</p></main>
  }

  if (!interview) {
    return (
      <main className="workspace-page">
        <Link to="/admin/interviews">Back to interviews</Link>
        <p className="error-message">{error || 'Interview not found.'}</p>
      </main>
    )
  }

  return (
    <main className="workspace-page">
      <Link to="/admin/interviews">Back to interviews</Link>
      <p className="eyebrow">Interview detail</p>
      <h1>{interview.id}</h1>
      {error && <p className="error-message">{error}</p>}
      <div className="detail-grid">
        <p><strong>id</strong>{interview.id}</p>
        <p><strong>applicationId</strong>{interview.applicationId}</p>
        <p><strong>scheduledAt</strong>{new Date(interview.scheduledAt).toLocaleString()}</p>
        <p><strong>location</strong>{interview.location || 'Not set'}</p>
        <p className="full-width"><strong>note</strong>{interview.note || 'Not set'}</p>
        <p><strong>createdAt</strong>{new Date(interview.createdAt).toLocaleString()}</p>
      </div>
    </main>
  )
}

export default AdminInterviewDetailPage
