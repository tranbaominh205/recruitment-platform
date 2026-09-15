import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../configurations/useAuth'

function AdminLayout() {
  const { account, logout } = useAuth()

  return (
    <div className="candidate-shell">
      <header className="shell-header">
        <div>
          <p className="eyebrow">Admin workspace</p>
          <strong>Operations desk</strong>
        </div>
        <div className="shell-account">
          <span>{account.email}</span>
          <button type="button" className="button-secondary" onClick={logout}>Log out</button>
        </div>
      </header>
      <nav className="shell-nav" aria-label="Admin navigation">
        <NavLink to="/admin" end>Overview</NavLink>
        <NavLink to="/admin/accounts">Accounts</NavLink>
        <NavLink to="/admin/companies">Companies</NavLink>
        <NavLink to="/admin/jobs">Jobs</NavLink>
      </nav>
      <Outlet />
    </div>
  )
}

export default AdminLayout
