import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../configurations/useAuth'
import { getApiErrorMessage } from '../services/apiError'

function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
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
      const account = await login(form)

      if (account.role === 'CANDIDATE') {
        navigate('/candidate', { replace: true })
      } else if (account.role === 'RECRUITER') {
        navigate('/recruiter', { replace: true })
      } else {
        navigate('/', { replace: true })
      }
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to sign in.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <p className="eyebrow">Recruitment Platform</p>
        <h1>Sign in</h1>
        {location.state?.registeredMessage && (
          <p className="success-message">{location.state.registeredMessage}</p>
        )}
        {error && <p className="error-message">{error}</p>}
        <form onSubmit={handleSubmit}>
          <label>
            Email
            <input name="email" type="email" value={form.email} onChange={updateField} required />
          </label>
          <label>
            Password
            <input name="password" type="password" value={form.password} onChange={updateField} required />
          </label>
          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
        <p className="auth-link">New here? <Link to="/register">Create an account</Link></p>
      </section>
    </main>
  )
}

export default LoginPage