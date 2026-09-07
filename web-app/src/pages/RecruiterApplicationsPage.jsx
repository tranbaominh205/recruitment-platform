import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getApplicationsForJob } from '../services/recruitmentService'
import { getOwnedJob } from '../services/jobService'

function RecruiterApplicationsPage() {
  const { jobId } = useParams(); const [job, setJob] = useState(null); const [applications, setApplications] = useState([]); const [error, setError] = useState(''); const [isLoading, setIsLoading] = useState(true)
  useEffect(() => { Promise.all([getOwnedJob(jobId), getApplicationsForJob(jobId, { page: 0, size: 50 })]).then(([jobResult, applicationResult]) => { setJob(jobResult); setApplications(applicationResult.content || []) }).catch((requestError) => setError(getApiErrorMessage(requestError, 'Unable to load applications.'))).finally(() => setIsLoading(false)) }, [jobId])
  return <main className="workspace-page"><Link to={`/recruiter/jobs/${jobId}`}>Back to job</Link><p className="eyebrow">Applications</p><h1>{job ? job.title : 'Job applications'}</h1>{error && <p className="error-message">{error}</p>}{isLoading ? <p className="page-state">Loading applications...</p> : applications.length === 0 ? <p className="empty-state">No applications yet.</p> : <div className="item-list">{applications.map((application) => <article className="list-item" key={application.id}><div><p className="eyebrow">{application.status}</p><h2>{application.id}</h2><p>Candidate: {application.candidateId}<br />Resume: {application.resumeId}<br />Submitted: {new Date(application.submittedAt).toLocaleString()}</p></div><Link className="button-link" to={`/recruiter/applications/${application.id}`}>Review</Link></article>)}</div>}</main>
}

export default RecruiterApplicationsPage