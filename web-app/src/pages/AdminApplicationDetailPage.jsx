import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminApplication } from '../services/recruitmentService'

function AdminApplicationDetailPage() {
  const { applicationId } = useParams()
  const [application, setApplication] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadApplication() {
      setIsLoading(true)
      setError('')
      try {
        setApplication(await getAdminApplication(applicationId))
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load application detail.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadApplication()
  }, [applicationId])

  if (isLoading) {
    return <main className="workspace-page"><p className="page-state">Loading application...</p></main>
  }

  if (!application) {
    return (
      <main className="workspace-page">
        <Link to="/admin/applications">Back to applications</Link>
        <p className="error-message">{error || 'Application not found.'}</p>
      </main>
    )
  }

  return (
    <main className="workspace-page">
      <Link to="/admin/applications">Back to applications</Link>
      <p className="eyebrow">Application detail</p>
      <h1>{application.id}</h1>
      {error && <p className="error-message">{error}</p>}
      <div className="detail-grid">
        <p><strong>id</strong>{application.id}</p>
        <p><strong>candidateId</strong>{application.candidateId}</p>
        <p><strong>jobId</strong>{application.jobId}</p>
        <p><strong>resumeId</strong>{application.resumeId}</p>
        <p><strong>status</strong>{application.status}</p>
        <p><strong>submittedAt</strong>{new Date(application.submittedAt).toLocaleString()}</p>
      </div>
    </main>
  )
}

export default AdminApplicationDetailPage
