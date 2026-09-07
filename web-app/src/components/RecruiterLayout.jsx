import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../configurations/useAuth'

function RecruiterLayout() {
  const { account, logout } = useAuth()
  return <div className="recruiter-shell"><header className="shell-header"><div><p className="eyebrow">Recruiter workspace</p><strong>Hiring desk</strong></div><div className="shell-account"><span>{account.email}</span><button type="button" className="button-secondary" onClick={logout}>Log out</button></div></header><nav className="shell-nav" aria-label="Recruiter navigation"><NavLink to="/recruiter" end>Overview</NavLink><NavLink to="/recruiter/company">Company</NavLink><NavLink to="/recruiter/jobs">Jobs</NavLink><NavLink to="/recruiter/notifications">Notifications</NavLink></nav><Outlet /></div>
}

export default RecruiterLayout