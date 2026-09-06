const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
const TOKEN_STORAGE_KEY = "tunagiftset.accessToken";

// The backend is stateless JWT over an Authorization header (no session cookie, no
// CSRF token) — see JwtAuthenticationFilter/SecurityConfig. That means there is no
// httpOnly-cookie option available today; localStorage is the only storage that can
// be read back to build that header on every request. This trades some XSS exposure
// for working within the current backend's actual auth model — moving to httpOnly
// cookies would be a backend change (see report). We deliberately do not fall back
// to writing our own cookie handling to compensate.
export function getStoredToken() {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setStoredToken(token) {
  try {
    if (token) {
      localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
  } catch {
    // Storage can be unavailable (private mode); auth simply won't persist.
  }
}

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

let unauthorizedHandler = null;
// Registered once by AuthContext so a 401 from any API call can clear the session
// and redirect. A single registration point avoids each caller re-implementing this.
export function onUnauthorized(handler) {
  unauthorizedHandler = handler;
}

async function request(path, { method = "GET", body, signal } = {}) {
  const headers = { Accept: "application/json" };
  const token = getStoredToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  // FormData (file uploads) must go through untouched — the browser sets its own
  // multipart Content-Type with the correct boundary, which JSON.stringify would destroy.
  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;
  if (body !== undefined && !isFormData) {
    headers["Content-Type"] = "application/json";
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
      signal,
    });
  } catch {
    throw new ApiError(0, "NETWORK_ERROR", "네트워크 연결을 확인해 주세요.");
  }

  if (response.status === 204) {
    return null;
  }

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const data = isJson ? await response.json().catch(() => null) : null;

  if (!response.ok) {
    const code = data?.code || "UNKNOWN_ERROR";
    const message = data?.message || "요청을 처리하지 못했습니다.";

    if (response.status === 401) {
      unauthorizedHandler?.();
    }

    throw new ApiError(response.status, code, message);
  }

  return data;
}

export const apiClient = {
  get: (path, options) => request(path, { ...options, method: "GET" }),
  post: (path, body, options) => request(path, { ...options, method: "POST", body }),
  patch: (path, body, options) => request(path, { ...options, method: "PATCH", body }),
  put: (path, body, options) => request(path, { ...options, method: "PUT", body }),
  delete: (path, body, options) => request(path, { ...options, method: "DELETE", body }),
};
