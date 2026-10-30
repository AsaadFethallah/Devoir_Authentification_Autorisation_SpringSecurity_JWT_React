import axios from 'axios'

export const API_BASE_URL = 'http://localhost:8080'

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' }
})

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' }
})

let refreshPromise = null

export function saveSession(data) {
  localStorage.setItem('jwt', data.token)
  localStorage.setItem('refreshToken', data.refreshToken)
  localStorage.setItem('role', data.role)
  localStorage.setItem('fullName', data.fullName)
}

export function clearSession() {
  localStorage.removeItem('jwt')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('role')
  localStorage.removeItem('fullName')
}

function forceLoginAfterExpiration() {
  clearSession()
  sessionStorage.setItem('sessionMessage', 'Votre session a expiré, veuillez vous reconnecter')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status
    const url = error.config?.url || ''
    const isAuthRoute = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout'].some((route) => url.includes(route))
    const refreshToken = localStorage.getItem('refreshToken')
    const originalRequest = error.config

    if (status === 401 && !isAuthRoute && !originalRequest?._retry && refreshToken) {
      originalRequest._retry = true
      try {
        if (!refreshPromise) {
          refreshPromise = refreshClient.post('/auth/refresh', { refreshToken })
            .then(({ data }) => {
              saveSession(data)
              return data
            })
            .finally(() => { refreshPromise = null })
        }
        const refreshed = await refreshPromise
        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${refreshed.token}`
        return api(originalRequest)
      } catch (refreshError) {
        forceLoginAfterExpiration()
        return Promise.reject(refreshError)
      }
    }

    if (status === 401 && !isAuthRoute) {
      forceLoginAfterExpiration()
    }
    return Promise.reject(error)
  }
)

export function getErrorMessage(error) {
  const data = error.response?.data
  if (data?.errors?.length) {
    return `${data.message} : ${data.errors.map((item) => `${item.field} (${item.message})`).join(', ')}`
  }
  return data?.message || error.message || 'Une erreur est survenue'
}
