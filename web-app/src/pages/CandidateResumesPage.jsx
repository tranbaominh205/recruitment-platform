import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../services/apiError'
import { downloadMyResume, getMyResumes, uploadResume } from '../services/resumeService'

function CandidateResumesPage() {
  const [resumes, setResumes] = useState([])
  const [selectedFile, setSelectedFile] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isUploading, setIsUploading] = useState(false)
  const [error, setError] = useState('')

  async function loadResumes() {
    try {
      setResumes(await getMyResumes())
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to load resumes.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { loadResumes() }, [])

  async function handleUpload(event) {
    event.preventDefault()
    if (!selectedFile) return
    setError('')
    setIsUploading(true)
    try {
      await uploadResume(selectedFile)
      setSelectedFile(null)
      event.target.reset()
      await loadResumes()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to upload this PDF resume.'))
    } finally {
      setIsUploading(false)
    }
  }

  async function handleDownload(resume) {
    try {
      const result = await downloadMyResume(resume.id)
      const url = URL.createObjectURL(result.blob)
      const link = document.createElement('a')
      link.href = url
      link.download = resume.originalFileName
      link.click()
      URL.revokeObjectURL(url)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to download this resume.'))
    }
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Resume library</p>
      <h1>Your resumes</h1>
      <p className="muted">Upload PDF files. Each upload remains a distinct resume that can be selected for an application.</p>
      {error && <p className="error-message">{error}</p>}
      <form className="upload-row" onSubmit={handleUpload}>
        <input type="file" accept="application/pdf,.pdf" onChange={(event) => setSelectedFile(event.target.files[0] || null)} required />
        <button type="submit" disabled={isUploading}>{isUploading ? 'Uploading...' : 'Upload PDF'}</button>
      </form>
      {isLoading ? <p className="page-state">Loading resumes...</p> : resumes.length === 0 ? <p className="empty-state">No resumes yet. Upload one before applying to a job.</p> : (
        <div className="item-list">
          {resumes.map((resume) => (
            <article className="list-item" key={resume.id}>
              <div><h2>{resume.displayName}</h2><p>{resume.originalFileName} · {Math.round(resume.size / 1024)} KB · {resume.status}</p></div>
              <button type="button" className="button-secondary" onClick={() => handleDownload(resume)}>Download</button>
            </article>
          ))}
        </div>
      )}
    </main>
  )
}

export default CandidateResumesPage