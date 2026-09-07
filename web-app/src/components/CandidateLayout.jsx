import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../configurations/useAuth'

function CandidateLayout() {
  const { account, logout } = useAuth()

  return (
    <div className="candidate-shell">
      <header className="shell-header">
        <div>
          <p className="eyebrow">Candidate workspace</p>
          <strong>Career desk</strong>
        </div>
        <div className="shell-account">
          <span>{account.email}</span>
          <button type="button" className="button-secondary" onClick={logout}>Log out</button>
        </div>
      </header>
      <nav className="shell-nav" aria-label="Candidate navigation">
        <NavLink to="/candidate" end>Overview</NavLink>
        <NavLink to="/candidate/profile">Profile</NavLink>
        <NavLink to="/candidate/resumes">Resumes</NavLink>
        <NavLink to="/candidate/jobs">Jobs</NavLink>
        <NavLink to="/candidate/applications">My applications</NavLink>
        <NavLink to="/candidate/notifications">Notifications</NavLink>
      </nav>
      <Outlet />
    </div>
  )
}

export default CandidateLayout