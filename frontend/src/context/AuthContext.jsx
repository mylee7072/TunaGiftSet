import { useCallback, useEffect, useMemo, useState } from "react";
import { authApi, memberApi } from "../api/authApi";
import { getStoredToken, onUnauthorized, setStoredToken } from "../api/apiClient";
import { AuthContext } from "./authContextValue";

export function AuthProvider({ children }) {
  const [member, setMember] = useState(null);
  // Starts true whenever a token exists, so protected routes don't flash a
  // "not logged in" state while we're still confirming the token with the server.
  const [isLoading, setIsLoading] = useState(Boolean(getStoredToken()));

  const clearSession = useCallback(() => {
    setStoredToken(null);
    setMember(null);
  }, []);

  useEffect(() => {
    onUnauthorized(() => clearSession());
  }, [clearSession]);

  useEffect(() => {
    const token = getStoredToken();
    if (!token) {
      setIsLoading(false);
      return;
    }

    memberApi
      .getMe()
      .then((data) => setMember(data))
      .catch(() => clearSession())
      .finally(() => setIsLoading(false));
  }, [clearSession]);

  const login = useCallback(async (email, password) => {
    const response = await authApi.login({ email, password });
    setStoredToken(response.accessToken);
    setMember(response.member);
    return response.member;
  }, []);

  const signup = useCallback((payload) => authApi.signup(payload), []);

  const oauthLogin = useCallback(async (provider, code, redirectUri) => {
    const response = await authApi.oauthLogin(provider, { code, redirectUri });
    setStoredToken(response.accessToken);
    setMember(response.member);
    return response.member;
  }, []);

  // Stateless JWT: there is no server-side session to invalidate, so "logout" is
  // discarding the local token. This matches the current backend's auth model —
  // it does not expose a logout endpoint (see AuthController).
  const logout = useCallback(() => {
    clearSession();
  }, [clearSession]);

  const value = useMemo(
    () => ({
      member,
      isAuthenticated: Boolean(member),
      isLoading,
      login,
      signup,
      oauthLogin,
      logout,
    }),
    [member, isLoading, login, signup, oauthLogin, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
