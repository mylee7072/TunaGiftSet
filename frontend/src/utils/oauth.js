const PROVIDER_CONFIG = {
  kakao: {
    clientId: import.meta.env.VITE_KAKAO_CLIENT_ID,
    authorizeUrl: "https://kauth.kakao.com/oauth/authorize",
  },
  google: {
    clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID,
    authorizeUrl: "https://accounts.google.com/o/oauth2/v2/auth",
    extraParams: { scope: "openid email profile" },
  },
};

const STATE_STORAGE_KEY = "tunagiftset.oauthState";

function buildRedirectUri(provider) {
  return `${window.location.origin}/auth/${provider}/callback`;
}

export function isOAuthProviderConfigured(provider) {
  return Boolean(PROVIDER_CONFIG[provider]?.clientId);
}

// Redirects the browser to the provider's consent screen. A random `state` is stashed in
// sessionStorage and echoed back by the provider so the callback page can reject a
// forged/replayed redirect (basic CSRF protection for the OAuth authorization-code flow).
export function startOAuthLogin(provider) {
  const config = PROVIDER_CONFIG[provider];
  if (!config?.clientId) {
    throw new Error(`${provider} 로그인이 아직 설정되지 않았습니다.`);
  }

  const state = crypto.randomUUID();
  sessionStorage.setItem(STATE_STORAGE_KEY, state);

  const params = new URLSearchParams({
    client_id: config.clientId,
    redirect_uri: buildRedirectUri(provider),
    response_type: "code",
    state,
    ...config.extraParams,
  });

  window.location.href = `${config.authorizeUrl}?${params.toString()}`;
}

// Verifies the callback's `state` against the one stashed before redirecting, then clears it
// (single use). On success, returns the same redirectUri that must be sent back to the backend
// unchanged — providers validate the token exchange against the redirect_uri used to get the code.
export function consumeOAuthCallback(provider, searchParams) {
  const expectedState = sessionStorage.getItem(STATE_STORAGE_KEY);
  sessionStorage.removeItem(STATE_STORAGE_KEY);

  if (searchParams.get("error")) {
    return { ok: false };
  }

  const code = searchParams.get("code");
  const state = searchParams.get("state");
  if (!code || !state || !expectedState || state !== expectedState) {
    return { ok: false };
  }

  return { ok: true, code, redirectUri: buildRedirectUri(provider) };
}
