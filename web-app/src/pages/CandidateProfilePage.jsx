import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../services/apiError'
import {
  createMyProfile,
  getMyProfile,
  updateMyPreferences,
  updateMyProfile,
} from '../services/candidateService'

const blankProfile = {
  fullName: '', phone: '', school: '', major: '', graduationYear: '', location: '',
}

const blankPreferences = {
  desiredJobTitles: '', preferredLocations: '', employmentTypes: '', workplaceTypes: '',
}

function toList(value) {
  return value.split(',').map((item) => item.trim()).filter(Boolean)
}

function toText(value) {
  return Array.isArray(value) ? value.join(', ') : ''
}

function CandidateProfilePage() {
  const [profile, setProfile] = useState(blankProfile)
  const [preferences, setPreferences] = useState(blankPreferences)
  const [profileExists, setProfileExists] = useState(true)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadProfile() {
      try {
        const result = await getMyProfile()
        setProfile({ ...result, graduationYear: result.graduationYear || '' })
        setPreferences({
          desiredJobTitles: toText(result.desiredJobTitles),
          preferredLocations: toText(result.preferredLocations),
          employmentTypes: toText(result.employmentTypes),
          workplaceTypes: toText(result.workplaceTypes),
        })
      } catch (requestError) {
        if (requestError.response?.status === 404) {
          setProfileExists(false)
        } else {
          setError(getApiErrorMessage(requestError, 'Unable to load your profile.'))
        }
      } finally {
        setIsLoading(false)
      }
    }

    loadProfile()
  }, [])

  function updateProfileField(event) {
    setProfile({ ...profile, [event.target.name]: event.target.value })
  }

  function updatePreferenceField(event) {
    setPreferences({ ...preferences, [event.target.name]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    setIsSaving(true)

    try {
      const payload = { ...profile, graduationYear: profile.graduationYear ? Number(profile.graduationYear) : null }
      const savedProfile = profileExists ? await updateMyProfile(payload) : await createMyProfile(payload)
      await updateMyPreferences({
        desiredJobTitles: toList(preferences.desiredJobTitles),
        preferredLocations: toList(preferences.preferredLocations),
        employmentTypes: toList(preferences.employmentTypes),
        workplaceTypes: toList(preferences.workplaceTypes),
      })
      setProfileExists(true)
      setProfile({ ...savedProfile, graduationYear: savedProfile.graduationYear || '' })
      setMessage('Profile and preferences saved.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to save your profile.'))
    } finally {
      setIsSaving(false)
    }
  }

  if (isLoading) return <main className="workspace-page"><p className="page-state">Loading profile...</p></main>
  if (error && !profileExists) return <main className="workspace-page"><p className="error-message">{error}</p></main>

  return (
    <main className="workspace-page">
      <p className="eyebrow">Your information</p>
      <h1>{profileExists ? 'Profile' : 'Create your profile'}</h1>
      <p className="muted">Profile information and preferences are stored separately from your resumes.</p>
      {error && <p className="error-message">{error}</p>}
      {message && <p className="success-message">{message}</p>}
      <form className="form-grid" onSubmit={handleSubmit}>
        <label>Full name<input name="fullName" value={profile.fullName} onChange={updateProfileField} required /></label>
        <label>Phone<input name="phone" value={profile.phone || ''} onChange={updateProfileField} /></label>
        <label>School<input name="school" value={profile.school || ''} onChange={updateProfileField} /></label>
        <label>Major<input name="major" value={profile.major || ''} onChange={updateProfileField} /></label>
        <label>Graduation year<input name="graduationYear" type="number" min="1900" max="2100" value={profile.graduationYear} onChange={updateProfileField} /></label>
        <label>Location<input name="location" value={profile.location || ''} onChange={updateProfileField} /></label>
        <div className="section-heading"><h2>Preferences</h2><p>Use comma-separated values. These are for your own preferences, not recruiter scoring.</p></div>
        <label>Desired job titles<input name="desiredJobTitles" value={preferences.desiredJobTitles} onChange={updatePreferenceField} /></label>
        <label>Preferred locations<input name="preferredLocations" value={preferences.preferredLocations} onChange={updatePreferenceField} /></label>
        <label>Employment types<input name="employmentTypes" value={preferences.employmentTypes} onChange={updatePreferenceField} /></label>
        <label>Workplace types<input name="workplaceTypes" value={preferences.workplaceTypes} onChange={updatePreferenceField} /></label>
        <button type="submit" disabled={isSaving}>{isSaving ? 'Saving...' : 'Save profile'}</button>
      </form>
    </main>
  )
}

export default CandidateProfilePage