import { Link } from 'react-router-dom'

export default function AccessDenied() {
  return <main className="page-container centered-state"><div className="error-code">403</div><p className="eyebrow">ACCÈS REFUSÉ</p><h1>Cette page ne vous est pas accessible.</h1><p className="muted">Votre rôle ne permet pas d’utiliser cette fonctionnalité.</p><Link className="button button-primary" to="/dashboard">Retour au tableau de bord</Link></main>
}
