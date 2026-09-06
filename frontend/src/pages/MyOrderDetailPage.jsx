import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { orderApi } from "../api/orderApi";
import { useToast } from "../context/useToast";
import { Loading } from "../components/common/Loading";
import { ErrorState } from "../components/common/ErrorState";
import { ConfirmDialog } from "../components/common/ConfirmDialog";
import {
  formatDateTime,
  formatDeliveryStatus,
  formatAddress,
  formatOrderStatus,
  formatPaymentMethod,
  formatPaymentStatus,
  formatPrice,
  isCancelableOrderStatus,
} from "../utils/format";
import { ApiError } from "../api/apiClient";

export function MyOrderDetailPage() {
  const { orderNumber } = useParams();
  const { showToast } = useToast();

  const [order, setOrder] = useState(null);
  const [status, setStatus] = useState("loading");
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [canceling, setCanceling] = useState(false);

  const loadOrder = useCallback(() => {
    setStatus("loading");
    return orderApi
      .findOrder(orderNumber)
      .then((data) => {
        setOrder(data);
        setStatus("ready");
        document.title = `주문 상세 - ${data.orderNumber}`;
      })
      .catch(() => setStatus("error"));
  }, [orderNumber]);

  useEffect(() => {
    loadOrder();
  }, [loadOrder]);

  async function handleCancelConfirmed() {
    setConfirmOpen(false);
    setCanceling(true);
    try {
      await orderApi.cancelOrder(orderNumber);
      showToast("주문이 취소되었습니다.");
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : "주문 취소에 실패했습니다.", "error");
    } finally {
      setCanceling(false);
      loadOrder();
    }
  }

  if (status === "loading") return <Loading />;
  if (status === "error" || !order) {
    return (
      <div className="container section">
        <ErrorState message="주문 정보를 불러오지 못했습니다." onRetry={loadOrder} />
      </div>
    );
  }

  const shippingAddress = {
    zipCode: order.zipCode,
    roadAddress: order.roadAddress || order.address1,
    detailAddress: order.detailAddress || order.address2,
    extraAddress: order.extraAddress,
  };

  return (
    <div className="container section order-detail-page">
      <div className="page-header">
        <div>
          <p className="breadcrumb">홈 / 마이페이지 / 주문내역 / 상세</p>
          <h1 className="page-title">주문 상세</h1>
          <p>주문번호 {order.orderNumber}</p>
        </div>
        <span className="order-list__status">{formatOrderStatus(order.orderStatus)}</span>
      </div>

      <section className="order-detail-page__section">
        <div className="order-detail-page__header">
          <span>주문번호 {order.orderNumber}</span>
          <span>{formatDateTime(order.orderedAt)}</span>
        </div>
      </section>

      <section className="order-timeline" aria-label="주문 진행 상태">
        {["PAYMENT_PENDING", "PAID", "PREPARING", "SHIPPING", "DELIVERED"].map((statusKey) => (
          <span key={statusKey} className={order.orderStatus === statusKey ? "is-current" : ""}>
            {formatOrderStatus(statusKey)}
          </span>
        ))}
      </section>

      <section className="order-detail-page__section">
        <h2>주문 상품</h2>
        <ul className="order-detail-page__items">
          {order.items.map((item) => (
            <li key={item.orderItemId}>
              <span>{item.productName}</span>
              <span>{item.quantity}개</span>
              <span>{formatPrice(item.totalPrice)}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="order-detail-page__section">
        <h2>배송 정보</h2>
        <dl>
          <div>
            <dt>받는 분</dt>
            <dd>{order.recipientName}</dd>
          </div>
          <div>
            <dt>연락처</dt>
            <dd>{order.recipientPhone}</dd>
          </div>
          <div>
            <dt>주소</dt>
            <dd>{formatAddress(shippingAddress)}</dd>
          </div>
          {order.deliveryMessage && (
            <div>
              <dt>배송 메시지</dt>
              <dd>{order.deliveryMessage}</dd>
            </div>
          )}
          <div>
            <dt>배송 상태</dt>
            <dd>{formatDeliveryStatus(order.delivery?.status)}</dd>
          </div>
          {order.delivery?.trackingNumber && (
            <div>
              <dt>택배사 / 송장번호</dt>
              <dd>{order.delivery.carrier} / {order.delivery.trackingNumber}</dd>
            </div>
          )}
        </dl>
      </section>

      <section className="order-detail-page__section">
        <h2>결제 정보</h2>
        <dl>
          <div>
            <dt>결제상태</dt>
            <dd>{formatPaymentStatus(order.payment?.status)}</dd>
          </div>
          <div>
            <dt>결제수단</dt>
            <dd>{formatPaymentMethod(order.payment?.paymentMethod)}</dd>
          </div>
          <div>
            <dt>상품금액</dt>
            <dd>{formatPrice(order.productAmount)}</dd>
          </div>
          {Number(order.promotionDiscountAmount) > 0 && (
            <div>
              <dt>프로모션 할인</dt>
              <dd>-{formatPrice(order.promotionDiscountAmount)}</dd>
            </div>
          )}
          {Number(order.couponDiscountAmount) > 0 && (
            <div>
              <dt>쿠폰 할인{order.couponNameSnapshot ? ` (${order.couponNameSnapshot})` : ""}</dt>
              <dd>-{formatPrice(order.couponDiscountAmount)}</dd>
            </div>
          )}
          <div>
            <dt>배송비</dt>
            <dd>{formatPrice(order.shippingFee)}</dd>
          </div>
          <div>
            <dt>총 결제금액</dt>
            <dd>{formatPrice(order.totalAmount)}</dd>
          </div>
        </dl>
      </section>

      {isCancelableOrderStatus(order.orderStatus) && (
        <div className="order-detail-page__actions">
          <button type="button" className="btn btn--secondary" onClick={() => setConfirmOpen(true)} disabled={canceling}>
            주문 취소
          </button>
        </div>
      )}

      <ConfirmDialog
        open={confirmOpen}
        title="주문을 취소하시겠습니까?"
        description="결제가 완료된 주문은 결제 취소까지 함께 진행됩니다."
        confirmLabel="주문 취소"
        onConfirm={handleCancelConfirmed}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}
