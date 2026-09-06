import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../configurations/AuthContext'
import CandidateApplicationDetailPage from '../pages/CandidateApplicationDetailPage'
import CandidateApplicationsPage from '../pages/CandidateApplicationsPage'
import CandidateHomePage from '../pages/CandidateHomePage'
import CandidateJobDetailPage from '../pages/CandidateJobDetailPage'
import CandidateJobsPage from '../pages/CandidateJobsPage'
import CandidateProfilePage from '../pages/CandidateProfilePage'
import CandidateResumesPage from '../pages/CandidateResumesPage'
import CandidateLayout from '../components/CandidateLayout'
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
            <Route path="/candidate" element={<CandidateLayout />}>
              <Route index element={<CandidateHomePage />} />
              <Route path="profile" element={<CandidateProfilePage />} />
              <Route path="resumes" element={<CandidateResumesPage />} />
              <Route path="jobs" element={<CandidateJobsPage />} />
              <Route path="jobs/:jobId" element={<CandidateJobDetailPage />} />
              <Route path="applications" element={<CandidateApplicationsPage />} />
              <Route path="applications/:applicationId" element={<CandidateApplicationDetailPage />} />
            </Route>
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