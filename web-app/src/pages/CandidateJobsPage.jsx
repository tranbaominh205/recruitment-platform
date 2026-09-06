import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../services/apiError'
import { searchJobs } from '../services/jobService'

function CandidateJobsPage() {
  const [jobs, setJobs] = useState([])
  const [filters, setFilters] = useState({ keyword: '', location: '', employmentType: '', workplaceType: '' })
  const [page, setPage] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadJobs(nextPage = page) {
    setIsLoading(true)
    setError('')
    try {
      const result = await searchJobs({ ...filters, page: nextPage, size: 20 })
      setJobs(result.content || [])
      setPage(result.page)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Unable to load published jobs.'))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { loadJobs(0) }, [])

  function updateFilter(event) {
    setFilters({ ...filters, [event.target.name]: event.target.value })
  }

  function handleSearch(event) {
    event.preventDefault()
    loadJobs(0)
  }

  return (
    <main className="workspace-page">
      <p className="eyebrow">Opportunities</p>
      <h1>Find a job</h1>
      {error && <p className="error-message">{error}</p>}
      <form className="search-grid" onSubmit={handleSearch}>
        <label>Keyword<input name="keyword" value={filters.keyword} onChange={updateFilter} placeholder="Job title" /></label>
        <label>Location<input name="location" value={filters.location} onChange={updateFilter} /></label>
        <label>Employment type<input name="employmentType" value={filters.employmentType} onChange={updateFilter} /></label>
        <label>Workplace type<input name="workplaceType" value={filters.workplaceType} onChange={updateFilter} /></label>
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
      {!isLoading && (page > 0 || jobs.length === 20) && <div className="pager"><button type="button" className="button-secondary" disabled={page === 0} onClick={() => loadJobs(page - 1)}>Previous</button><span>Page {page + 1}</span><button type="button" className="button-secondary" disabled={jobs.length < 20} onClick={() => loadJobs(page + 1)}>Next</button></div>}
    </main>
  )
}

export default CandidateJobsPage