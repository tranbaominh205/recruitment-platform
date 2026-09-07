import { Link } from 'react-router-dom'

function RecruiterHomePage() {
  return <main className="workspace-page"><p className="eyebrow">Overview</p><h1>Build the next team.</h1><p className="lead">Set up your company, shape a job, and review candidates through the exact resume and matching evidence they submitted.</p><div className="overview-grid"><Link className="overview-link" to="/recruiter/company"><strong>Company profile</strong><span>Create or update your employer profile.</span></Link><Link className="overview-link" to="/recruiter/jobs"><strong>My jobs</strong><span>Create drafts, publish roles, and review applications.</span></Link></div></main>
}

export default RecruiterHomePage