const API_BASE = 'http://localhost:8080/api/v1';
const ACCESS_TOKEN_KEY = 'voiceos_access_token';
const USER_KEY = 'voiceos_user';

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function storeAuth(auth) {
  if (auth?.accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, auth.accessToken);
  }
  if (auth?.user) {
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
  }
}

export function clearAuth() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function authHeaders(extra = {}) {
  const token = getAccessToken();
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra
  };
}

export async function apiFetch(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...authHeaders(),
      ...(options.headers || {})
    }
  });
  if (res.status === 401) {
    clearAuth();
    window.dispatchEvent(new Event('voiceos:unauthorized'));
  }
  return res;
}

export async function login(email, password) {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  if (!res.ok) {
    throw new Error('Invalid email or password');
  }
  const auth = await res.json();
  storeAuth(auth);
  window.dispatchEvent(new Event('voiceos:auth-changed'));
  return auth;
}

export async function register(displayName, email, password) {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ displayName, email, password })
  });
  if (!res.ok) {
    throw new Error('Registration failed');
  }
  const auth = await res.json();
  storeAuth(auth);
  window.dispatchEvent(new Event('voiceos:auth-changed'));
  return auth;
}

export function logout() {
  clearAuth();
  window.dispatchEvent(new Event('voiceos:auth-changed'));
}

export async function fetchActionTimeline(actionId) {
  if (!actionId) {
    return null;
  }
  const res = await apiFetch(`/actions/${actionId}/timeline`);
  if (res.status === 401 || res.status === 404) {
    return null;
  }
  if (!res.ok) {
    return null;
  }
  return res.json();
}
