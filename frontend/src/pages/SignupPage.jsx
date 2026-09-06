import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { useToast } from "../context/useToast";
import {
  SIGNUP_FIELD_ORDER,
  getFirstInvalidField,
  resolveAuthServerError,
  validateSignupField,
  validateSignupForm,
} from "../utils/authValidation";

const INITIAL_FORM = { email: "", password: "", passwordConfirm: "", name: "", phone: "" };
const FIELD_IDS = {
  email: "signup-email",
  password: "signup-password",
  passwordConfirm: "signup-password-confirm",
  name: "signup-name",
  phone: "signup-phone",
};

export function SignupPage() {
  const { signup } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [form, setForm] = useState(INITIAL_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [validatedFields, setValidatedFields] = useState(() => new Set());
  const [serverError, setServerError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const fieldRefs = useRef({});

  useEffect(() => {
    document.title = "회원가입 - SeyoungGiftSet";
  }, []);

  function handleChange(event) {
    const { name, value } = event.target;
    setServerError("");
    setForm((current) => {
      const next = { ...current, [name]: value };
      setFieldErrors((currentErrors) => {
        const nextErrors = { ...currentErrors };
        const fieldsToValidate = new Set([name]);
        if (name === "password" && validatedFields.has("passwordConfirm")) {
          fieldsToValidate.add("passwordConfirm");
        }

        fieldsToValidate.forEach((field) => {
          if (!validatedFields.has(field)) return;
          const message = validateSignupField(field, next);
          if (message) {
            nextErrors[field] = message;
          } else {
            delete nextErrors[field];
          }
        });
        return nextErrors;
      });
      return next;
    });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationErrors = validateSignupForm(form);
    setValidatedFields(new Set(SIGNUP_FIELD_ORDER));
    setFieldErrors(validationErrors);
    setServerError("");

    const firstInvalidField = getFirstInvalidField(SIGNUP_FIELD_ORDER, validationErrors);
    if (firstInvalidField) {
      fieldRefs.current[firstInvalidField]?.focus();
      return;
    }

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
      setServerError(resolveAuthServerError(submitError, { duplicateEmailMessage: "이미 가입된 이메일입니다." }));
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
        <p className="eyebrow">Join</p>
        <h1 className="page-title">회원가입</h1>
        <p>선물세트 주문과 배송지 관리를 더 편하게 이용해 보세요.</p>
      </div>
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        {serverError && (
          <div className="auth-form__server-error" role="alert">
            {serverError}
          </div>
        )}

        <div className="form-field">
          <label htmlFor="signup-email">이메일</label>
          <input id="signup-email" name="email" type="email" autoComplete="email" value={form.email} onChange={handleChange} {...getInputProps("email")} />
          {renderFieldError("email")}
        </div>

        <div className="form-field">
          <label htmlFor="signup-password">비밀번호</label>
          <input
            id="signup-password"
            name="password"
            type="password"
            autoComplete="new-password"
            value={form.password}
            onChange={handleChange}
            {...getInputProps("password")}
          />
          <p className="form-hint">영문자와 숫자를 포함해 8자 이상 입력해 주세요.</p>
          {renderFieldError("password")}
        </div>

        <div className="form-field">
          <label htmlFor="signup-password-confirm">비밀번호 확인</label>
          <input
            id="signup-password-confirm"
            name="passwordConfirm"
            type="password"
            autoComplete="new-password"
            value={form.passwordConfirm}
            onChange={handleChange}
            {...getInputProps("passwordConfirm")}
          />
          {renderFieldError("passwordConfirm")}
        </div>

        <div className="form-field">
          <label htmlFor="signup-name">이름</label>
          <input id="signup-name" name="name" type="text" autoComplete="name" value={form.name} onChange={handleChange} {...getInputProps("name")} />
          {renderFieldError("name")}
        </div>

        <div className="form-field">
          <label htmlFor="signup-phone">휴대전화번호</label>
          <input
            id="signup-phone"
            name="phone"
            type="tel"
            autoComplete="tel"
            placeholder="01012345678"
            value={form.phone}
            onChange={handleChange}
            {...getInputProps("phone")}
          />
          {renderFieldError("phone")}
        </div>

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
