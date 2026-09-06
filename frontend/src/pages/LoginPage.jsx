import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import {
  LOGIN_FIELD_ORDER,
  getFirstInvalidField,
  resolveAuthServerError,
  validateLoginField,
  validateLoginForm,
} from "../utils/authValidation";

const FIELD_IDS = {
  email: "login-email",
  password: "login-password",
};

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
  const [fieldErrors, setFieldErrors] = useState({});
  const [validatedFields, setValidatedFields] = useState(() => new Set());
  const [serverError, setServerError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const fieldRefs = useRef({});

  useEffect(() => {
    document.title = "로그인 - SeyoungGiftSet";
  }, []);

  function handleChange(event) {
    const { name, value } = event.target;
    setServerError("");
    setForm((current) => {
      const next = { ...current, [name]: value };
      if (validatedFields.has(name)) {
        setFieldErrors((currentErrors) => {
          const message = validateLoginField(name, next);
          const nextErrors = { ...currentErrors };
          if (message) {
            nextErrors[name] = message;
          } else {
            delete nextErrors[name];
          }
          return nextErrors;
        });
      }
      return next;
    });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationErrors = validateLoginForm(form);
    setValidatedFields(new Set(LOGIN_FIELD_ORDER));
    setFieldErrors(validationErrors);
    setServerError("");

    const firstInvalidField = getFirstInvalidField(LOGIN_FIELD_ORDER, validationErrors);
    if (firstInvalidField) {
      fieldRefs.current[firstInvalidField]?.focus();
      return;
    }

    setSubmitting(true);
    try {
      await login(form.email.trim(), form.password);
      navigate(resolveRedirectTarget(location), { replace: true });
    } catch (submitError) {
      setServerError(resolveAuthServerError(submitError));
      fieldRefs.current.password?.focus();
    } finally {
      setSubmitting(false);
    }
  }

  function renderFieldError(fieldName) {
    const message = fieldErrors[fieldName];
    if (!message) return null;
    return (
      <p id={`${FIELD_IDS[fieldName]}-error`} className="form-field__error">
        {message}
      </p>
    );
  }

  function getInputProps(fieldName) {
    const hasError = Boolean(fieldErrors[fieldName]);
    return {
      ref: (node) => {
        fieldRefs.current[fieldName] = node;
      },
      "aria-invalid": hasError ? "true" : undefined,
      "aria-describedby": hasError ? `${FIELD_IDS[fieldName]}-error` : undefined,
      className: hasError ? "is-invalid" : undefined,
    };
  }

  return (
    <div className="container auth-page">
      <div className="auth-page__intro">
        <p className="eyebrow">Member</p>
        <h1 className="page-title">로그인</h1>
        <p>주문내역, 배송지, 찜한 상품을 안전하게 관리하세요.</p>
      </div>
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        {serverError && (
          <div className="auth-form__server-error" role="alert">
            {serverError}
          </div>
        )}

        <div className="form-field">
          <label htmlFor="login-email">이메일</label>
          <input
            id="login-email"
            name="email"
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={handleChange}
            {...getInputProps("email")}
          />
          {renderFieldError("email")}
        </div>

        <div className="form-field">
          <label htmlFor="login-password">비밀번호</label>
          <input
            id="login-password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={form.password}
            onChange={handleChange}
            {...getInputProps("password")}
          />
          {renderFieldError("password")}
        </div>

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
