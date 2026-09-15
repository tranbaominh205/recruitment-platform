import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminCompany } from '../services/employerService'

function AdminCompanyDetailPage() {
  const { companyId } = useParams()
  const [company, setCompany] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadCompany() {
      setIsLoading(true)
      setError('')
      try {
        setCompany(await getAdminCompany(companyId))
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load company detail.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadCompany()
  }, [companyId])

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
        <p><strong>createdAt</strong>{new Date(company.createdAt).toLocaleString()}</p>
        <p><strong>updatedAt</strong>{new Date(company.updatedAt).toLocaleString()}</p>
        <p className="full-width"><strong>description</strong>{company.description || 'No description.'}</p>
      </div>
    </main>
  )
}

export default AdminCompanyDetailPage
