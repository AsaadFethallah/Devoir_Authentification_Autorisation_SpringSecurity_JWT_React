import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage } from '../services/api'
import { getFullName, getRole } from '../services/authService'
import { incidentService } from '../services/incidentService'

const statusLabels = { OPEN: 'Ouvert', ASSIGNED: 'Affecté', IN_PROGRESS: 'En cours', RESOLVED: 'Résolu', CLOSED: 'Clôturé' }

function StatCard({ label, value, accent = '' }) {
  return <div className={`stat-card ${accent}`}><span>{label}</span><strong>{value ?? 0}</strong></div>
}

function IncidentPreview({ incident }) {
  return (
    <Link className="incident-row" to={`/incidents/${incident.id}`}>
      <span className="incident-dot" />
      <span className="incident-row-main"><strong>{incident.title}</strong><small>{incident.category} · {incident.priority}</small></span>
      <span className={`status-badge status-${incident.status.toLowerCase()}`}>{statusLabels[incident.status] || incident.status}</span>
    </Link>
  )
}

export default function Dashboard() {
  const role = getRole()
  const [incidents, setIncidents] = useState([])
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    const request = role === 'ADMIN' ? incidentService.statistics() : role === 'TECHNICIAN' ? incidentService.listAssigned() : incidentService.listMine()
    request.then((data) => {
      if (!active) return
      if (role === 'ADMIN') setStats(data)
      else setIncidents(data)
    }).catch((err) => active && setError(getErrorMessage(err)))
    return () => { active = false }
  }, [role])

  const computed = useMemo(() => ({
    total: incidents.length,
    open: incidents.filter((item) => item.status === 'OPEN').length,
    assigned: incidents.filter((item) => item.status === 'ASSIGNED').length,
    progress: incidents.filter((item) => item.status === 'IN_PROGRESS').length,
    resolved: incidents.filter((item) => item.status === 'RESOLVED' || item.status === 'CLOSED').length,
    critical: incidents.filter((item) => item.priority === 'CRITICAL').length
  }), [incidents])

  return (
    <main className="page-container">
      <div className="page-heading dashboard-heading"><div><p className="eyebrow">VUE D’ENSEMBLE</p><h1>Bonjour, {getFullName()}</h1><p className="muted">Voici l’état actuel de votre activité.</p></div>{role === 'EMPLOYEE' && <Link className="button button-primary" to="/incidents/new">+ Déclarer un incident</Link>}</div>
      {error && <div className="alert alert-error">{error}</div>}
      {role === 'ADMIN' && stats && <div className="stats-grid"><StatCard label="Utilisateurs" value={stats.totalUsers} /><StatCard label="Employés" value={stats.employees} /><StatCard label="Techniciens" value={stats.technicians} /><StatCard label="Incidents" value={stats.totalIncidents} accent="accent-blue" /><StatCard label="Ouverts" value={stats.open} accent="accent-orange" /><StatCard label="En cours" value={stats.inProgress} accent="accent-purple" /><StatCard label="Clôturés" value={stats.closed} accent="accent-green" /></div>}
      {role === 'EMPLOYEE' && <div className="stats-grid"><StatCard label="Total de mes incidents" value={computed.total} accent="accent-blue" /><StatCard label="Ouverts" value={computed.open} accent="accent-orange" /><StatCard label="En cours" value={computed.progress} accent="accent-purple" /><StatCard label="Résolus / clôturés" value={computed.resolved} accent="accent-green" /></div>}
      {role === 'TECHNICIAN' && <div className="stats-grid"><StatCard label="Incidents affectés" value={computed.total} accent="accent-blue" /><StatCard label="Critiques" value={computed.critical} accent="accent-red" /><StatCard label="En cours" value={computed.progress} accent="accent-purple" /><StatCard label="Résolus" value={computed.resolved} accent="accent-green" /></div>}
      {role !== 'ADMIN' && <section className="content-card"><div className="section-heading"><div><p className="eyebrow">ACTIVITÉ RÉCENTE</p><h2>{role === 'TECHNICIAN' ? 'Incidents à traiter' : 'Mes derniers incidents'}</h2></div><Link to={role === 'TECHNICIAN' ? '/incidents/assigned' : '/incidents/my'} className="text-link">Voir tout →</Link></div>{incidents.slice(0, 5).map((incident) => <IncidentPreview incident={incident} key={incident.id} />)}{!incidents.length && <p className="empty-state">Aucun incident pour le moment.</p>}</section>}
      {role === 'ADMIN' && <section className="content-card admin-shortcut"><div><p className="eyebrow">ADMINISTRATION</p><h2>Gérez la réponse aux incidents</h2><p className="muted">Consultez la file globale et affectez les incidents aux techniciens actifs.</p></div><Link className="button button-secondary" to="/admin/incidents">Ouvrir la file globale</Link></section>}
    </main>
  )
}
