import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getMyApplications } from '../services/recruitmentService'

function CandidateApplicationsPage() {
  const [applications, setApplications] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadApplications() {
      try {
        const result = await getMyApplications({ page: 0, size: 20 })
        setApplications(result.content || [])
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load your applications.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadApplications()
  }, [])

  return <main className="workspace-page"><p className="eyebrow">Your activity</p><h1>My applications</h1>{error && <p className="error-message">{error}</p>}{isLoading ? <p className="page-state">Loading applications...</p> : applications.length === 0 ? <p className="empty-state">You have not applied to a job yet.</p> : <div className="item-list">{applications.map((application) => <article className="list-item" key={application.id}><div><p className="eyebrow">{application.status}</p><h2>Application {application.id}</h2><p>Job: {application.jobId} · Resume: {application.resumeId} · Submitted {new Date(application.submittedAt).toLocaleString()}</p></div><Link className="button-link" to={`/candidate/applications/${application.id}`}>View details</Link></article>)}</div>}</main>
}

export default CandidateApplicationsPage