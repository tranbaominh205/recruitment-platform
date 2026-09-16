import { EventSource } from 'eventsource'
import { ACCESS_TOKEN_KEY } from '../configurations/apiClient'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const SSE_URL = `${API_BASE_URL.replace(/\/$/, '')}/job/events`

const EVENT_CONNECTED = 'CONNECTED'
const EVENT_MY_JOB_LIST_CHANGED = 'MY_JOB_LIST_CHANGED'
const EVENT_PUBLIC_JOB_LIST_CHANGED = 'PUBLIC_JOB_LIST_CHANGED'
const JOB_LIST_CHANGES = new Set(['CREATED', 'UPDATED', 'PUBLISHED', 'CLOSED', 'MODERATION_CHANGED'])

function parseJsonObject(data) {
  try {
    const parsed = JSON.parse(data)
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch {
    return null
  }
}

function parseConnectedPayload(event) {
  const payload = parseJsonObject(event.data)
  if (!payload) {
    return null
  }
  return payload.type === EVENT_CONNECTED ? payload : null
}

function parseJobListChangedPayload(event, expectedType) {
  const payload = parseJsonObject(event.data)

  if (!payload || payload.type !== expectedType) {
    return null
  }

  if (typeof payload.jobId !== 'string' || !JOB_LIST_CHANGES.has(payload.change)) {
    return null
  }

  return payload
}

export function createJobSseConnection({ onMyJobListChanged, onPublicJobListChanged } = {}) {
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
        throw new Error('Missing access token for job SSE connection.')
      }

      const headers = new Headers(init?.headers ?? {})
      headers.set('Authorization', `Bearer ${currentToken}`)

      const response = await fetch(input, {
        ...init,
        headers,
      })

      if (response.status === 401 || response.status === 403) {
        source?.close()
        throw new Error(`Job SSE authorization failed with status ${response.status}.`)
      }

      return response
    },
  })

  source.addEventListener(EVENT_CONNECTED, (event) => {
    parseConnectedPayload(event)
  })

  source.addEventListener(EVENT_MY_JOB_LIST_CHANGED, (event) => {
    const payload = parseJobListChangedPayload(event, EVENT_MY_JOB_LIST_CHANGED)
    if (!payload) {
      return
    }

    try {
      onMyJobListChanged?.(payload)
    } catch {
      // no-op: subscriber failures must not break event handling.
    }
  })

  source.addEventListener(EVENT_PUBLIC_JOB_LIST_CHANGED, (event) => {
    const payload = parseJobListChangedPayload(event, EVENT_PUBLIC_JOB_LIST_CHANGED)
    if (!payload) {
      return
    }

    try {
      onPublicJobListChanged?.(payload)
    } catch {
      // no-op: subscriber failures must not break event handling.
    }
  })

  return source
}
