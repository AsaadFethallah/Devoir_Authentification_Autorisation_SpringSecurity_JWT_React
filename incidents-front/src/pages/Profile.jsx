import { useEffect, useState } from 'react'
import { getErrorMessage, api } from '../services/api'
import { getFullName, replaceToken } from '../services/authService'

const emptyPassword = { currentPassword: '', newPassword: '', confirmation: '' }

export default function Profile() {
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState({ fullName: getFullName(), email: '' })
  const [password, setPassword] = useState(emptyPassword)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    api.get('/api/profile').then(({ data }) => {
      setProfile(data)
      setForm({ fullName: data.fullName, email: data.email })
    }).catch((err) => setError(getErrorMessage(err)))
  }, [])

  async function updateProfile(event) {
    event.preventDefault()
    setError(''); setMessage('')
    try {
      const { data } = await api.put('/api/profile', form)
      setProfile(data.profile)
      setForm({ fullName: data.profile.fullName, email: data.profile.email })
      localStorage.setItem('fullName', data.profile.fullName)
      replaceToken(data.token)
      setMessage(data.token ? 'Profil mis à jour. Votre nouveau token a été enregistré.' : 'Profil mis à jour.')
    } catch (err) { setError(getErrorMessage(err)) }
  }

  async function changePassword(event) {
    event.preventDefault()
    setError(''); setMessage('')
    try {
      await api.put('/api/profile/password', password)
      setPassword(emptyPassword)
      setMessage('Mot de passe modifié avec succès.')
    } catch (err) { setError(getErrorMessage(err)) }
  }

  return (
    <main className="page-container narrow-container">
      <div className="page-heading"><div><p className="eyebrow">COMPTE</p><h1>Mon profil</h1><p className="muted">Gérez vos informations personnelles et votre mot de passe.</p></div></div>
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}
      <div className="two-column">
        <section className="content-card"><div className="section-heading"><div><p className="eyebrow">INFORMATIONS</p><h2>Profil utilisateur</h2></div><span className="role-pill dark">{profile?.role || '…'}</span></div><form onSubmit={updateProfile} className="form-stack"><label>Nom complet<input value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} required /></label><label>Email<input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required /></label><button className="button button-primary">Enregistrer les changements</button></form></section>
        <section className="content-card"><div className="section-heading"><div><p className="eyebrow">SÉCURITÉ</p><h2>Changer le mot de passe</h2></div></div><form onSubmit={changePassword} className="form-stack"><label>Ancien mot de passe<input type="password" value={password.currentPassword} onChange={(event) => setPassword({ ...password, currentPassword: event.target.value })} required /></label><label>Nouveau mot de passe<input type="password" minLength="8" value={password.newPassword} onChange={(event) => setPassword({ ...password, newPassword: event.target.value })} required /></label><label>Confirmation<input type="password" minLength="8" value={password.confirmation} onChange={(event) => setPassword({ ...password, confirmation: event.target.value })} required /></label><button className="button button-secondary">Modifier le mot de passe</button></form></section>
      </div>
    </main>
  )
}
