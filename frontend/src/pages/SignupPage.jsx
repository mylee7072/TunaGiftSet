import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { useToast } from "../context/useToast";
import { ApiError } from "../api/apiClient";

const INITIAL_FORM = { email: "", password: "", passwordConfirm: "", name: "", phone: "" };

export function SignupPage() {
  const { signup } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [form, setForm] = useState(INITIAL_FORM);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    document.title = "회원가입 - SeyoungGiftSet";
  }, []);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function validate() {
    if (!form.email.trim()) return { message: "이메일을 입력해 주세요.", field: "signup-email" };
    if (!form.password) return { message: "비밀번호를 입력해 주세요.", field: "signup-password" };
    if (form.password !== form.passwordConfirm) {
      return { message: "비밀번호가 일치하지 않습니다.", field: "signup-password-confirm" };
    }
    if (!form.name.trim()) return { message: "이름을 입력해 주세요.", field: "signup-name" };
    if (!form.phone.trim()) return { message: "휴대전화번호를 입력해 주세요.", field: "signup-phone" };
    return null;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationResult = validate();
    if (validationResult) {
      setError(validationResult.message);
      document.getElementById(validationResult.field)?.focus();
      return;
    }

    setError("");
    setSubmitting(true);
    try {
      await signup({
        email: form.email.trim(),
        password: form.password,
        name: form.name.trim(),
        phone: form.phone.trim(),
      });
      showToast("회원가입이 완료되었습니다. 로그인해 주세요.");
      navigate("/login", { replace: true });
    } catch (submitError) {
      setError(submitError instanceof ApiError ? submitError.message : "회원가입에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="container auth-page">
      <div className="auth-page__intro">
        <p className="eyebrow">Join</p>
        <h1 className="page-title">회원가입</h1>
        <p>선물세트 주문과 배송지 관리를 더 편하게 이용해 보세요.</p>
      </div>
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        <label htmlFor="signup-email">이메일</label>
        <input id="signup-email" name="email" type="email" autoComplete="email" value={form.email} onChange={handleChange} />

        <label htmlFor="signup-password">비밀번호</label>
        <input
          id="signup-password"
          name="password"
          type="password"
          autoComplete="new-password"
          value={form.password}
          onChange={handleChange}
        />
        <p className="form-hint">영문자와 숫자를 포함해 8자 이상 입력해 주세요.</p>

        <label htmlFor="signup-password-confirm">비밀번호 확인</label>
        <input
          id="signup-password-confirm"
          name="passwordConfirm"
          type="password"
          autoComplete="new-password"
          value={form.passwordConfirm}
          onChange={handleChange}
        />

        <label htmlFor="signup-name">이름</label>
        <input id="signup-name" name="name" type="text" autoComplete="name" value={form.name} onChange={handleChange} />

        <label htmlFor="signup-phone">휴대전화번호</label>
        <input
          id="signup-phone"
          name="phone"
          type="tel"
          autoComplete="tel"
          placeholder="01012345678"
          value={form.phone}
          onChange={handleChange}
        />

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        <button type="submit" className="btn btn--primary" disabled={submitting}>
          {submitting && <span className="btn__spinner" aria-hidden="true" />}
          {submitting ? "가입 처리 중..." : "회원가입"}
        </button>
      </form>
      <p className="auth-page__footer">
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </div>
  );
}
