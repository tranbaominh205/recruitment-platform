import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getPublishedJob } from '../services/jobService'
import { getMyResumes } from '../services/resumeService'
import { submitApplication } from '../services/recruitmentService'

function CandidateJobDetailPage() {
  const { jobId } = useParams()
  const navigate = useNavigate()
  const [job, setJob] = useState(null)
  const [resumes, setResumes] = useState([])
  const [selectedResumeId, setSelectedResumeId] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadDetails() {
      try {
        const [jobResult, resumeResult] = await Promise.all([getPublishedJob(jobId), getMyResumes()])
        setJob(jobResult)
        setResumes(resumeResult)
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load this job.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadDetails()
  }, [jobId])

  async function handleApply(event) {
    event.preventDefault()
    if (!selectedResumeId) {
      setError('Select a resume before submitting your application.')
      return
    }
    setError('')
    setIsSubmitting(true)
    try {
      await submitApplication({ jobId, resumeId: selectedResumeId })
      navigate('/candidate/applications')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to submit this application.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) return <main className="workspace-page"><p className="page-state">Loading job...</p></main>
  if (!job) return <main className="workspace-page"><p className="error-message">{error || 'Job not found.'}</p></main>

  return (
    <main className="workspace-page">
      <Link to="/candidate/jobs">Back to jobs</Link>
      <p className="eyebrow">Published opportunity</p>
      <h1>{job.title}</h1>
      <div className="detail-grid"><p><strong>Location</strong>{job.location || 'Not specified'}</p><p><strong>Employment</strong>{job.employmentType || 'Not specified'}</p><p><strong>Workplace</strong>{job.workplaceType || 'Not specified'}</p><p><strong>Education</strong>{job.requiredEducationLevel || 'Not specified'}</p></div>
      <section className="content-section"><h2>About the role</h2><p>{job.description || 'No description provided.'}</p></section>
      <section className="content-section"><h2>Requirements</h2><p>{job.requirements || 'No requirements provided.'}</p>{job.requiredSkills?.length > 0 && <p><strong>Skills:</strong> {job.requiredSkills.join(', ')}</p>}</section>
      <section className="apply-panel"><h2>Apply with a specific resume</h2>{error && <p className="error-message">{error}</p>}{resumes.length === 0 ? <><p className="empty-state">You need a resume before applying.</p><Link className="button-link" to="/candidate/resumes">Upload a resume</Link></> : <form onSubmit={handleApply}><label>Select resume<select value={selectedResumeId} onChange={(event) => setSelectedResumeId(event.target.value)} required><option value="">Choose one</option>{resumes.map((resume) => <option value={resume.id} key={resume.id}>{resume.displayName} ({resume.originalFileName})</option>)}</select></label><p className="muted">The selected resume ID is sent exactly as chosen. No resume is selected automatically.</p><button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Submitting...' : 'Submit application'}</button></form>}</section>
    </main>
  )
}

export default CandidateJobDetailPage