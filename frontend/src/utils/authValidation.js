import { ApiError } from "../api/apiClient";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;
const KOREAN_MOBILE_PATTERN = /^010\d{7,8}$/;

export const SIGNUP_FIELD_ORDER = ["email", "password", "passwordConfirm", "name", "phone"];
export const LOGIN_FIELD_ORDER = ["email", "password"];

export function validateSignupField(name, form) {
  if (name === "email") {
    const email = form.email.trim();
    if (!email) return "이메일을 입력해 주세요.";
    if (!EMAIL_PATTERN.test(email)) return "올바른 이메일 형식이 아닙니다.";
  }

  if (name === "password") {
    if (!form.password) return "비밀번호를 입력해 주세요.";
    if (!PASSWORD_PATTERN.test(form.password)) return "영문자와 숫자를 포함해 8자 이상 입력해 주세요.";
  }

  if (name === "passwordConfirm") {
    if (!form.passwordConfirm) return "비밀번호를 다시 입력해 주세요.";
    if (form.password !== form.passwordConfirm) return "비밀번호가 일치하지 않습니다.";
  }

  if (name === "name" && !form.name.trim()) {
    return "이름을 입력해 주세요.";
  }

  if (name === "phone") {
    const phone = form.phone.trim();
    if (!phone) return "휴대전화번호를 입력해 주세요.";
    if (!KOREAN_MOBILE_PATTERN.test(phone)) return "올바른 휴대전화번호를 입력해 주세요.";
  }

  return "";
}

export function validateSignupForm(form) {
  return collectFieldErrors(SIGNUP_FIELD_ORDER, form, validateSignupField);
}

export function validateLoginField(name, form) {
  if (name === "email") {
    const email = form.email.trim();
    if (!email) return "이메일을 입력해 주세요.";
    if (!EMAIL_PATTERN.test(email)) return "올바른 이메일 형식이 아닙니다.";
  }

  if (name === "password" && !form.password) {
    return "비밀번호를 입력해 주세요.";
  }

  return "";
}

export function validateLoginForm(form) {
  return collectFieldErrors(LOGIN_FIELD_ORDER, form, validateLoginField);
}

export function getFirstInvalidField(fieldOrder, fieldErrors) {
  return fieldOrder.find((field) => Boolean(fieldErrors[field])) || null;
}

export function resolveAuthServerError(error, { duplicateEmailMessage } = {}) {
  if (error instanceof ApiError) {
    if (error.status === 0 || error.code === "NETWORK_ERROR") {
      return "서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.";
    }
    if (error.status === 409 && duplicateEmailMessage) {
      return duplicateEmailMessage;
    }
    return error.message || "요청을 처리하지 못했습니다.";
  }
  return "요청을 처리하지 못했습니다.";
}

function collectFieldErrors(fieldOrder, form, validator) {
  return fieldOrder.reduce((errors, field) => {
    const message = validator(field, form);
    if (message) {
      errors[field] = message;
    }
    return errors;
  }, {});
}
