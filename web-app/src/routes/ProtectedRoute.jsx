import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../configurations/useAuth'

function ProtectedRoute({ allowedRole }) {
  const { account, isRestoring } = useAuth()
  const location = useLocation()

  if (isRestoring) {
    return <main className="page-state">Restoring your session...</main>
  }

  if (!account) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (account.role !== allowedRole) {
    if (account.role === 'CANDIDATE') {
      return <Navigate to="/candidate" replace />
    }

    if (account.role === 'RECRUITER') {
      return <Navigate to="/recruiter" replace />
    }

    return <Navigate to="/" replace />
  }

  return <Outlet />
}

export default ProtectedRoute