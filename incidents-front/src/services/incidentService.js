import { api } from './api'

export const incidentService = {
  listMine: () => api.get('/api/incidents/my').then((response) => response.data),
  listAssigned: () => api.get('/api/incidents/assigned').then((response) => response.data),
  listAll: () => api.get('/api/incidents').then((response) => response.data),
  get: (id) => api.get(`/api/incidents/${id}`).then((response) => response.data),
  create: (payload) => api.post('/api/incidents', payload).then((response) => response.data),
  updateStatus: (id, status) => api.put(`/api/incidents/${id}/status`, { status }).then((response) => response.data),
  assign: (id, technicianId) => api.put(`/api/incidents/${id}/assign/${technicianId}`).then((response) => response.data),
  comments: (id) => api.get(`/api/incidents/${id}/comments`).then((response) => response.data),
  history: (id) => api.get(`/api/incidents/${id}/history`).then((response) => response.data),
  addComment: (id, content) => api.post(`/api/incidents/${id}/comments`, { content }).then((response) => response.data),
  adminUsers: () => api.get('/api/admin/users').then((response) => response.data),
  enableUser: (id) => api.put(`/api/admin/users/${id}/enable`).then((response) => response.data),
  disableUser: (id) => api.put(`/api/admin/users/${id}/disable`).then((response) => response.data),
  statistics: () => api.get('/api/admin/statistics').then((response) => response.data)
}
