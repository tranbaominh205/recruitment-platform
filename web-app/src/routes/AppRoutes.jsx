import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../configurations/AuthContext'
import CandidateHomePage from '../pages/CandidateHomePage'
import HomePage from '../pages/HomePage'
import LoginPage from '../pages/LoginPage'
import RegisterPage from '../pages/RegisterPage'
import RecruiterHomePage from '../pages/RecruiterHomePage'
import ProtectedRoute from './ProtectedRoute'

function AppRoutes() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route element={<ProtectedRoute allowedRole="CANDIDATE" />}>
            <Route path="/candidate" element={<CandidateHomePage />} />
          </Route>
          <Route element={<ProtectedRoute allowedRole="RECRUITER" />}>
            <Route path="/recruiter" element={<RecruiterHomePage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default AppRoutes