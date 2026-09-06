import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getApplication, getInterview, withdrawApplication } from '../services/recruitmentService'

function CandidateApplicationDetailPage() {
  const { applicationId } = useParams()
  const [application, setApplication] = useState(null)
  const [interview, setInterview] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isWithdrawing, setIsWithdrawing] = useState(false)
  const [error, setError] = useState('')

  async function loadDetails() {
    try {
      const result = await getApplication(applicationId)
      setApplication(result)
      try {
        setInterview(await getInterview(applicationId))
      } catch (interviewError) {
        if (interviewError.response?.status !== 404) throw interviewError
      }
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to load this application.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { loadDetails() }, [applicationId])

  async function handleWithdraw() {
    setError('')
    setIsWithdrawing(true)
    try {
      setApplication(await withdrawApplication(applicationId))
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to withdraw this application.'))
    } finally {
      setIsWithdrawing(false)
    }
  }

  if (isLoading) return <main className="workspace-page"><p className="page-state">Loading application...</p></main>
  if (!application) return <main className="workspace-page"><p className="error-message">{error || 'Application not found.'}</p></main>
  const canWithdraw = ['SUBMITTED', 'SCREENING', 'INTERVIEW', 'OFFER'].includes(application.status)

  return <main className="workspace-page"><Link to="/candidate/applications">Back to applications</Link><p className="eyebrow">Application detail</p><h1>{application.status}</h1>{error && <p className="error-message">{error}</p>}<div className="detail-grid"><p><strong>Application ID</strong>{application.id}</p><p><strong>Job ID</strong>{application.jobId}</p><p><strong>Submitted resume ID</strong>{application.resumeId}</p><p><strong>Submitted</strong>{new Date(application.submittedAt).toLocaleString()}</p></div>{canWithdraw && <button type="button" className="button-danger" onClick={handleWithdraw} disabled={isWithdrawing}>{isWithdrawing ? 'Withdrawing...' : 'Withdraw application'}</button>}<section className="content-section"><h2>Interview</h2>{interview ? <div className="interview-card"><p><strong>Scheduled:</strong> {new Date(interview.scheduledAt).toLocaleString()}</p><p><strong>Location:</strong> {interview.location}</p>{interview.note && <p><strong>Note:</strong> {interview.note}</p>}</div> : <p className="empty-state">No interview has been scheduled for this application.</p>}</section></main>
}

export default CandidateApplicationDetailPage