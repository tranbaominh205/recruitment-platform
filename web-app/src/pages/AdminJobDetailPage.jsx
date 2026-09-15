import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../configurations/useAuth'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminJob, updateAdminJobModeration } from '../services/jobService'

function AdminJobDetailPage() {
  const { jobId } = useParams()
  const { account } = useAuth()
  const [job, setJob] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [moderationStatus, setModerationStatus] = useState('ACTIVE')
  const [moderationReason, setModerationReason] = useState('')
  const [moderationError, setModerationError] = useState('')
  const [moderationSuccess, setModerationSuccess] = useState('')
  const [isMutating, setIsMutating] = useState(false)

  const permissions = account?.permissions ?? []
  const canModerateJob = permissions.includes('JOB_MODERATE')

  useEffect(() => {
    async function loadJob() {
      setIsLoading(true)
      setError('')
      try {
        const loadedJob = await getAdminJob(jobId)
        setJob(loadedJob)
        setModerationStatus(loadedJob.moderationStatus || 'ACTIVE')
        setModerationReason(loadedJob.moderationReason || '')
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load job detail.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadJob()
  }, [jobId])

  async function handleModerationSubmit(event) {
    event.preventDefault()
    const nextReason = moderationReason.trim()

    if (['HIDDEN', 'REMOVED'].includes(moderationStatus) && !nextReason) {
      setModerationError('A non-blank reason is required for HIDDEN and REMOVED.')
      setModerationSuccess('')
      return
    }

    setIsMutating(true)
    setModerationError('')
    setModerationSuccess('')

    try {
      const updatedJob = await updateAdminJobModeration(jobId, {
        moderationStatus,
        reason: nextReason || undefined,
      })
      setJob(updatedJob)
      setModerationStatus(updatedJob.moderationStatus || 'ACTIVE')
      setModerationReason(updatedJob.moderationReason || '')
      setModerationSuccess('Job moderation updated successfully.')
    } catch (requestError) {
      setModerationError(getApiErrorMessage(requestError, 'Unable to update job moderation.'))
    } finally {
      setIsMutating(false)
    }
  }

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
        <p><strong>moderationStatus</strong>{job.moderationStatus || 'ACTIVE'}</p>
        <p><strong>moderationReason</strong>{job.moderationReason || 'Not set'}</p>
        <p><strong>moderatedAt</strong>{job.moderatedAt ? new Date(job.moderatedAt).toLocaleString() : 'Not set'}</p>
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

      <section className="content-section">
        <h2>Moderation</h2>
        {!canModerateJob ? (
          <p className="content-note">Read-only access. JOB_MODERATE is required to change job moderation.</p>
        ) : (
          <form onSubmit={handleModerationSubmit}>
            {moderationError && <p className="error-message">{moderationError}</p>}
            {moderationSuccess && <p className="success-message">{moderationSuccess}</p>}
            <div className="form-grid">
              <label>
                Moderation status
                <select value={moderationStatus} onChange={(event) => setModerationStatus(event.target.value)}>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="HIDDEN">HIDDEN</option>
                  <option value="REMOVED">REMOVED</option>
                </select>
              </label>
              <label className="full-width">
                Reason
                <textarea
                  rows="4"
                  maxLength={1000}
                  value={moderationReason}
                  onChange={(event) => setModerationReason(event.target.value)}
                  placeholder={moderationStatus === 'ACTIVE' ? 'Optional for ACTIVE' : 'Required for HIDDEN and REMOVED'}
                />
              </label>
              <button type="submit" disabled={isMutating}>
                {isMutating ? 'Updating...' : 'Update moderation'}
              </button>
            </div>
          </form>
        )}
      </section>
    </main>
  )
}

export default AdminJobDetailPage
