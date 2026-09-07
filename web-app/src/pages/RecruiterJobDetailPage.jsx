import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { closeJob, getOwnedJob, publishJob } from '../services/jobService'

function RecruiterJobDetailPage() {
  const { jobId } = useParams(); const [job, setJob] = useState(null); const [error, setError] = useState(''); const [isMutating, setIsMutating] = useState(false)
  useEffect(() => { getOwnedJob(jobId).then(setJob).catch((requestError) => setError(getApiErrorMessage(requestError, 'Unable to load this job.'))) }, [jobId])
  async function mutate(action) { setError(''); setIsMutating(true); try { setJob(await action(jobId)) } catch (requestError) { setError(getApiErrorMessage(requestError, 'Unable to update job.')) } finally { setIsMutating(false) } }
  if (!job) return <main className="workspace-page"><p className={error ? 'error-message' : 'page-state'}>{error || 'Loading job...'}</p></main>
  return <main className="workspace-page"><Link to="/recruiter/jobs">Back to jobs</Link><div className="page-heading"><div><p className="eyebrow">{job.status}</p><h1>{job.title}</h1></div><div className="action-row">{job.status === 'DRAFT' && <Link className="button-link" to={`/recruiter/jobs/${job.id}/edit`}>Edit</Link>}{job.status === 'DRAFT' && <button type="button" disabled={isMutating} onClick={() => mutate(publishJob)}>Publish</button>}{job.status === 'PUBLISHED' && <button type="button" className="button-danger" disabled={isMutating} onClick={() => mutate(closeJob)}>Close</button>}</div></div>{error && <p className="error-message">{error}</p>}<div className="detail-grid"><p><strong>Domain</strong>{job.domain}</p><p><strong>Education</strong>{job.requiredEducationLevel}</p><p><strong>Experience</strong>{job.minimumYearsExperience} years</p><p><strong>Skills</strong>{job.requiredSkills?.join(', ')}</p><p><strong>Location</strong>{job.location || 'Not specified'}</p><p><strong>Workplace</strong>{job.workplaceType || 'Not specified'}</p></div><section className="content-section"><h2>Description</h2><p>{job.description}</p><h2>Requirements</h2><p>{job.requirements || 'No additional requirements.'}</p></section><Link className="button-link" to={`/recruiter/jobs/${job.id}/applications`}>Review applications</Link></main>
}

export default RecruiterJobDetailPage