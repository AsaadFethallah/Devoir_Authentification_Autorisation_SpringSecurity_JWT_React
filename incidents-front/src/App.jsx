import { Navigate, Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import RoleRoute from './components/RoleRoute'
import AccessDenied from './pages/AccessDenied'
import AdminIncidents from './pages/AdminIncidents'
import AdminUsers from './pages/AdminUsers'
import AssignedIncidents from './pages/AssignedIncidents'
import Dashboard from './pages/Dashboard'
import IncidentDetails from './pages/IncidentDetails'
import Login from './pages/Login'
import MyIncidents from './pages/MyIncidents'
import NewIncident from './pages/NewIncident'
import Profile from './pages/Profile'
import Register from './pages/Register'

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/access-denied" element={<AccessDenied />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/incidents/:id" element={<IncidentDetails />} />
          <Route element={<RoleRoute roles={['EMPLOYEE']} />}>
            <Route path="/incidents/my" element={<MyIncidents />} />
            <Route path="/incidents/new" element={<NewIncident />} />
          </Route>
          <Route element={<RoleRoute roles={['TECHNICIAN']} />}>
            <Route path="/incidents/assigned" element={<AssignedIncidents />} />
          </Route>
          <Route element={<RoleRoute roles={['ADMIN']} />}>
            <Route path="/admin/users" element={<AdminUsers />} />
            <Route path="/admin/incidents" element={<AdminIncidents />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </>
  )
}
