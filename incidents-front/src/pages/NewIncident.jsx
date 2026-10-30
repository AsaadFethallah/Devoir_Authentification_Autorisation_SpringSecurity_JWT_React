import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../services/api'
import { incidentService } from '../services/incidentService'

const categories = ['NETWORK', 'MALWARE', 'PHISHING', 'ACCESS', 'SOFTWARE', 'HARDWARE', 'DATA_LEAK', 'OTHER']
const priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

export default function NewIncident() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: '', description: '', category: '', priority: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  function update(event) { setForm({ ...form, [event.target.name]: event.target.value }) }
  async function submit(event) {
    event.preventDefault(); setError(''); setLoading(true)
    try { const incident = await incidentService.create(form); navigate(`/incidents/${incident.id}`) } catch (err) { setError(getErrorMessage(err)) } finally { setLoading(false) }
  }
  return <main className="page-container narrow-container"><div className="page-heading"><div><p className="eyebrow">DÉCLARATION</p><h1>Nouvel incident</h1><p className="muted">Décrivez précisément le problème rencontré.</p></div></div>{error && <div className="alert alert-error">{error}</div>}<section className="content-card"><form onSubmit={submit} className="form-stack"><label>Titre<input name="title" value={form.title} onChange={update} placeholder="Ex. Tentative de phishing" required /></label><div className="form-grid"><label>Catégorie<select name="category" value={form.category} onChange={update} required><option value="">Sélectionner…</option>{categories.map((item) => <option key={item} value={item}>{item}</option>)}</select></label><label>Priorité<select name="priority" value={form.priority} onChange={update} required><option value="">Sélectionner…</option>{priorities.map((item) => <option key={item} value={item}>{item}</option>)}</select></label></div><label>Description<textarea name="description" rows="7" value={form.description} onChange={update} placeholder="Que s’est-il passé ? Depuis quand ? Quels éléments sont concernés ?" required /></label><div className="form-actions"><button type="button" className="button button-ghost" onClick={() => navigate(-1)}>Annuler</button><button className="button button-primary" disabled={loading}>{loading ? 'Déclaration…' : 'Déclarer l’incident'}</button></div></form></section></main>
}
