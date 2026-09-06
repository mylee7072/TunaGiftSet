const priceFormatter = new Intl.NumberFormat("ko-KR");

export function formatPrice(value) {
  const number = Number(value ?? 0);
  return `${priceFormatter.format(Number.isFinite(number) ? number : 0)}원`;
}

const dateTimeFormatter = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

export function formatDateTime(isoString) {
  if (!isoString) return "-";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "-";
  return dateTimeFormatter.format(date).replace(/\. /g, ". ").replace(/,/g, "");
}

export const ORDER_STATUS_LABEL = {
  PAYMENT_PENDING: "결제대기",
  PAID: "결제완료",
  PREPARING: "상품 준비중",
  SHIPPING: "배송중",
  DELIVERED: "배송완료",
  CANCELED: "주문취소",
  EXPIRED: "결제시간 초과",
};

export function formatOrderStatus(status) {
  return ORDER_STATUS_LABEL[status] || status || "-";
}

export const PAYMENT_STATUS_LABEL = {
  READY: "결제대기",
  IN_PROGRESS: "결제 처리중",
  PAID: "결제완료",
  CANCELED: "결제취소",
  FAILED: "결제실패",
};

export function formatPaymentStatus(status) {
  return PAYMENT_STATUS_LABEL[status] || status || "-";
}

export const PAYMENT_METHOD_LABEL = {
  CARD: "카드",
  BANK_TRANSFER: "계좌이체",
  VIRTUAL_ACCOUNT: "가상계좌",
  EASY_PAY: "간편결제",
  UNKNOWN: "-",
};

export function formatPaymentMethod(method) {
  return PAYMENT_METHOD_LABEL[method] || method || "-";
}

export const DELIVERY_STATUS_LABEL = {
  READY: "배송 준비중",
  PREPARING: "상품 준비중",
  SHIPPING: "배송중",
  DELIVERED: "배송완료",
  RETURNED: "반송",
  CANCELED: "배송취소",
};

export function formatDeliveryStatus(status) {
  return DELIVERY_STATUS_LABEL[status] || status || "-";
}

export function isCancelableOrderStatus(status) {
  return status === "PAYMENT_PENDING" || status === "PAID";
}

export function formatPhone(value) {
  const digits = String(value || "").replace(/\D/g, "");
  if (digits.length === 11) {
    return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
  }
  if (digits.length === 10) {
    return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`;
  }
  return value || "-";
}

export function formatCouponDiscount(coupon) {
  if (!coupon) return "-";
  if (coupon.discountType === "PERCENTAGE") {
    const cap = coupon.maximumDiscountAmount ? ` (최대 ${formatPrice(coupon.maximumDiscountAmount)})` : "";
    return `${Number(coupon.discountValue)}% 할인${cap}`;
  }
  return `${formatPrice(coupon.discountValue)} 할인`;
}

export function formatCouponCondition(coupon) {
  if (!coupon) return "";
  const minimum = Number(coupon.minimumOrderAmount ?? 0);
  return minimum > 0 ? `${formatPrice(minimum)} 이상 구매 시 사용 가능` : "최소 주문금액 제한 없음";
}

export const COUPON_UNAVAILABLE_REASON_LABEL = {
  COUPON_NOT_STARTED: "아직 사용 기간이 아닙니다.",
  COUPON_EXPIRED: "유효기간이 지난 쿠폰입니다.",
  COUPON_MINIMUM_ORDER_NOT_MET: "최소 주문금액을 충족하지 않았습니다.",
  COUPON_ALREADY_RESERVED: "다른 주문에 사용 중인 쿠폰입니다.",
  COUPON_ALREADY_USED: "이미 사용한 쿠폰입니다.",
  COUPON_NOT_AVAILABLE: "사용할 수 없는 쿠폰입니다.",
};

export function formatCouponUnavailableReason(reason) {
  return COUPON_UNAVAILABLE_REASON_LABEL[reason] || "사용할 수 없는 쿠폰입니다.";
}

export function classifyMemberCoupon(memberCoupon) {
  if (memberCoupon.status === "USED") return "USED";
  if (memberCoupon.status === "RESERVED") return "RESERVED";
  const validUntil = memberCoupon.validUntil ? new Date(memberCoupon.validUntil) : null;
  if (validUntil && validUntil.getTime() < Date.now()) return "EXPIRED";
  return "AVAILABLE";
}

export function formatAddress(address) {
  if (!address) return "";
  return [
    address.zipCode ? `(${address.zipCode})` : "",
    address.roadAddress || address.address1 || "",
    address.detailAddress || address.address2 || "",
    address.extraAddress || "",
  ]
    .filter(Boolean)
    .join(" ");
}
