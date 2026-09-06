import { useAuth } from '../configurations/useAuth'

function RecruiterHomePage() {
  const { account, logout } = useAuth()

  return (
    <main className="workspace-page">
      <p className="eyebrow">Recruiter workspace</p>
      <h1>Welcome</h1>
      <p>Signed in as {account.email}</p>
      <button type="button" onClick={logout}>Log out</button>
    </main>
  )
}

export default RecruiterHomePage