import { Link, useLocation, useSearchParams } from "react-router-dom";

export function PaymentFailPage() {
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const message = location.state?.message || searchParams.get("message") || "결제를 완료하지 못했습니다.";
  const orderNumber = location.state?.orderNumber || searchParams.get("orderId");

  return (
    <div className="container section payment-result">
      <div className="payment-result__icon payment-result__icon--fail" aria-hidden="true">
        <svg viewBox="0 0 52 52" width="52" height="52">
          <circle cx="26" cy="26" r="24" fill="none" strokeWidth="3" />
          <path fill="none" strokeWidth="4" strokeLinecap="round" d="M18 18l16 16M34 18l-16 16" />
        </svg>
      </div>
      <h1 className="page-title">결제를 완료하지 못했습니다</h1>
      <p className="payment-result__message">{message}</p>

      <div className="payment-result__actions">
        {orderNumber ? (
          <Link to={`/mypage/orders/${orderNumber}`} className="btn btn--secondary">
            주문 상세 보기
          </Link>
        ) : (
          <Link to="/cart" className="btn btn--secondary">
            장바구니로 이동
          </Link>
        )}
        <Link to="/mypage/orders" className="btn btn--secondary">
          주문내역
        </Link>
        <Link to="/products" className="btn btn--primary">
          상품 다시 보기
        </Link>
      </div>
    </div>
  );
}
