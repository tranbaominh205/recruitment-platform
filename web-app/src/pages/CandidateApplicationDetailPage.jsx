import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { subscribeToNotificationCreated } from '../services/accountSseService'
import { getApplication, getInterview, withdrawApplication } from '../services/recruitmentService'

function CandidateApplicationDetailPage() {
  const { applicationId } = useParams()
  const [application, setApplication] = useState(null)
  const [interview, setInterview] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isWithdrawing, setIsWithdrawing] = useState(false)
  const [error, setError] = useState('')
  const requestSequenceRef = useRef(0)
  const isMountedRef = useRef(false)

  const loadDetails = useCallback(async ({ showPageLoading }) => {
    const currentRequest = requestSequenceRef.current + 1
    requestSequenceRef.current = currentRequest

    if (showPageLoading) {
      setIsLoading(true)
      setError('')
      setApplication(null)
      setInterview(null)
    }

    try {
      const result = await getApplication(applicationId)
      let nextInterview = null

      try {
        nextInterview = await getInterview(applicationId)
      } catch (interviewError) {
        if (interviewError.response?.status !== 404) throw interviewError
        nextInterview = null
      }

      if (!isMountedRef.current || currentRequest !== requestSequenceRef.current) {
        return
      }

      setApplication(result)
      setInterview(nextInterview)
      setError('')
    } catch (requestError) {
      if (!isMountedRef.current || currentRequest !== requestSequenceRef.current) {
        return
      }

      setError(getApiErrorMessage(requestError, 'Unable to load this application.'))
    } finally {
      if (showPageLoading && isMountedRef.current && currentRequest === requestSequenceRef.current) {
        setIsLoading(false)
      }
    }
  }, [applicationId])

  useEffect(() => {
    isMountedRef.current = true
    return () => {
      isMountedRef.current = false
    }
  }, [])

  useEffect(() => {
    loadDetails({ showPageLoading: true })
  }, [applicationId, loadDetails])

  useEffect(() => {
    const unsubscribe = subscribeToNotificationCreated((payload) => {
      const notificationType = payload?.notificationType
      const referenceId = payload?.referenceId

      if (!referenceId) {
        return
      }

      if (String(referenceId) !== applicationId) {
        return
      }

      const isRelevantNotification =
        notificationType === 'APPLICATION_STATUS_CHANGED'
        || notificationType === 'INTERVIEW_SCHEDULED'

      if (!isRelevantNotification) {
        return
      }

      loadDetails({ showPageLoading: false })
    })

    return unsubscribe
  }, [applicationId, loadDetails])

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