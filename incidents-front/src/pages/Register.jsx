import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../services/api'
import { register } from '../services/authService'

export default function Register() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirmation: '' })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  function update(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    if (form.password !== form.confirmation) {
      setError('Les mots de passe ne correspondent pas')
      return
    }
    setLoading(true)
    try {
      await register({ fullName: form.fullName, email: form.email, password: form.password })
      setSuccess(true)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <main className="auth-page single-auth">
        <section className="auth-panel auth-form-panel success-card">
          <div className="success-icon">✓</div>
          <p className="eyebrow">COMPTE CRÉÉ</p>
          <h2>Bienvenue dans Incident Desk</h2>
          <p className="muted">Votre compte employé est prêt. Vous pouvez maintenant vous connecter.</p>
          <button className="button button-primary button-wide" onClick={() => navigate('/login')}>Aller à la connexion</button>
        </section>
      </main>
    )
  }

  return (
    <main className="auth-page single-auth">
      <section className="auth-panel auth-form-panel">
        <div className="form-heading">
          <Link className="brand" to="/login"><span className="brand-mark">ID</span> Incident Desk</Link>
          <p className="eyebrow">NOUVEAU COMPTE</p>
          <h2>Créer un compte</h2>
          <p className="muted">Les nouveaux comptes sont créés avec le rôle EMPLOYEE.</p>
        </div>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={submit} className="form-stack">
          <label>Nom complet<input name="fullName" value={form.fullName} onChange={update} required /></label>
          <label>Email<input name="email" type="email" autoComplete="email" value={form.email} onChange={update} required /></label>
          <label>Mot de passe<input name="password" type="password" minLength="8" autoComplete="new-password" value={form.password} onChange={update} required /></label>
          <label>Confirmation<input name="confirmation" type="password" minLength="8" autoComplete="new-password" value={form.confirmation} onChange={update} required /></label>
          <button className="button button-primary button-wide" disabled={loading}>{loading ? 'Création…' : 'Créer le compte'}</button>
        </form>
        <p className="form-footer">Déjà inscrit ? <Link to="/login">Se connecter</Link></p>
      </section>
    </main>
  )
}
