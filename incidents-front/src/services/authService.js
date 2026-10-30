import { api, clearSession, saveSession } from './api'

export async function login(credentials) {
  const { data } = await api.post('/auth/login', credentials)
  saveSession(data)
  return data
}

export async function register(payload) {
  const { data } = await api.post('/auth/register', payload)
  return data
}

export async function logout() {
  const refreshToken = localStorage.getItem('refreshToken')
  try {
    if (refreshToken) await api.post('/auth/logout', { refreshToken })
  } finally {
    clearSession()
  }
}

export function isAuthenticated() {
  return Boolean(localStorage.getItem('jwt'))
}

export function getRole() {
  return localStorage.getItem('role')
}

export function getFullName() {
  return localStorage.getItem('fullName') || ''
}

export function replaceToken(token) {
  if (token) localStorage.setItem('jwt', token)
}
