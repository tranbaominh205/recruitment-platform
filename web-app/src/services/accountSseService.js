import { EventSource } from 'eventsource'
import { ACCESS_TOKEN_KEY } from '../configurations/apiClient'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const SSE_URL = `${API_BASE_URL.replace(/\/$/, '')}/notification/events`
const notificationCreatedListeners = new Set()

export function subscribeToNotificationCreated(listener) {
  notificationCreatedListeners.add(listener)

  return () => {
    notificationCreatedListeners.delete(listener)
  }
}

function notifyNotificationCreated(event) {
  let payload

  try {
    payload = JSON.parse(event.data)
  } catch {
    return
  }

  for (const listener of Array.from(notificationCreatedListeners)) {
    try {
      listener(payload)
    } catch {
      // no-op: listener failures must not break event distribution.
    }
  }
}

export function createAccountSseConnection() {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)

  if (!token) {
    return null
  }

  let source = null

  source = new EventSource(SSE_URL, {
    fetch: async (input, init) => {
      const currentToken = localStorage.getItem(ACCESS_TOKEN_KEY)

      if (!currentToken) {
        source?.close()
        throw new Error('Missing access token for SSE connection.')
      }

      const headers = new Headers(init?.headers ?? {})
      headers.set('Authorization', `Bearer ${currentToken}`)

      const response = await fetch(input, {
        ...init,
        headers,
      })

      if (response.status === 401 || response.status === 403) {
        source?.close()
        throw new Error(`SSE authorization failed with status ${response.status}.`)
      }

      return response
    },
  })

  source.addEventListener('CONNECTED', () => {
    // Intentionally no-op: this is the transport proof only.
  })
  source.addEventListener('NOTIFICATION_CREATED', notifyNotificationCreated)

  return source
}
