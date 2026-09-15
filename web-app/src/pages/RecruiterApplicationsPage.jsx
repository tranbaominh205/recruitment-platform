import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getOwnedJob } from '../services/jobService'
import { getApiErrorMessage } from '../services/apiError'
import { getApplicationsForJob } from '../services/recruitmentService'

const PAGE_SIZE = 20
const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'SUBMITTED', label: 'New applicant' },
  { value: 'SCREENING', label: 'Screening' },
  { value: 'INTERVIEW', label: 'Interview' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'HIRED', label: 'Hired' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'WITHDRAWN', label: 'Withdrawn' }
]
const SORT_OPTIONS = [
  { value: 'desc', label: 'Newest first' },
  { value: 'asc', label: 'Oldest first' }
]

function RecruiterApplicationsPage() {
  const { jobId } = useParams()

  const [job, setJob] = useState(null)
  const [applications, setApplications] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [sortDirection, setSortDirection] = useState('desc')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [jobError, setJobError] = useState('')
  const [applicationError, setApplicationError] = useState('')
  const [isApplicationsLoading, setIsApplicationsLoading] = useState(true)

  useEffect(() => {
    let isActive = true

    async function loadJob() {
      setJobError('')
      try {
        const result = await getOwnedJob(jobId)
        if (isActive) {
          setJob(result)
        }
      } catch (requestError) {
        if (isActive) {
          setJobError(getApiErrorMessage(requestError, 'Unable to load job.'))
        }
      }
    }

    loadJob()

    return () => {
      isActive = false
    }
  }, [jobId])

  useEffect(() => {
    let isActive = true

    async function loadApplications() {
      setApplicationError('')
      setIsApplicationsLoading(true)

      const params = {
        sortDirection,
        page,
        size: PAGE_SIZE
      }

      if (statusFilter !== '') {
        params.status = statusFilter
      }

      try {
        const result = await getApplicationsForJob(jobId, params)
        if (isActive) {
          setApplications(result.content || [])
          setTotalPages(result.totalPages || 0)
          setTotalElements(result.totalElements || 0)
        }
      } catch (requestError) {
        if (isActive) {
          setApplications([])
          setTotalPages(0)
          setTotalElements(0)
          setApplicationError(getApiErrorMessage(requestError, 'Unable to load applications.'))
        }
      } finally {
        if (isActive) {
          setIsApplicationsLoading(false)
        }
      }
    }

    loadApplications()

    return () => {
      isActive = false
    }
  }, [jobId, statusFilter, sortDirection, page])

  const hasActiveStatusFilter = statusFilter !== ''
  const pageLabel = totalPages > 0 ? `Page ${page + 1} of ${totalPages}` : 'Page 0 of 0'
  const totalLabel = `${totalElements} application${totalElements === 1 ? '' : 's'}`

  return (
    <main className="workspace-page">
      <Link to={`/recruiter/jobs/${jobId}`}>Back to job</Link>
      <p className="eyebrow">Applications</p>
      <h1>{job ? job.title : 'Job applications'}</h1>

      {jobError && <p className="error-message">{jobError}</p>}
      {applicationError && <p className="error-message">{applicationError}</p>}

      <div className="page-heading">
        <p className="muted">{totalLabel}</p>
      </div>

      <div className="form-grid">
        <label>
          Status
          <select
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value)
              setPage(0)
            }}
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value || 'all'} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Sort
          <select
            value={sortDirection}
            onChange={(event) => {
              setSortDirection(event.target.value)
              setPage(0)
            }}
          >
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {isApplicationsLoading ? (
        <p className="page-state">Loading applications...</p>
      ) : applications.length === 0 ? (
        <p className="empty-state">
          {hasActiveStatusFilter ? 'No applications match this filter.' : 'No applications yet.'}
        </p>
      ) : (
        <div className="item-list">
          {applications.map((application) => (
            <article className="list-item" key={application.id}>
              <div>
                <p className="eyebrow">{application.status}</p>
                <h2>{application.id}</h2>
                <p>
                  Candidate: {application.candidateId}
                  <br />
                  Resume: {application.resumeId}
                  <br />
                  Submitted: {new Date(application.submittedAt).toLocaleString()}
                </p>
              </div>

              <Link className="button-link" to={`/recruiter/applications/${application.id}`}>
                Review
              </Link>
            </article>
          ))}
        </div>
      )}

      <div className="pager">
        <button
          type="button"
          className="button-secondary"
          disabled={isApplicationsLoading || page <= 0}
          onClick={() => setPage((currentPage) => currentPage - 1)}
        >
          Previous
        </button>
        <p className="muted">{pageLabel}</p>
        <button
          type="button"
          className="button-secondary"
          disabled={isApplicationsLoading || page >= totalPages - 1}
          onClick={() => setPage((currentPage) => currentPage + 1)}
        >
          Next
        </button>
      </div>
    </main>
  )
}

export default RecruiterApplicationsPage