import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../configurations/useAuth'
import { getApiErrorMessage } from '../services/apiError'
import {
  getAdminCompany,
  updateAdminCompanyModeration,
  updateAdminCompanyVerification,
} from '../services/employerService'

function AdminCompanyDetailPage() {
  const { companyId } = useParams()
  const { account } = useAuth()
  const [company, setCompany] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [moderationForm, setModerationForm] = useState({ moderationStatus: 'ACTIVE', reason: '' })
  const [verificationForm, setVerificationForm] = useState({ verificationStatus: 'UNVERIFIED', reason: '' })
  const [moderationError, setModerationError] = useState('')
  const [moderationSuccess, setModerationSuccess] = useState('')
  const [verificationError, setVerificationError] = useState('')
  const [verificationSuccess, setVerificationSuccess] = useState('')
  const [isUpdatingModeration, setIsUpdatingModeration] = useState(false)
  const [isUpdatingVerification, setIsUpdatingVerification] = useState(false)

  const permissions = account?.permissions ?? []
  const canModerateCompany = permissions.includes('COMPANY_MODERATE')
  const canVerifyCompany = permissions.includes('COMPANY_VERIFY')

  useEffect(() => {
    async function loadCompany() {
      setIsLoading(true)
      setError('')
      try {
        const loadedCompany = await getAdminCompany(companyId)
        setCompany(loadedCompany)
        setModerationForm({
          moderationStatus: loadedCompany.moderationStatus || 'ACTIVE',
          reason: loadedCompany.moderationReason || '',
        })
        setVerificationForm({
          verificationStatus: loadedCompany.verificationStatus || 'UNVERIFIED',
          reason: loadedCompany.verificationReason || '',
        })
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load company detail.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadCompany()
  }, [companyId])

  async function handleModerationSubmit(event) {
    event.preventDefault()
    const nextReason = moderationForm.reason.trim()

    if (moderationForm.moderationStatus === 'SUSPENDED' && !nextReason) {
      setModerationError('A non-blank reason is required when suspending a company.')
      setModerationSuccess('')
      return
    }

    setIsUpdatingModeration(true)
    setModerationError('')
    setModerationSuccess('')

    try {
      const updatedCompany = await updateAdminCompanyModeration(companyId, {
        moderationStatus: moderationForm.moderationStatus,
        reason: nextReason || undefined,
      })
      setCompany(updatedCompany)
      setModerationForm({
        moderationStatus: updatedCompany.moderationStatus || 'ACTIVE',
        reason: updatedCompany.moderationReason || '',
      })
      setModerationSuccess('Company moderation updated successfully.')
    } catch (requestError) {
      setModerationError(getApiErrorMessage(requestError, 'Unable to update company moderation.'))
    } finally {
      setIsUpdatingModeration(false)
    }
  }

  async function handleVerificationSubmit(event) {
    event.preventDefault()
    const nextReason = verificationForm.reason.trim()

    if (verificationForm.verificationStatus === 'REJECTED' && !nextReason) {
      setVerificationError('A non-blank reason is required when rejecting verification.')
      setVerificationSuccess('')
      return
    }

    setIsUpdatingVerification(true)
    setVerificationError('')
    setVerificationSuccess('')

    try {
      const updatedCompany = await updateAdminCompanyVerification(companyId, {
        verificationStatus: verificationForm.verificationStatus,
        reason: nextReason || undefined,
      })
      setCompany(updatedCompany)
      setVerificationForm({
        verificationStatus: updatedCompany.verificationStatus || 'UNVERIFIED',
        reason: updatedCompany.verificationReason || '',
      })
      setVerificationSuccess('Company verification updated successfully.')
    } catch (requestError) {
      setVerificationError(getApiErrorMessage(requestError, 'Unable to update company verification.'))
    } finally {
      setIsUpdatingVerification(false)
    }
  }

  if (isLoading) {
    return <main className="workspace-page"><p className="page-state">Loading company...</p></main>
  }

  if (!company) {
    return (
      <main className="workspace-page">
        <Link to="/admin/companies">Back to companies</Link>
        <p className="error-message">{error || 'Company not found.'}</p>
      </main>
    )
  }

  return (
    <main className="workspace-page">
      <Link to="/admin/companies">Back to companies</Link>
      <p className="eyebrow">Company detail</p>
      <h1>{company.name}</h1>
      {error && <p className="error-message">{error}</p>}
      <div className="detail-grid">
        <p><strong>id</strong>{company.id}</p>
        <p><strong>name</strong>{company.name}</p>
        <p><strong>website</strong>{company.website || 'Not set'}</p>
        <p><strong>industry</strong>{company.industry || 'Not set'}</p>
        <p><strong>location</strong>{company.location || 'Not set'}</p>
        <p><strong>moderationStatus</strong>{company.moderationStatus || 'ACTIVE'}</p>
        <p><strong>moderationReason</strong>{company.moderationReason || 'Not set'}</p>
        <p><strong>moderatedAt</strong>{company.moderatedAt ? new Date(company.moderatedAt).toLocaleString() : 'Not set'}</p>
        <p><strong>verificationStatus</strong>{company.verificationStatus || 'UNVERIFIED'}</p>
        <p><strong>verificationReason</strong>{company.verificationReason || 'Not set'}</p>
        <p><strong>verifiedAt</strong>{company.verifiedAt ? new Date(company.verifiedAt).toLocaleString() : 'Not set'}</p>
        <p><strong>createdAt</strong>{new Date(company.createdAt).toLocaleString()}</p>
        <p><strong>updatedAt</strong>{new Date(company.updatedAt).toLocaleString()}</p>
        <p className="full-width"><strong>description</strong>{company.description || 'No description.'}</p>
      </div>

      <section className="content-section">
        <h2>Moderation</h2>
        {!canModerateCompany ? (
          <p className="content-note">Read-only access. COMPANY_MODERATE is required to change moderation.</p>
        ) : (
          <form onSubmit={handleModerationSubmit}>
            {moderationError && <p className="error-message">{moderationError}</p>}
            {moderationSuccess && <p className="success-message">{moderationSuccess}</p>}
            <div className="form-grid">
              <label>
                Moderation status
                <select
                  value={moderationForm.moderationStatus}
                  onChange={(event) =>
                    setModerationForm({ ...moderationForm, moderationStatus: event.target.value })
                  }
                >
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="SUSPENDED">SUSPENDED</option>
                </select>
              </label>
              <label className="full-width">
                Reason
                <textarea
                  rows="4"
                  maxLength={1000}
                  value={moderationForm.reason}
                  onChange={(event) =>
                    setModerationForm({ ...moderationForm, reason: event.target.value })
                  }
                  placeholder={moderationForm.moderationStatus === 'SUSPENDED' ? 'Required for SUSPENDED' : 'Optional for ACTIVE'}
                />
              </label>
              <button type="submit" disabled={isUpdatingModeration}>
                {isUpdatingModeration ? 'Updating moderation...' : 'Update moderation'}
              </button>
            </div>
          </form>
        )}
      </section>

      <section className="content-section">
        <h2>Verification</h2>
        {!canVerifyCompany ? (
          <p className="content-note">Read-only access. COMPANY_VERIFY is required to change verification.</p>
        ) : (
          <form onSubmit={handleVerificationSubmit}>
            {verificationError && <p className="error-message">{verificationError}</p>}
            {verificationSuccess && <p className="success-message">{verificationSuccess}</p>}
            <div className="form-grid">
              <label>
                Verification status
                <select
                  value={verificationForm.verificationStatus}
                  onChange={(event) =>
                    setVerificationForm({ ...verificationForm, verificationStatus: event.target.value })
                  }
                >
                  <option value="UNVERIFIED">UNVERIFIED</option>
                  <option value="VERIFIED">VERIFIED</option>
                  <option value="REJECTED">REJECTED</option>
                </select>
              </label>
              <label className="full-width">
                Reason
                <textarea
                  rows="4"
                  maxLength={1000}
                  value={verificationForm.reason}
                  onChange={(event) =>
                    setVerificationForm({ ...verificationForm, reason: event.target.value })
                  }
                  placeholder={
                    verificationForm.verificationStatus === 'REJECTED'
                      ? 'Required for REJECTED'
                      : verificationForm.verificationStatus === 'VERIFIED'
                        ? 'Optional for VERIFIED'
                        : 'Optional for UNVERIFIED'
                  }
                />
              </label>
              <button type="submit" disabled={isUpdatingVerification}>
                {isUpdatingVerification ? 'Updating verification...' : 'Update verification'}
              </button>
            </div>
          </form>
        )}
      </section>
    </main>
  )
}

export default AdminCompanyDetailPage
