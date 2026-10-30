import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage } from '../services/api'
import { incidentService } from '../services/incidentService'

const labels = { OPEN: 'Ouvert', ASSIGNED: 'Affecté', IN_PROGRESS: 'En cours', RESOLVED: 'Résolu', CLOSED: 'Clôturé' }

export default function AdminIncidents() {
  const [incidents, setIncidents] = useState([]); const [error, setError] = useState('')
  useEffect(() => { incidentService.listAll().then(setIncidents).catch((err) => setError(getErrorMessage(err))) }, [])
  return <main className="page-container"><div className="page-heading"><div><p className="eyebrow">ADMINISTRATION</p><h1>File globale</h1><p className="muted">Tous les incidents déclarés par les employés.</p></div></div>{error && <div className="alert alert-error">{error}</div>}<section className="content-card table-card"><div className="table-header"><strong>{incidents.length} incident(s)</strong><span className="muted">Créateur · Affectation</span></div>{incidents.map((item) => <Link className="table-row" to={`/incidents/${item.id}`} key={item.id}><span className="row-title"><strong>{item.title}</strong><small>{item.category} · {item.createdBy.fullName}</small></span><span className="assignment-text">{item.assignedTo?.fullName || 'Non affecté'}</span><span className={`status-badge status-${item.status.toLowerCase()}`}>{labels[item.status]}</span><span className="arrow">→</span></Link>)}{!incidents.length && <p className="empty-state">Aucun incident.</p>}</section></main>
}
