import { NavLink, useNavigate } from 'react-router-dom'
import { getFullName, getRole, isAuthenticated, logout } from '../services/authService'

export default function Navbar() {
  const navigate = useNavigate()
  const role = getRole()
  if (!isAuthenticated()) return null

  async function signOut() {
    await logout()
    navigate('/login')
  }

  return (
    <header className="topbar">
      <NavLink className="brand" to="/dashboard">
        <span className="brand-mark">ID</span>
        <span>Incident Desk</span>
      </NavLink>
      <nav className="nav-links">
        <NavLink to="/dashboard">Tableau de bord</NavLink>
        {role === 'EMPLOYEE' && <NavLink to="/incidents/my">Mes incidents</NavLink>}
        {role === 'TECHNICIAN' && <NavLink to="/incidents/assigned">Incidents affectés</NavLink>}
        {role === 'ADMIN' && <NavLink to="/admin/incidents">Tous les incidents</NavLink>}
        {role === 'ADMIN' && <NavLink to="/admin/users">Utilisateurs</NavLink>}
        <NavLink to="/profile">Mon profil</NavLink>
      </nav>
      <div className="account-area">
        <span className="account-name">{getFullName()}</span>
        <span className="role-pill">{role}</span>
        <button className="button button-ghost" onClick={signOut}>Déconnexion</button>
      </div>
    </header>
  )
}
