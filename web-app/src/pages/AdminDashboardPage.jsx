import { useCallback, useEffect, useState } from 'react'
import { getAdminEmployerStatistics } from '../services/employerService'
import { getAdminAccountStatistics } from '../services/identityService'
import { getAdminJobStatistics } from '../services/jobService'
import { getAdminMatchingStatistics } from '../services/matchingService'
import { getAdminNotificationStatistics } from '../services/notificationService'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminRecruitmentStatistics } from '../services/recruitmentService'
import { getAdminResumeStatistics } from '../services/resumeService'

const statisticSections = [
  {
    title: 'Identity statistics',
    key: 'identity',
    fields: [
      'totalAccounts',
      'candidateAccounts',
      'recruiterAccounts',
      'adminAccounts',
      'enabledAccounts',
      'disabledAccounts',
    ],
  },
  {
    title: 'Employer statistics',
    key: 'employer',
    fields: ['totalCompanies'],
  },
  {
    title: 'Job statistics',
    key: 'job',
    fields: ['totalJobs', 'draftJobs', 'publishedJobs', 'closedJobs'],
  },
  {
    title: 'Recruitment statistics',
    key: 'recruitment',
    fields: [
      'totalApplications',
      'submittedApplications',
      'screeningApplications',
      'interviewApplications',
      'offerApplications',
      'hiredApplications',
      'rejectedApplications',
      'withdrawnApplications',
      'totalInterviews',
    ],
  },
  {
    title: 'Resume statistics',
    key: 'resume',
    fields: ['totalResumes', 'activeResumes', 'archivedResumes'],
  },
  {
    title: 'Matching statistics',
    key: 'matching',
    fields: ['totalMatchResults'],
  },
  {
    title: 'Notification statistics',
    key: 'notification',
    fields: [
      'totalNotifications',
      'readNotifications',
      'unreadNotifications',
      'applicationStatusChangedNotifications',
      'interviewScheduledNotifications',
    ],
  },
]

function AdminDashboardPage() {
  const [statistics, setStatistics] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const loadStatistics = useCallback(async () => {
    setIsLoading(true)
    setError('')

    try {
      const [
        identity,
        employer,
        job,
        recruitment,
        resume,
        matching,
        notification,
      ] = await Promise.all([
        getAdminAccountStatistics(),
        getAdminEmployerStatistics(),
        getAdminJobStatistics(),
        getAdminRecruitmentStatistics(),
        getAdminResumeStatistics(),
        getAdminMatchingStatistics(),
        getAdminNotificationStatistics(),
      ])

      setStatistics({
        identity,
        employer,
        job,
        recruitment,
        resume,
        matching,
        notification,
      })
    } catch (requestError) {
      setStatistics(null)
      setError(getApiErrorMessage(requestError, 'Unable to load admin statistics.'))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    loadStatistics()
  }, [loadStatistics])

  return (
    <main className="workspace-page">
      <p className="eyebrow">Overview</p>
      <h1>Platform statistics</h1>
      {error && <p className="error-message">{error}</p>}
      {isLoading ? (
        <p className="page-state">Loading platform statistics...</p>
      ) : error ? (
        <button type="button" className="button-secondary" onClick={loadStatistics}>Retry</button>
      ) : (
        statisticSections.map((section) => (
          <section className="content-section" key={section.key}>
            <h2>{section.title}</h2>
            <div className="detail-grid">
              {section.fields.map((fieldName) => (
                <p key={fieldName}>
                  <strong>{fieldName}</strong>
                  {statistics?.[section.key]?.[fieldName] ?? 0}
                </p>
              ))}
            </div>
          </section>
        ))
      )}
    </main>
  )
}

export default AdminDashboardPage
