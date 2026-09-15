import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminJob } from '../services/jobService'

function AdminJobDetailPage() {
  const { jobId } = useParams()
  const [job, setJob] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadJob() {
      setIsLoading(true)
      setError('')
      try {
        setJob(await getAdminJob(jobId))
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load job detail.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadJob()
  }, [jobId])

  if (isLoading) {
    return <main className="workspace-page"><p className="page-state">Loading job...</p></main>
  }

  if (!job) {
    return (
      <main className="workspace-page">
        <Link to="/admin/jobs">Back to jobs</Link>
        <p className="error-message">{error || 'Job not found.'}</p>
      </main>
    )
  }

  return (
    <main className="workspace-page">
      <Link to="/admin/jobs">Back to jobs</Link>
      <p className="eyebrow">Job detail</p>
      <h1>{job.title}</h1>
      {error && <p className="error-message">{error}</p>}
      <div className="detail-grid">
        <p><strong>id</strong>{job.id}</p>
        <p><strong>companyId</strong>{job.companyId}</p>
        <p><strong>status</strong>{job.status}</p>
        <p><strong>domain</strong>{job.domain || 'Not set'}</p>
        <p><strong>location</strong>{job.location || 'Not set'}</p>
        <p><strong>employmentType</strong>{job.employmentType || 'Not set'}</p>
        <p><strong>workplaceType</strong>{job.workplaceType || 'Not set'}</p>
        <p><strong>minimumYearsExperience</strong>{job.minimumYearsExperience ?? 'Not set'}</p>
        <p><strong>requiredEducationLevel</strong>{job.requiredEducationLevel || 'Not set'}</p>
        <p><strong>salaryMin</strong>{job.salaryMin ?? 'Not set'}</p>
        <p><strong>salaryMax</strong>{job.salaryMax ?? 'Not set'}</p>
        <p><strong>createdAt</strong>{new Date(job.createdAt).toLocaleString()}</p>
        <p><strong>updatedAt</strong>{new Date(job.updatedAt).toLocaleString()}</p>
        <p className="full-width">
          <strong>requiredSkills</strong>
          {job.requiredSkills?.length ? job.requiredSkills.join(', ') : 'None'}
        </p>
      </div>
      <section className="content-section">
        <h2>Description</h2>
        <p>{job.description}</p>
        <h2>Requirements</h2>
        <p>{job.requirements || 'No additional requirements.'}</p>
      </section>
    </main>
  )
}

export default AdminJobDetailPage
