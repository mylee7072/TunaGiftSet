import { useEffect, useState } from "react";
import { adminCouponApi } from "../../api/couponApi";
import { Loading } from "../../components/common/Loading";
import { ErrorState } from "../../components/common/ErrorState";
import { Pagination } from "../../components/common/Pagination";
import { useToast } from "../../context/useToast";
import { ApiError } from "../../api/apiClient";
import { formatCouponCondition, formatCouponDiscount, formatDateTime } from "../../utils/format";

const EMPTY_FORM = {
  name: "",
  code: "",
  discountType: "FIXED_AMOUNT",
  discountValue: "",
  minimumOrderAmount: "0",
  maximumDiscountAmount: "",
  validFrom: "",
  validUntil: "",
  status: "ACTIVE",
  totalIssueLimit: "",
  perMemberLimit: "1",
};

function toInstant(datetimeLocalValue) {
  if (!datetimeLocalValue) return null;
  return new Date(datetimeLocalValue).toISOString();
}

export function AdminCouponsPage() {
  const { showToast } = useToast();
  const [statusFilter, setStatusFilter] = useState("");
  const [page, setPage] = useState(0);
  const [pageData, setPageData] = useState(null);
  const [status, setStatus] = useState("loading");

  const [creating, setCreating] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [issuingCoupon, setIssuingCoupon] = useState(null);
  const [issueMemberId, setIssueMemberId] = useState("");
  const [issueError, setIssueError] = useState("");
  const [issuing, setIssuing] = useState(false);

  function load() {
    setStatus("loading");
    adminCouponApi
      .findCoupons(statusFilter || undefined, page, 20)
      .then((data) => {
        setPageData(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, page]);

  function startCreate() {
    setCreating(true);
    setForm(EMPTY_FORM);
    setFormError("");
  }

  async function handleCreateSubmit(event) {
    event.preventDefault();
    if (submitting) return;

    if (!form.name.trim() || !form.code.trim() || !form.discountValue || !form.validFrom || !form.validUntil) {
      setFormError("필수 항목을 모두 입력해 주세요.");
      return;
    }

    const payload = {
      name: form.name.trim(),
      code: form.code.trim(),
      discountType: form.discountType,
      discountValue: Number(form.discountValue),
      minimumOrderAmount: Number(form.minimumOrderAmount) || 0,
      maximumDiscountAmount: form.maximumDiscountAmount === "" ? null : Number(form.maximumDiscountAmount),
      validFrom: toInstant(form.validFrom),
      validUntil: toInstant(form.validUntil),
      status: form.status,
      totalIssueLimit: form.totalIssueLimit === "" ? null : Number(form.totalIssueLimit),
      perMemberLimit: Number(form.perMemberLimit) || 1,
    };

    setSubmitting(true);
    setFormError("");
    try {
      await adminCouponApi.createCoupon(payload);
      showToast("쿠폰을 등록했습니다.");
      setCreating(false);
      setPage(0);
      load();
    } catch (submitError) {
      setFormError(submitError instanceof ApiError ? submitError.message : "쿠폰 등록에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  function startIssue(coupon) {
    setIssuingCoupon(coupon);
    setIssueMemberId("");
    setIssueError("");
  }

  async function handleIssueSubmit(event) {
    event.preventDefault();
    if (issuing) return;
    const memberId = Number(issueMemberId);
    if (!memberId || memberId <= 0) {
      setIssueError("회원 ID를 올바르게 입력해 주세요.");
      return;
    }

    setIssuing(true);
    setIssueError("");
    try {
      await adminCouponApi.issueCoupon(issuingCoupon.id, memberId);
      showToast(`회원 ID ${memberId}에게 쿠폰을 발급했습니다.`);
      setIssuingCoupon(null);
    } catch (issueSubmitError) {
      setIssueError(issueSubmitError instanceof ApiError ? issueSubmitError.message : "쿠폰 발급에 실패했습니다.");
    } finally {
      setIssuing(false);
    }
  }

  return (
    <div className="admin-page">
      <div className="admin-page__header">
        <h1 className="page-title">쿠폰 관리</h1>
        {!creating && (
          <button type="button" className="btn btn--primary" onClick={startCreate}>
            쿠폰 등록
          </button>
        )}
      </div>

      {creating && (
        <form className="admin-form" onSubmit={handleCreateSubmit} noValidate>
          <h2>쿠폰 등록</h2>

          <label htmlFor="coupon-name">쿠폰명</label>
          <input id="coupon-name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} maxLength={150} />

          <label htmlFor="coupon-code">쿠폰 코드</label>
          <input id="coupon-code" value={form.code} onChange={(event) => setForm({ ...form, code: event.target.value })} maxLength={50} />

          <label htmlFor="coupon-discount-type">할인 유형</label>
          <select id="coupon-discount-type" value={form.discountType} onChange={(event) => setForm({ ...form, discountType: event.target.value })}>
            <option value="FIXED_AMOUNT">정액 할인</option>
            <option value="PERCENTAGE">정률 할인</option>
          </select>

          <label htmlFor="coupon-discount-value">
            할인 값 {form.discountType === "PERCENTAGE" ? "(%, 1~100)" : "(원)"}
          </label>
          <input
            id="coupon-discount-value"
            type="number"
            min={1}
            max={form.discountType === "PERCENTAGE" ? 100 : undefined}
            value={form.discountValue}
            onChange={(event) => setForm({ ...form, discountValue: event.target.value })}
          />

          <label htmlFor="coupon-min-order">최소 주문금액 (원)</label>
          <input
            id="coupon-min-order"
            type="number"
            min={0}
            value={form.minimumOrderAmount}
            onChange={(event) => setForm({ ...form, minimumOrderAmount: event.target.value })}
          />

          {form.discountType === "PERCENTAGE" && (
            <>
              <label htmlFor="coupon-max-discount">최대 할인금액 (원, 선택)</label>
              <input
                id="coupon-max-discount"
                type="number"
                min={1}
                value={form.maximumDiscountAmount}
                onChange={(event) => setForm({ ...form, maximumDiscountAmount: event.target.value })}
              />
            </>
          )}

          <label htmlFor="coupon-valid-from">사용 시작일시</label>
          <input
            id="coupon-valid-from"
            type="datetime-local"
            value={form.validFrom}
            onChange={(event) => setForm({ ...form, validFrom: event.target.value })}
          />

          <label htmlFor="coupon-valid-until">사용 종료일시</label>
          <input
            id="coupon-valid-until"
            type="datetime-local"
            value={form.validUntil}
            onChange={(event) => setForm({ ...form, validUntil: event.target.value })}
          />

          <label htmlFor="coupon-status">상태</label>
          <select id="coupon-status" value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>
            <option value="ACTIVE">활성</option>
            <option value="INACTIVE">비활성</option>
          </select>

          <label htmlFor="coupon-total-limit">전체 발급 한도 (선택, 비우면 무제한)</label>
          <input
            id="coupon-total-limit"
            type="number"
            min={1}
            value={form.totalIssueLimit}
            onChange={(event) => setForm({ ...form, totalIssueLimit: event.target.value })}
          />

          <label htmlFor="coupon-member-limit">회원당 발급 한도</label>
          <input
            id="coupon-member-limit"
            type="number"
            min={1}
            value={form.perMemberLimit}
            onChange={(event) => setForm({ ...form, perMemberLimit: event.target.value })}
          />

          {formError && (
            <p className="form-error" role="alert">
              {formError}
            </p>
          )}

          <div className="admin-form__actions">
            <button type="button" className="btn btn--secondary" onClick={() => setCreating(false)}>
              취소
            </button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              등록
            </button>
          </div>
        </form>
      )}

      <div className="admin-filter-bar">
        <label htmlFor="coupon-status-filter">상태</label>
        <select
          id="coupon-status-filter"
          value={statusFilter}
          onChange={(event) => {
            setPage(0);
            setStatusFilter(event.target.value);
          }}
        >
          <option value="">전체</option>
          <option value="ACTIVE">활성</option>
          <option value="INACTIVE">비활성</option>
        </select>
      </div>

      {status === "loading" && <Loading />}
      {status === "error" && <ErrorState message="쿠폰 목록을 불러오지 못했습니다." onRetry={load} />}

      {status === "ready" && pageData && (
        <>
          <div className="admin-table-wrapper">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>쿠폰명</th>
                  <th>코드</th>
                  <th>할인</th>
                  <th>최소주문금액</th>
                  <th>유효기간</th>
                  <th>발급한도</th>
                  <th>상태</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((coupon) => (
                  <tr key={coupon.id}>
                    <td>{coupon.id}</td>
                    <td>{coupon.name}</td>
                    <td>{coupon.code}</td>
                    <td>{formatCouponDiscount(coupon)}</td>
                    <td>{formatCouponCondition(coupon)}</td>
                    <td>
                      {formatDateTime(coupon.validFrom)} ~ {formatDateTime(coupon.validUntil)}
                    </td>
                    <td>
                      {coupon.totalIssueLimit ?? "무제한"} / 회원당 {coupon.perMemberLimit}
                    </td>
                    <td>
                      <span className={`admin-badge${coupon.status === "ACTIVE" ? " admin-badge--active" : ""}`}>
                        {coupon.status === "ACTIVE" ? "활성" : "비활성"}
                      </span>
                    </td>
                    <td>
                      <button type="button" className="btn btn--secondary" onClick={() => startIssue(coupon)}>
                        회원 발급
                      </button>
                    </td>
                  </tr>
                ))}
                {pageData.content.length === 0 && (
                  <tr>
                    <td colSpan={9} className="admin-table__empty">
                      등록된 쿠폰이 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <Pagination page={pageData.page} totalPages={pageData.totalPages} onPageChange={setPage} />
        </>
      )}

      {issuingCoupon && (
        <div className="modal-overlay" role="presentation" onClick={() => setIssuingCoupon(null)}>
          <div className="modal" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}>
            <h2>쿠폰 발급 — {issuingCoupon.name}</h2>
            <form onSubmit={handleIssueSubmit} noValidate>
              <label htmlFor="issue-member-id">회원 ID</label>
              <input
                id="issue-member-id"
                type="number"
                min={1}
                value={issueMemberId}
                onChange={(event) => setIssueMemberId(event.target.value)}
                autoFocus
              />
              {issueError && (
                <p className="form-error" role="alert">
                  {issueError}
                </p>
              )}
              <div className="modal__actions">
                <button type="button" className="btn btn--secondary" onClick={() => setIssuingCoupon(null)}>
                  취소
                </button>
                <button type="submit" className="btn btn--primary" disabled={issuing}>
                  발급
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
