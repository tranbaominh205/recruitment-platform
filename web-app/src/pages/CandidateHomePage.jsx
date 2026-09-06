import { Link } from 'react-router-dom'

function CandidateHomePage() {
  return (
    <main className="workspace-page">
      <p className="eyebrow">Overview</p>
      <h1>Move your search forward.</h1>
      <p className="lead">Keep your candidate information current, choose the right resume for each application, and follow every next step from one place.</p>
      <div className="overview-grid">
        <Link className="overview-link" to="/candidate/profile"><strong>Profile and preferences</strong><span>Maintain your own candidate details.</span></Link>
        <Link className="overview-link" to="/candidate/resumes"><strong>Resume library</strong><span>Upload and review your PDF resumes.</span></Link>
        <Link className="overview-link" to="/candidate/jobs"><strong>Browse jobs</strong><span>Search published opportunities.</span></Link>
        <Link className="overview-link" to="/candidate/applications"><strong>My applications</strong><span>Track statuses and interview details.</span></Link>
      </div>
    </main>
  )
}

export default CandidateHomePage