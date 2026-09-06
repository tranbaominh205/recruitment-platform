import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { registerAccount } from '../services/identityService'

function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', role: 'CANDIDATE' })
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  function updateField(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      await registerAccount(form)
      navigate('/login', {
        replace: true,
        state: { registeredMessage: 'Registration successful. Sign in to continue.' },
      })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to create the account.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <p className="eyebrow">Recruitment Platform</p>
        <h1>Create account</h1>
        {error && <p className="error-message">{error}</p>}
        <form onSubmit={handleSubmit}>
          <label>
            Email
            <input name="email" type="email" value={form.email} onChange={updateField} required />
          </label>
          <label>
            Password
            <input name="password" type="password" minLength="8" maxLength="72" value={form.password} onChange={updateField} required />
          </label>
          <label>
            Account type
            <select name="role" value={form.role} onChange={updateField}>
              <option value="CANDIDATE">Candidate</option>
              <option value="RECRUITER">Recruiter</option>
            </select>
          </label>
          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Creating account...' : 'Create account'}
          </button>
        </form>
        <p className="auth-link">Already registered? <Link to="/login">Sign in</Link></p>
      </section>
    </main>
  )
}

export default RegisterPage