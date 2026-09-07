import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../services/apiError'
import { createCompany, getMyCompany, updateCompany } from '../services/employerService'

const emptyCompany = { name: '', description: '', website: '', industry: '', location: '' }

function RecruiterCompanyPage() {
  const [company, setCompany] = useState(null)
  const [form, setForm] = useState(emptyCompany)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    getMyCompany().then((result) => { setCompany(result); setForm(result) }).catch((requestError) => {
      if (requestError.response?.status !== 404) setError(getApiErrorMessage(requestError, 'Unable to load company.'))
    }).finally(() => setIsLoading(false))
  }, [])

  function updateField(event) { setForm({ ...form, [event.target.name]: event.target.value }) }

  async function handleSubmit(event) {
    event.preventDefault(); setError(''); setMessage(''); setIsSaving(true)
    try { const result = company ? await updateCompany(form) : await createCompany(form); setCompany(result); setForm(result); setMessage(company ? 'Company updated.' : 'Company created.') } catch (requestError) { setError(getApiErrorMessage(requestError, 'Unable to save company.')) } finally { setIsSaving(false) }
  }

  if (isLoading) return <main className="workspace-page"><p className="page-state">Loading company...</p></main>
  return <main className="workspace-page"><p className="eyebrow">Company</p><h1>{company ? 'Your company' : 'Set up your company'}</h1><p className="lead">A company is required before you can create an owned job.</p>{error && <p className="error-message">{error}</p>}{message && <p className="success-message">{message}</p>}<form className="form-grid" onSubmit={handleSubmit}>{['name', 'website', 'industry', 'location'].map((field) => <label key={field}>{field[0].toUpperCase() + field.slice(1)}<input name={field} value={form[field] || ''} onChange={updateField} required={field === 'name'} /></label>)}<label className="full-width">Description<textarea name="description" value={form.description || ''} onChange={updateField} rows="5" /></label><button type="submit" disabled={isSaving}>{isSaving ? 'Saving...' : company ? 'Save company' : 'Create company'}</button></form></main>
}

export default RecruiterCompanyPage