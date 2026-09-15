import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { getAdminAccount, updateAdminAccountEnabled } from '../services/identityService'

function AdminAccountDetailPage() {
  const { accountId } = useParams()
  const [account, setAccount] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isMutating, setIsMutating] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadAccount() {
      setIsLoading(true)
      setError('')
      try {
        setAccount(await getAdminAccount(accountId))
      } catch (requestError) {
        setError(getApiErrorMessage(requestError, 'Unable to load account detail.'))
      } finally {
        setIsLoading(false)
      }
    }

    loadAccount()
  }, [accountId])

  async function handleToggleEnabled() {
    if (!account) {
      return
    }

    setIsMutating(true)
    setError('')
    try {
      const updated = await updateAdminAccountEnabled(account.id, !account.enabled)
      setAccount(updated)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to update account enabled state.'))
    } finally {
      setIsMutating(false)
    }
  }

  if (isLoading) {
    return <main className="workspace-page"><p className="page-state">Loading account...</p></main>
  }

  if (!account) {
    return (
      <main className="workspace-page">
        <Link to="/admin/accounts">Back to accounts</Link>
        <p className="error-message">{error || 'Account not found.'}</p>
      </main>
    )
  }

  return (
    <main className="workspace-page">
      <Link to="/admin/accounts">Back to accounts</Link>
      <p className="eyebrow">Account detail</p>
      <h1>{account.email}</h1>
      {error && <p className="error-message">{error}</p>}
      <div className="detail-grid">
        <p><strong>id</strong>{account.id}</p>
        <p><strong>email</strong>{account.email}</p>
        <p><strong>role</strong>{account.role}</p>
        <p><strong>enabled</strong>{String(account.enabled)}</p>
        <p><strong>createdAt</strong>{new Date(account.createdAt).toLocaleString()}</p>
      </div>
      <button type="button" onClick={handleToggleEnabled} disabled={isMutating}>
        {isMutating ? 'Saving...' : account.enabled ? 'Disable account' : 'Enable account'}
      </button>
    </main>
  )
}

export default AdminAccountDetailPage
