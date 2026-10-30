import { useEffect, useState } from 'react'
import { getErrorMessage } from '../services/api'
import { incidentService } from '../services/incidentService'

export default function AdminUsers() {
  const [users, setUsers] = useState([]); const [error, setError] = useState('')
  function load() { incidentService.adminUsers().then(setUsers).catch((err) => setError(getErrorMessage(err))) }
  useEffect(load, [])
  async function toggle(user) { setError(''); try { const updated = user.enabled ? await incidentService.disableUser(user.id) : await incidentService.enableUser(user.id); setUsers((items) => items.map((item) => item.id === updated.id ? updated : item)) } catch (err) { setError(getErrorMessage(err)) } }
  return <main className="page-container"><div className="page-heading"><div><p className="eyebrow">ADMINISTRATION</p><h1>Utilisateurs</h1><p className="muted">Activez ou désactivez les comptes de la plateforme.</p></div></div>{error && <div className="alert alert-error">{error}</div>}<section className="content-card table-card"><div className="table-header"><strong>{users.length} utilisateur(s)</strong><span className="muted">État du compte</span></div>{users.map((user) => <div className="table-row user-row" key={user.id}><span className="avatar">{user.fullName.slice(0, 1).toUpperCase()}</span><span className="row-title"><strong>{user.fullName}</strong><small>{user.email}</small></span><span className="role-pill dark">{user.role}</span><span className={`enabled-state ${user.enabled ? 'enabled' : 'disabled'}`}>{user.enabled ? 'Actif' : 'Désactivé'}</span><button className={`button button-small ${user.enabled ? 'button-danger' : 'button-secondary'}`} onClick={() => toggle(user)}>{user.enabled ? 'Désactiver' : 'Activer'}</button></div>)}{!users.length && <p className="empty-state">Aucun utilisateur.</p>}</section></main>
}
