import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { orderApi } from "../api/orderApi";
import { Loading } from "../components/common/Loading";
import { ErrorState } from "../components/common/ErrorState";
import { formatDateTime, formatOrderStatus, formatPaymentMethod, formatPrice } from "../utils/format";

export function OrderCompletePage() {
  const { orderNumber } = useParams();
  const [order, setOrder] = useState(null);
  const [status, setStatus] = useState("loading");

  useEffect(() => {
    document.title = "주문완료 - TunaGiftSet";
    orderApi
      .findOrder(orderNumber)
      .then((data) => {
        setOrder(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, [orderNumber]);

  if (status === "loading") return <Loading label="주문 정보를 불러오는 중입니다..." />;
  if (status === "error" || !order) {
    return (
      <div className="container section">
        <ErrorState message="주문 정보를 불러오지 못했습니다." />
      </div>
    );
  }

  return (
    <div className="container section payment-result">
      <div className="checkout-progress checkout-progress--complete" aria-label="주문 진행 단계">
        <span>장바구니</span>
        <span>주문/결제</span>
        <strong>주문완료</strong>
      </div>
      <div className="payment-result__icon payment-result__icon--success" aria-hidden="true">
        <svg viewBox="0 0 52 52" width="52" height="52">
          <circle cx="26" cy="26" r="24" fill="none" strokeWidth="3" />
          <path fill="none" strokeWidth="4" d="M14 27l8 8 16-17" />
        </svg>
      </div>
      <h1 className="page-title">주문이 완료되었습니다</h1>
      <p className="payment-result__message">주문번호와 결제 정보를 확인해 주세요.</p>

      <dl className="order-complete-summary">
        <div>
          <dt>주문번호</dt>
          <dd>{order.orderNumber}</dd>
        </div>
        <div>
          <dt>주문일시</dt>
          <dd>{formatDateTime(order.orderedAt)}</dd>
        </div>
        <div>
          <dt>결제금액</dt>
          <dd>{formatPrice(order.totalAmount)}</dd>
        </div>
        <div>
          <dt>결제수단</dt>
          <dd>{formatPaymentMethod(order.payment?.paymentMethod)}</dd>
        </div>
        <div>
          <dt>주문상태</dt>
          <dd>{formatOrderStatus(order.orderStatus)}</dd>
        </div>
      </dl>

      <div className="payment-result__actions">
        <Link to="/mypage/orders" className="btn btn--secondary">
          주문내역 보기
        </Link>
        <Link to="/products" className="btn btn--primary">
          쇼핑 계속하기
        </Link>
      </div>
    </div>
  );
}
