import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { searchJobs } from '../services/jobService'
import { createJobSseConnection } from '../services/jobSseService'

const emptyFilters = { keyword: '', location: '', employmentType: '', workplaceType: '' }
const pageSize = 20

function CandidateJobsPage() {
  const [jobs, setJobs] = useState([])
  const [draftFilters, setDraftFilters] = useState(emptyFilters)
  const [appliedFilters, setAppliedFilters] = useState(emptyFilters)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const pageRef = useRef(0)
  const appliedFiltersRef = useRef(emptyFilters)

  useEffect(() => {
    pageRef.current = page
  }, [page])

  useEffect(() => {
    appliedFiltersRef.current = appliedFilters
  }, [appliedFilters])

  async function loadJobs(filters, requestedPage = 0) {
    setIsLoading(true)
    setError('')

    try {
      const firstResult = await searchJobs({ ...filters, page: requestedPage, size: pageSize })
      let normalized = firstResult || { content: [], page: 0, totalPages: 0, totalElements: 0 }

      const maxPage = Math.max(0, (normalized.totalPages || 0) - 1)
      if ((normalized.totalPages || 0) > 0 && requestedPage > maxPage) {
        const clampedResult = await searchJobs({ ...filters, page: maxPage, size: pageSize })
        normalized = clampedResult || { content: [], page: 0, totalPages: 0, totalElements: 0 }
      }

      setJobs(normalized.content || [])
      setPage((normalized.totalPages || 0) === 0 ? 0 : (normalized.page || 0))
      setTotalPages(normalized.totalPages || 0)
      setTotalElements(normalized.totalElements || 0)
    } catch (requestError) {
      setJobs([])
      setPage(0)
      setTotalPages(0)
      setTotalElements(0)
      setError(getApiErrorMessage(requestError, 'Unable to load published jobs.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadJobs(appliedFiltersRef.current, 0)
  }, [])

  useEffect(() => {
    const source = createJobSseConnection({
      onPublicJobListChanged: () => {
        loadJobs(appliedFiltersRef.current, pageRef.current)
      },
    })

    return () => {
      source?.close()
    }
  }, [])

  function updateFilter(event) {
    setDraftFilters({ ...draftFilters, [event.target.name]: event.target.value })
  }

  function handleSearch(event) {
    event.preventDefault()
    const nextAppliedFilters = { ...draftFilters }
    setAppliedFilters(nextAppliedFilters)
    loadJobs(nextAppliedFilters, 0)
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Opportunities</p>
      <h1>Find a job</h1>
      {error && <p className="error-message">{error}</p>}
      <form className="search-grid" onSubmit={handleSearch}>
        <label>Keyword<input name="keyword" value={draftFilters.keyword} onChange={updateFilter} placeholder="Job title" /></label>
        <label>Location<input name="location" value={draftFilters.location} onChange={updateFilter} /></label>
        <label>Employment type<input name="employmentType" value={draftFilters.employmentType} onChange={updateFilter} /></label>
        <label>Workplace type<input name="workplaceType" value={draftFilters.workplaceType} onChange={updateFilter} /></label>
        <button type="submit">Search jobs</button>
      </form>
      {isLoading ? <p className="page-state">Loading jobs...</p> : jobs.length === 0 ? <p className="empty-state">No published jobs match your search.</p> : (
        <div className="item-list">
          {jobs.map((job) => <article className="list-item" key={job.id}>
            <div><p className="eyebrow">{job.companyId}</p><h2>{job.title}</h2><p>{job.location || 'Location not specified'} · {job.employmentType || 'Employment type not specified'} · {job.workplaceType || 'Workplace type not specified'}</p></div>
            <Link className="button-link" to={`/candidate/jobs/${job.id}`}>View job</Link>
          </article>)}
        </div>
      )}
      {!isLoading && totalPages > 0 && <div className="pager"><button type="button" className="button-secondary" disabled={page === 0} onClick={() => loadJobs(appliedFilters, page - 1)}>Previous</button><span>Page {page + 1} of {totalPages} ({totalElements} total)</span><button type="button" className="button-secondary" disabled={page >= totalPages - 1} onClick={() => loadJobs(appliedFilters, page + 1)}>Next</button></div>}
    </main>
  )
}

export default CandidateJobsPage