import { Navigate, Outlet } from 'react-router-dom'
import { getRole } from '../services/authService'

export default function RoleRoute({ roles }) {
  return roles.includes(getRole()) ? <Outlet /> : <Navigate to="/access-denied" replace />
}
