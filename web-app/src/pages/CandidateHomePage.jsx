import { useAuth } from '../configurations/useAuth'

function CandidateHomePage() {
  const { account, logout } = useAuth()

  return (
    <main className="workspace-page">
      <p className="eyebrow">Candidate workspace</p>
      <h1>Welcome</h1>
      <p>Signed in as {account.email}</p>
      <button type="button" onClick={logout}>Log out</button>
    </main>
  )
}

export default CandidateHomePage