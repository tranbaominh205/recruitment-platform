import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getMyJobs } from '../services/jobService'

function RecruiterJobsPage() {
  const [jobs, setJobs] = useState([]); const [isLoading, setIsLoading] = useState(true); const [error, setError] = useState('')
  useEffect(() => { getMyJobs({ page: 0, size: 50 }).then((result) => setJobs(result.content || [])).catch((requestError) => setError(getApiErrorMessage(requestError, 'Unable to load jobs.'))).finally(() => setIsLoading(false)) }, [])
  return <main className="workspace-page"><div className="page-heading"><div><p className="eyebrow">Jobs</p><h1>My jobs</h1></div><Link className="button-link" to="/recruiter/jobs/new">Create draft</Link></div>{error && <p className="error-message">{error}</p>}{isLoading ? <p className="page-state">Loading jobs...</p> : jobs.length === 0 ? <p className="empty-state">No jobs yet. Create your first draft.</p> : <div className="item-list">{jobs.map((job) => <article className="list-item" key={job.id}><div><p className="eyebrow">{job.status}</p><h2>{job.title}</h2><p>{job.domain} · {job.location || 'Location not set'} · {job.requiredSkills?.join(', ')}</p></div><Link className="button-link" to={`/recruiter/jobs/${job.id}`}>Open job</Link></article>)}</div>}</main>
}

export default RecruiterJobsPage