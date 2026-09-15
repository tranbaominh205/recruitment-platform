import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminResume } from '../services/resumeService'

function AdminResumeDetailPage() {
  const { resumeId } = useParams()
  const [resume, setResume] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadResume() {
      setIsLoading(true)
      setError('')
      try {
        setResume(await getAdminResume(resumeId))
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load resume detail.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadResume()
  }, [resumeId])

  if (isLoading) {
    return <main className="workspace-page"><p className="page-state">Loading resume...</p></main>
  }

  if (!resume) {
    return (
      <main className="workspace-page">
        <Link to="/admin/resumes">Back to resumes</Link>
        <p className="error-message">{error || 'Resume not found.'}</p>
      </main>
    )
  }

  return (
    <main className="workspace-page">
      <Link to="/admin/resumes">Back to resumes</Link>
      <p className="eyebrow">Resume detail</p>
      <h1>{resume.displayName}</h1>
      {error && <p className="error-message">{error}</p>}
      <div className="detail-grid">
        <p><strong>id</strong>{resume.id}</p>
        <p><strong>ownerAccountId</strong>{resume.ownerAccountId}</p>
        <p><strong>displayName</strong>{resume.displayName}</p>
        <p><strong>originalFileName</strong>{resume.originalFileName}</p>
        <p><strong>contentType</strong>{resume.contentType}</p>
        <p><strong>size</strong>{resume.size}</p>
        <p><strong>status</strong>{resume.status}</p>
        <p><strong>createdAt</strong>{new Date(resume.createdAt).toLocaleString()}</p>
      </div>
    </main>
  )
}

export default AdminResumeDetailPage
