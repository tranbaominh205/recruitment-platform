import { EventSource } from 'eventsource'
import { ACCESS_TOKEN_KEY } from '../configurations/apiClient'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const EVENT_CONNECTED = 'CONNECTED'
const EVENT_APPLICATION_LIST_CHANGED = 'APPLICATION_LIST_CHANGED'

function buildSseUrl(jobId) {
  return `${API_BASE_URL.replace(/\/$/, '')}/recruitment/application/job/${jobId}/events`
}

function parseApplicationListChangedPayload(event) {
  let payload

  try {
    payload = JSON.parse(event.data)
  } catch {
    return null
  }

  if (!payload || typeof payload !== 'object') {
    return null
  }

  if (payload.type !== EVENT_APPLICATION_LIST_CHANGED) {
    return null
  }

  if (typeof payload.jobId !== 'string' || typeof payload.applicationId !== 'string') {
    return null
  }

  if (
    payload.change !== 'SUBMITTED'
    && payload.change !== 'WITHDRAWN'
    && payload.change !== 'STATUS_CHANGED'
  ) {
    return null
  }

  return payload
}

export function createRecruitmentJobApplicationsSseConnection(jobId, onApplicationListChanged) {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY)

  if (!token || !jobId) {
    return null
  }

  let source = null

  source = new EventSource(buildSseUrl(jobId), {
    fetch: async (input, init) => {
      const currentToken = localStorage.getItem(ACCESS_TOKEN_KEY)

      if (!currentToken) {
        source?.close()
        throw new Error('Missing access token for job application SSE connection.')
      }

      const headers = new Headers(init?.headers ?? {})
      headers.set('Authorization', `Bearer ${currentToken}`)

      const response = await fetch(input, {
        ...init,
        headers,
      })

      if (response.status === 401 || response.status === 403) {
        source?.close()
        throw new Error(`Job application SSE authorization failed with status ${response.status}.`)
      }

      return response
    },
  })

  source.addEventListener(EVENT_CONNECTED, () => {
    // Intentionally no-op: this is the transport proof only.
  })

  source.addEventListener(EVENT_APPLICATION_LIST_CHANGED, (event) => {
    const payload = parseApplicationListChangedPayload(event)

    if (!payload) {
      return
    }

    try {
      onApplicationListChanged?.(payload)
    } catch {
      // no-op: subscriber failures must not break event handling.
    }
  })

  return source
}
