import { useEffect, useState } from 'react'
import { ACCESS_TOKEN_KEY } from './apiClient'
import { getCurrentAccount, loginAccount } from '../services/identityService'
import { AuthContext } from './authContextValue'

export function AuthProvider({ children }) {
  const [account, setAccount] = useState(null)
  const [isRestoring, setIsRestoring] = useState(true)

  useEffect(() => {
    let active = true

    async function restoreSession() {
      const token = localStorage.getItem(ACCESS_TOKEN_KEY)

      if (!token) {
        setIsRestoring(false)
        return
      }

      try {
        const currentAccount = await getCurrentAccount()

        if (active) {
          setAccount(currentAccount)
        }
      } catch {
        localStorage.removeItem(ACCESS_TOKEN_KEY)
        if (active) {
          setAccount(null)
        }
      } finally {
        if (active) {
          setIsRestoring(false)
        }
      }
    }

    restoreSession()

    return () => {
      active = false
    }
  }, [])

  async function login(credentials) {
    const loginResult = await loginAccount(credentials)
    const token = loginResult?.accessToken

    if (!token) {
      throw new Error('Login response did not include an access token.')
    }

    localStorage.setItem(ACCESS_TOKEN_KEY, token)

    try {
      const currentAccount = await getCurrentAccount()
      setAccount(currentAccount)
      return currentAccount
    } catch (error) {
      localStorage.removeItem(ACCESS_TOKEN_KEY)
      setAccount(null)
      throw error
    }
  }

  function logout() {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    setAccount(null)
  }

  const value = {
    account,
    authenticated: account !== null,
    isRestoring,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}