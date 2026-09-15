import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../configurations/AuthContext'
import AdminAccountDetailPage from '../pages/AdminAccountDetailPage'
import AdminAccountsPage from '../pages/AdminAccountsPage'
import AdminCompaniesPage from '../pages/AdminCompaniesPage'
import AdminCompanyDetailPage from '../pages/AdminCompanyDetailPage'
import AdminLayout from '../components/AdminLayout'
import CandidateApplicationDetailPage from '../pages/CandidateApplicationDetailPage'
import CandidateApplicationsPage from '../pages/CandidateApplicationsPage'
import CandidateHomePage from '../pages/CandidateHomePage'
import CandidateJobDetailPage from '../pages/CandidateJobDetailPage'
import CandidateJobsPage from '../pages/CandidateJobsPage'
import CandidateProfilePage from '../pages/CandidateProfilePage'
import CandidateResumesPage from '../pages/CandidateResumesPage'
import AdminDashboardPage from '../pages/AdminDashboardPage'
import AdminJobDetailPage from '../pages/AdminJobDetailPage'
import AdminJobsPage from '../pages/AdminJobsPage'
import NotificationsPage from '../pages/NotificationsPage'
import CandidateLayout from '../components/CandidateLayout'
import RecruiterLayout from '../components/RecruiterLayout'
import HomePage from '../pages/HomePage'
import LoginPage from '../pages/LoginPage'
import RegisterPage from '../pages/RegisterPage'
import RecruiterHomePage from '../pages/RecruiterHomePage'
import RecruiterCompanyPage from '../pages/RecruiterCompanyPage'
import RecruiterJobsPage from '../pages/RecruiterJobsPage'
import RecruiterJobFormPage from '../pages/RecruiterJobFormPage'
import RecruiterJobDetailPage from '../pages/RecruiterJobDetailPage'
import RecruiterApplicationsPage from '../pages/RecruiterApplicationsPage'
import RecruiterApplicationDetailPage from '../pages/RecruiterApplicationDetailPage'
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
              <Route path="notifications" element={<NotificationsPage />} />
            </Route>
          </Route>
          <Route element={<ProtectedRoute allowedRole="RECRUITER" />}>
            <Route path="/recruiter" element={<RecruiterLayout />}>
              <Route index element={<RecruiterHomePage />} />
              <Route path="company" element={<RecruiterCompanyPage />} />
              <Route path="jobs" element={<RecruiterJobsPage />} />
              <Route path="jobs/new" element={<RecruiterJobFormPage />} />
              <Route path="jobs/:jobId/edit" element={<RecruiterJobFormPage />} />
              <Route path="jobs/:jobId" element={<RecruiterJobDetailPage />} />
              <Route path="jobs/:jobId/applications" element={<RecruiterApplicationsPage />} />
              <Route path="applications/:applicationId" element={<RecruiterApplicationDetailPage />} />
              <Route path="notifications" element={<NotificationsPage />} />
            </Route>
          </Route>
          <Route element={<ProtectedRoute allowedRole="ADMIN" />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<AdminDashboardPage />} />
              <Route path="accounts" element={<AdminAccountsPage />} />
              <Route path="accounts/:accountId" element={<AdminAccountDetailPage />} />
              <Route path="companies" element={<AdminCompaniesPage />} />
              <Route path="companies/:companyId" element={<AdminCompanyDetailPage />} />
              <Route path="jobs" element={<AdminJobsPage />} />
              <Route path="jobs/:jobId" element={<AdminJobDetailPage />} />
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default AppRoutes