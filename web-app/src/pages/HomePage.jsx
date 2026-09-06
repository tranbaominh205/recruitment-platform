import { Link } from 'react-router-dom'
import { useAuth } from '../configurations/useAuth'

function HomePage() {
  return (
    <main className="workspace-page">
      <h1>Recruitment Platform</h1>
      <HomeContent />
    </main>
  )
}

function HomeContent() {
  const { account, logout } = useAuth()

  if (account?.role === 'CANDIDATE') {
    return <p><Link to="/candidate">Open your workspace</Link> or <button type="button" onClick={logout}>log out</button>.</p>
  }

  if (account?.role === 'RECRUITER') {
    return <p><Link to="/recruiter">Open your workspace</Link> or <button type="button" onClick={logout}>log out</button>.</p>
  }

  if (account) {
    return (
      <>
        <p>This account role is not supported in the web application yet.</p>
        <button type="button" onClick={logout}>Log out</button>
      </>
    )
  }

  return <p><Link to="/login">Sign in</Link> or <Link to="/register">create an account</Link>.</p>
}

export default HomePage