import { apiClient } from "./apiClient";

export const authApi = {
  signup: (payload) => apiClient.post("/api/auth/signup", payload),
  login: (payload) => apiClient.post("/api/auth/login", payload),
  oauthLogin: (provider, payload) => apiClient.post(`/api/auth/oauth/${provider}`, payload),
};

export const memberApi = {
  getMe: () => apiClient.get("/api/members/me"),
};
