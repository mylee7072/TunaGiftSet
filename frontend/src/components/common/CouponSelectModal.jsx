import { useEffect, useRef } from "react";
import { useDismissibleOverlay } from "../../hooks/useDismissibleOverlay";
import { formatCouponCondition, formatCouponDiscount, formatCouponUnavailableReason, formatDateTime, formatPrice } from "../../utils/format";

export function CouponSelectModal({ open, coupons, selectedMemberCouponId, onSelect, onClose }) {
  const closeButtonRef = useRef(null);
  const modalRef = useRef(null);
  const { shouldRender, closing } = useDismissibleOverlay(open, onClose, modalRef);

  useEffect(() => {
    if (open) {
      closeButtonRef.current?.focus();
    }
  }, [open]);

  if (!shouldRender) return null;

  return (
    <div className={`modal-overlay${closing ? " modal-overlay--closing" : ""}`} role="presentation" onClick={onClose}>
      <div
        ref={modalRef}
        className={`modal coupon-select-modal${closing ? " modal--closing" : ""}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="coupon-select-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="coupon-select-title">쿠폰 선택</h2>

        <ul className="coupon-select-modal__list">
          <li>
            <label className={`coupon-select-modal__option${selectedMemberCouponId === null ? " is-selected" : ""}`}>
              <input type="radio" name="memberCoupon" checked={selectedMemberCouponId === null} onChange={() => onSelect(null)} />
              <span className="coupon-select-modal__option-body">
                <strong>쿠폰 사용 안 함</strong>
              </span>
            </label>
          </li>
          {coupons.length === 0 && <li className="coupon-select-modal__empty">사용 가능한 쿠폰이 없습니다.</li>}
          {coupons.map((coupon) => (
            <li key={coupon.memberCouponId}>
              <label className={`coupon-select-modal__option${!coupon.usable ? " is-disabled" : ""}${selectedMemberCouponId === coupon.memberCouponId ? " is-selected" : ""}`}>
                <input
                  type="radio"
                  name="memberCoupon"
                  disabled={!coupon.usable}
                  checked={selectedMemberCouponId === coupon.memberCouponId}
                  onChange={() => onSelect(coupon.memberCouponId)}
                />
                <span className="coupon-select-modal__option-body">
                  <strong>{coupon.name}</strong>
                  <span>{formatCouponDiscount(coupon)}</span>
                  <span>{formatCouponCondition(coupon)}</span>
                  <span className="muted">
                    {formatDateTime(coupon.validFrom)} ~ {formatDateTime(coupon.validUntil)}
                  </span>
                  {coupon.usable ? (
                    <span className="coupon-select-modal__expected">예상 할인 {formatPrice(coupon.expectedDiscountAmount)}</span>
                  ) : (
                    <span className="coupon-select-modal__unusable" role="note">
                      {formatCouponUnavailableReason(coupon.unavailableReason)}
                    </span>
                  )}
                </span>
              </label>
            </li>
          ))}
        </ul>

        <div className="modal__actions">
          <button type="button" className="btn btn--primary" ref={closeButtonRef} onClick={onClose}>
            닫기
          </button>
        </div>
      </div>
    </div>
  );
}
