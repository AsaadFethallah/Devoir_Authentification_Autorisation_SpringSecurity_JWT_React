import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage } from '../services/api'
import { incidentService } from '../services/incidentService'

const statusLabels = { OPEN: 'Ouvert', ASSIGNED: 'Affecté', IN_PROGRESS: 'En cours', RESOLVED: 'Résolu', CLOSED: 'Clôturé' }

export default function MyIncidents() {
  const [incidents, setIncidents] = useState([])
  const [error, setError] = useState('')
  useEffect(() => { incidentService.listMine().then(setIncidents).catch((err) => setError(getErrorMessage(err))) }, [])

  return <main className="page-container"><div className="page-heading"><div><p className="eyebrow">EMPLOYEE</p><h1>Mes incidents</h1><p className="muted">Suivez les incidents que vous avez déclarés.</p></div><Link className="button button-primary" to="/incidents/new">+ Nouvel incident</Link></div>{error && <div className="alert alert-error">{error}</div>}<section className="content-card table-card"><div className="table-header"><strong>{incidents.length} incident(s)</strong><span className="muted">Dernière mise à jour</span></div>{incidents.map((item) => <Link className="table-row" to={`/incidents/${item.id}`} key={item.id}><span className="row-title"><strong>{item.title}</strong><small>{item.category} · Priorité {item.priority}</small></span><span className={`status-badge status-${item.status.toLowerCase()}`}>{statusLabels[item.status]}</span><span className="date-text">{new Date(item.updatedAt).toLocaleDateString('fr-FR')}</span><span className="arrow">→</span></Link>)}{!incidents.length && <p className="empty-state">Aucun incident déclaré.</p>}</section></main>
}
