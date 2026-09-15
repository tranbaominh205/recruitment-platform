import { useEffect, useRef } from 'react'
import { useAuth } from '../configurations/useAuth'
import { createAccountSseConnection } from '../services/accountSseService'

function AccountSseConnection() {
  const { account } = useAuth()
  const connectionRef = useRef(null)

  useEffect(() => {
    const allowedRoles = new Set(['CANDIDATE', 'RECRUITER'])

    if (!account || !allowedRoles.has(account.role)) {
      if (connectionRef.current) {
        connectionRef.current.close()
        connectionRef.current = null
      }
      return undefined
    }

    const connection = createAccountSseConnection()

    if (!connection) {
      return undefined
    }

    connectionRef.current = connection

    return () => {
      connection.close()
      if (connectionRef.current === connection) {
        connectionRef.current = null
      }
    }
  }, [account])

  return null
}

export default AccountSseConnection
