import { useEffect, useRef, useState } from "react";
import { openKakaoPostcode } from "../../api/kakaoPostcode";

const EMPTY_FORM = {
  addressName: "",
  recipientName: "",
  recipientPhone: "",
  zipCode: "",
  roadAddress: "",
  jibunAddress: "",
  detailAddress: "",
  extraAddress: "",
  defaultAddress: false,
};

export function AddressForm({ initialValue, submitLabel = "저장", submitting = false, onSubmit, onCancel }) {
  const detailAddressRef = useRef(null);
  const [form, setForm] = useState(() => ({ ...EMPTY_FORM, ...initialValue }));
  const [error, setError] = useState("");
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    setForm({ ...EMPTY_FORM, ...initialValue });
  }, [initialValue]);

  function handleChange(event) {
    const { name, value, type, checked } = event.target;
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  }

  async function handleAddressSearch() {
    setError("");
    setSearching(true);
    try {
      await openKakaoPostcode((result) => {
        setForm((current) => ({
          ...current,
          zipCode: result.zipCode,
          roadAddress: result.roadAddress,
          jibunAddress: result.jibunAddress,
          extraAddress: result.extraAddress,
        }));
        window.setTimeout(() => detailAddressRef.current?.focus(), 0);
      });
    } catch {
      setError("주소검색 서비스를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setSearching(false);
    }
  }

  function validate() {
    if (!form.recipientName.trim()) return "수령인 이름을 입력해 주세요.";
    if (!form.recipientPhone.trim()) return "연락처를 입력해 주세요.";
    if (!/^01\d-?\d{3,4}-?\d{4}$/.test(form.recipientPhone.trim())) return "연락처를 확인해 주세요.";
    if (!/^\d{5}$/.test(form.zipCode.trim())) return "주소를 검색해 우편번호를 입력해 주세요.";
    if (!form.roadAddress.trim()) return "주소를 검색해 도로명주소를 입력해 주세요.";
    if (!form.detailAddress.trim()) return "상세주소를 입력해 주세요.";
    return "";
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationMessage = validate();
    if (validationMessage) {
      setError(validationMessage);
      return;
    }
    setError("");
    await onSubmit({
      addressName: form.addressName.trim(),
      recipientName: form.recipientName.trim(),
      recipientPhone: form.recipientPhone.replace(/\D/g, ""),
      zipCode: form.zipCode.trim(),
      roadAddress: form.roadAddress.trim(),
      jibunAddress: form.jibunAddress.trim(),
      detailAddress: form.detailAddress.trim(),
      extraAddress: form.extraAddress.trim(),
      defaultAddress: form.defaultAddress,
    });
  }

  return (
    <form className="address-form" onSubmit={handleSubmit} noValidate>
      <label htmlFor="addressName">배송지명</label>
      <input id="addressName" name="addressName" placeholder="집, 회사" value={form.addressName} onChange={handleChange} />

      <label htmlFor="recipientName">수령인</label>
      <input id="recipientName" name="recipientName" value={form.recipientName} onChange={handleChange} />

      <label htmlFor="recipientPhone">연락처</label>
      <input id="recipientPhone" name="recipientPhone" placeholder="01012345678" value={form.recipientPhone} onChange={handleChange} />

      <label htmlFor="zipCode">주소</label>
      <div className="address-form__search-row">
        <input id="zipCode" name="zipCode" placeholder="우편번호" value={form.zipCode} onChange={handleChange} />
        <button type="button" className="btn btn--secondary" onClick={handleAddressSearch} disabled={searching}>
          {searching && <span className="btn__spinner" aria-hidden="true" />}
          {searching ? "불러오는 중" : "주소검색"}
        </button>
      </div>

      <input aria-label="도로명주소" name="roadAddress" placeholder="도로명주소" value={form.roadAddress} onChange={handleChange} />
      <input aria-label="지번주소" name="jibunAddress" placeholder="지번주소" value={form.jibunAddress} onChange={handleChange} />
      <input
        ref={detailAddressRef}
        aria-label="상세주소"
        name="detailAddress"
        placeholder="상세주소"
        value={form.detailAddress}
        onChange={handleChange}
      />
      <input aria-label="참고항목" name="extraAddress" placeholder="참고항목" value={form.extraAddress} onChange={handleChange} />

      <label className="address-form__checkbox">
        <input type="checkbox" name="defaultAddress" checked={form.defaultAddress} onChange={handleChange} />
        기본배송지로 설정
      </label>

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      <div className="address-form__actions">
        {onCancel && (
          <button type="button" className="btn btn--secondary" onClick={onCancel} disabled={submitting}>
            취소
          </button>
        )}
        <button type="submit" className="btn btn--primary" disabled={submitting}>
          {submitting && <span className="btn__spinner" aria-hidden="true" />}
          {submitting ? "저장 중" : submitLabel}
        </button>
      </div>
    </form>
  );
}
