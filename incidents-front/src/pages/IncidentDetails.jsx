import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getErrorMessage } from '../services/api'
import { getRole } from '../services/authService'
import { incidentService } from '../services/incidentService'

const labels = { OPEN: 'Ouvert', ASSIGNED: 'Affecté', IN_PROGRESS: 'En cours', RESOLVED: 'Résolu', CLOSED: 'Clôturé' }
const statuses = ['IN_PROGRESS', 'RESOLVED', 'CLOSED']

function formatDate(value) { return value ? new Date(value).toLocaleString('fr-FR') : '—' }

export default function IncidentDetails() {
  const { id } = useParams()
  const role = getRole()
  const [incident, setIncident] = useState(null)
  const [history, setHistory] = useState([])
  const [comments, setComments] = useState([])
  const [users, setUsers] = useState([])
  const [technicianId, setTechnicianId] = useState('')
  const [comment, setComment] = useState('')
  const [selectedStatus, setSelectedStatus] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function load() {
    try {
      const [detail, incidentHistory, incidentComments] = await Promise.all([incidentService.get(id), incidentService.history(id), incidentService.comments(id)])
      setIncident(detail); setHistory(incidentHistory); setComments(incidentComments); setSelectedStatus(detail.status)
      if (detail.assignedTo) setTechnicianId(String(detail.assignedTo.id))
    } catch (err) { setError(getErrorMessage(err)) }
  }

  useEffect(() => {
    load()
    if (role === 'ADMIN') incidentService.adminUsers().then(setUsers).catch((err) => setError(getErrorMessage(err)))
  }, [id, role])

  async function changeStatus(event) {
    event.preventDefault(); setError(''); setMessage('')
    try { await incidentService.updateStatus(id, selectedStatus); await load(); setMessage('Statut mis à jour et historique enregistré.') } catch (err) { setError(getErrorMessage(err)) }
  }

  async function assign(event) {
    event.preventDefault(); setError(''); setMessage('')
    if (!technicianId) return setError('Sélectionnez un technicien actif')
    try { await incidentService.assign(id, technicianId); await load(); setMessage('Incident affecté avec succès.') } catch (err) { setError(getErrorMessage(err)) }
  }

  async function addComment(event) {
    event.preventDefault(); setError(''); setMessage('')
    try { await incidentService.addComment(id, comment); setComment(''); await load(); setMessage('Note ajoutée.') } catch (err) { setError(getErrorMessage(err)) }
  }

  if (error && !incident) return <main className="page-container"><div className="alert alert-error">{error}</div><Link className="text-link" to="/dashboard">← Retour au tableau de bord</Link></main>
  if (!incident) return <main className="page-container"><div className="loading-state">Chargement de l’incident…</div></main>

  const activeTechnicians = users.filter((user) => user.role === 'TECHNICIAN' && user.enabled)
  const canWriteNote = role === 'TECHNICIAN' || role === 'ADMIN'
  // Le backend vérifie l'affectation réelle ; l'interface peut donc proposer l'action
  // à un technicien et afficher proprement le refus éventuel de l'API.
  const canChangeStatus = role === 'TECHNICIAN' || role === 'ADMIN'

  return <main className="page-container detail-page"><div className="breadcrumb"><Link to="/dashboard">Tableau de bord</Link><span>/</span><span>Incident #{incident.id}</span></div>{error && <div className="alert alert-error">{error}</div>}{message && <div className="alert alert-success">{message}</div>}<div className="detail-layout"><section><div className="content-card incident-hero"><div className="detail-title-line"><div><p className="eyebrow">INCIDENT #{incident.id}</p><h1>{incident.title}</h1></div><span className={`status-badge status-${incident.status.toLowerCase()}`}>{labels[incident.status]}</span></div><div className="detail-meta"><span><small>Catégorie</small><strong>{incident.category}</strong></span><span><small>Priorité</small><strong className={`priority-text priority-${incident.priority.toLowerCase()}`}>{incident.priority}</strong></span><span><small>Créé le</small><strong>{formatDate(incident.createdAt)}</strong></span><span><small>Mis à jour</small><strong>{formatDate(incident.updatedAt)}</strong></span></div><div className="description-block"><p className="eyebrow">DESCRIPTION</p><p>{incident.description}</p></div><div className="people-line"><span>Déclaré par <strong>{incident.createdBy.fullName}</strong></span><span>Affecté à <strong>{incident.assignedTo?.fullName || 'Non affecté'}</strong></span></div></div>{canChangeStatus && incident.status !== 'CLOSED' && <section className="content-card action-card"><div className="section-heading"><div><p className="eyebrow">TRAITEMENT</p><h2>Modifier le statut</h2></div></div><form className="inline-form" onSubmit={changeStatus}><select value={selectedStatus} onChange={(event) => setSelectedStatus(event.target.value)}>{statuses.map((status) => <option key={status} value={status}>{labels[status]}</option>)}</select><button className="button button-primary">Enregistrer</button></form></section>}{canWriteNote && <section className="content-card action-card"><div className="section-heading"><div><p className="eyebrow">SUIVI</p><h2>Ajouter une note</h2></div></div><form onSubmit={addComment} className="form-stack"><textarea rows="4" value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Écrivez une note de traitement…" required /><button className="button button-secondary">Ajouter la note</button></form></section>}<section className="content-card"><div className="section-heading"><div><p className="eyebrow">NOTES</p><h2>Notes de traitement</h2></div></div>{comments.map((item) => <div className="feed-item" key={item.id}><div className="feed-icon">✦</div><div><p>{item.content}</p><small>{item.author.fullName} · {formatDate(item.createdAt)}</small></div></div>)}{!comments.length && <p className="empty-state">Aucune note pour le moment.</p>}</section></section><aside><section className="content-card"><div className="section-heading"><div><p className="eyebrow">JOURNAL</p><h2>Historique</h2></div></div><div className="timeline">{history.map((item) => <div className="timeline-item" key={item.id}><span className="timeline-dot" /><div><strong>{item.action}</strong><small>{item.author.fullName} · {formatDate(item.date)}</small></div></div>)}{!history.length && <p className="empty-state">Aucun événement.</p>}</div></section>{role === 'ADMIN' && <section className="content-card"><div className="section-heading"><div><p className="eyebrow">ADMINISTRATION</p><h2>Affectation</h2></div></div><form onSubmit={assign} className="form-stack"><label>Technicien actif<select value={technicianId} onChange={(event) => setTechnicianId(event.target.value)}><option value="">Sélectionner…</option>{activeTechnicians.map((user) => <option value={user.id} key={user.id}>{user.fullName}</option>)}</select></label><button className="button button-primary button-wide">Affecter l’incident</button></form></section>}</aside></div></main>
}
