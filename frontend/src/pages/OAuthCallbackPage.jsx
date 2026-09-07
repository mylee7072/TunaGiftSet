import { useEffect, useRef } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { Loading } from "../components/common/Loading";
import { ApiError } from "../api/apiClient";
import { consumeOAuthCallback } from "../utils/oauth";

const PROVIDER_LABEL = { kakao: "카카오", google: "구글" };

export function OAuthCallbackPage() {
  const { provider } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { oauthLogin } = useAuth();
  const hasRequestedRef = useRef(false);

  useEffect(() => {
    if (hasRequestedRef.current) return;
    hasRequestedRef.current = true;

    const label = PROVIDER_LABEL[provider] || "소셜";
    const result = consumeOAuthCallback(provider, searchParams);
    if (!result.ok) {
      navigate("/login", { replace: true, state: { oauthError: `${label} 로그인에 실패했습니다. 다시 시도해 주세요.` } });
      return;
    }

    oauthLogin(provider, result.code, result.redirectUri)
      .then(() => navigate("/", { replace: true }))
      .catch((error) => {
        const message = error instanceof ApiError ? error.message : `${label} 로그인에 실패했습니다.`;
        navigate("/login", { replace: true, state: { oauthError: message } });
      });
  }, [provider, searchParams, navigate, oauthLogin]);

  return (
    <div className="container section">
      <Loading label="로그인 처리 중입니다..." />
    </div>
  );
}
