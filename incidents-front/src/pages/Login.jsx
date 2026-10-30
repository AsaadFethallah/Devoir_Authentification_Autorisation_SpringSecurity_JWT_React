import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../services/api'
import { login } from '../services/authService'

export default function Login() {
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [notice, setNotice] = useState('')

  useEffect(() => {
    const message = sessionStorage.getItem('sessionMessage')
    if (message) {
      setNotice(message)
      sessionStorage.removeItem('sessionMessage')
    }
  }, [])

  function update(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(form)
      navigate(location.state?.from || '/dashboard', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-panel auth-intro">
        <div className="brand large"><span className="brand-mark">ID</span> Incident Desk</div>
        <p className="eyebrow">SÉCURITÉ DES SYSTÈMES DISTRIBUÉS</p>
        <h1>Suivre chaque incident avec clarté.</h1>
        <p className="muted light-text">Une plateforme unique pour déclarer, affecter et résoudre les incidents informatiques.</p>
        <div className="intro-points">
          <span>✓ Contrôle des accès par rôle</span>
          <span>✓ Historique de traitement</span>
          <span>✓ API protégée par JWT</span>
        </div>
      </section>
      <section className="auth-panel auth-form-panel">
        <div className="form-heading">
          <p className="eyebrow">ESPACE SÉCURISÉ</p>
          <h2>Connexion</h2>
          <p className="muted">Connectez-vous à votre espace incident.</p>
        </div>
        {notice && <div className="alert alert-info">{notice}</div>}
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={submit} className="form-stack">
          <label>Email<input name="email" type="email" autoComplete="email" value={form.email} onChange={update} required /></label>
          <label>Mot de passe<input name="password" type="password" autoComplete="current-password" value={form.password} onChange={update} required /></label>
          <button className="button button-primary button-wide" disabled={loading}>{loading ? 'Connexion…' : 'Se connecter'}</button>
        </form>
        <p className="form-footer">Pas encore de compte ? <Link to="/register">Créer un compte</Link></p>
      </section>
    </main>
  )
}
