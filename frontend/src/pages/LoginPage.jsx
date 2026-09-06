import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { ApiError } from "../api/apiClient";

function resolveRedirectTarget(location) {
  const from = location.state?.from;
  if (from && typeof from === "object" && typeof from.pathname === "string") {
    return `${from.pathname}${from.search || ""}`;
  }
  return "/";
}

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    document.title = "로그인 - SeyoungGiftSet";
  }, []);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    if (!form.email.trim()) {
      setError("이메일과 비밀번호를 입력해 주세요.");
      document.getElementById("login-email")?.focus();
      return;
    }
    if (!form.password) {
      setError("이메일과 비밀번호를 입력해 주세요.");
      document.getElementById("login-password")?.focus();
      return;
    }

    setSubmitting(true);
    try {
      await login(form.email.trim(), form.password);
      navigate(resolveRedirectTarget(location), { replace: true });
    } catch (submitError) {
      setError(submitError instanceof ApiError ? submitError.message : "로그인에 실패했습니다.");
      document.getElementById("login-password")?.focus();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="container auth-page">
      <div className="auth-page__intro">
        <p className="eyebrow">Member</p>
        <h1 className="page-title">로그인</h1>
        <p>주문내역, 배송지, 찜한 상품을 안전하게 관리하세요.</p>
      </div>
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <label htmlFor="login-email">이메일</label>
        <input
          id="login-email"
          name="email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={handleChange}
        />

        <label htmlFor="login-password">비밀번호</label>
        <input
          id="login-password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={form.password}
          onChange={handleChange}
        />

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        <button type="submit" className="btn btn--primary" disabled={submitting}>
          {submitting && <span className="btn__spinner" aria-hidden="true" />}
          {submitting ? "로그인 중..." : "로그인"}
        </button>
      </form>
      <p className="auth-page__footer">
        아직 회원이 아니신가요? <Link to="/signup">회원가입</Link>
      </p>
    </div>
  );
}
