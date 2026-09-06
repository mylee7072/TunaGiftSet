import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { adminOrderApi } from "../../api/adminApi";
import { Loading } from "../../components/common/Loading";
import { ErrorState } from "../../components/common/ErrorState";
import { ConfirmDialog } from "../../components/common/ConfirmDialog";
import { useToast } from "../../context/useToast";
import { ApiError } from "../../api/apiClient";
import {
  formatAddress,
  formatDateTime,
  formatDeliveryStatus,
  formatOrderStatus,
  formatPaymentMethod,
  formatPaymentStatus,
  formatPhone,
  formatPrice,
} from "../../utils/format";

const ADMIN_CANCELABLE_STATUSES = ["PAYMENT_PENDING", "PAID", "PREPARING"];

export function AdminOrderDetailPage() {
  const { orderNumber } = useParams();
  const { showToast } = useToast();

  const [order, setOrder] = useState(null);
  const [status, setStatus] = useState("loading");
  const [actionPending, setActionPending] = useState(false);
  const [deliveryForm, setDeliveryForm] = useState({ carrier: "", trackingNumber: "" });
  const [cancelReason, setCancelReason] = useState("");
  const [confirmCancelOpen, setConfirmCancelOpen] = useState(false);

  const load = useCallback(() => {
    setStatus("loading");
    return adminOrderApi
      .findOrder(orderNumber)
      .then((data) => {
        setOrder(data);
        setDeliveryForm({ carrier: data.carrier || "", trackingNumber: data.trackingNumber || "" });
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, [orderNumber]);

  useEffect(() => {
    load();
  }, [load]);

  async function runAction(action, successMessage) {
    if (actionPending) return;
    setActionPending(true);
    try {
      await action();
      showToast(successMessage);
      await load();
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : "처리하지 못했습니다.", "error");
    } finally {
      setActionPending(false);
    }
  }

  async function handleRegisterDelivery(event) {
    event.preventDefault();
    if (!deliveryForm.carrier.trim() || !deliveryForm.trackingNumber.trim()) {
      showToast("택배사와 송장번호를 입력해 주세요.", "error");
      return;
    }
    await runAction(
      () => adminOrderApi.registerDelivery(orderNumber, { carrier: deliveryForm.carrier.trim(), trackingNumber: deliveryForm.trackingNumber.trim() }),
      "배송정보를 등록했습니다."
    );
  }

  async function handleCancelConfirmed() {
    setConfirmCancelOpen(false);
    await runAction(() => adminOrderApi.cancel(orderNumber, cancelReason.trim() || undefined), "주문을 취소했습니다.");
    setCancelReason("");
  }

  if (status === "loading") return <Loading />;
  if (status === "error" || !order) {
    return (
      <div className="admin-page">
        <ErrorState message="주문 정보를 불러오지 못했습니다." onRetry={load} />
      </div>
    );
  }

  const canPrepare = order.orderStatus === "PAID";
  const canRegisterDelivery = order.orderStatus === "PREPARING";
  const canShip = order.orderStatus === "PREPARING" && Boolean(order.trackingNumber);
  const canDeliver = order.orderStatus === "SHIPPING";
  const canCancel = ADMIN_CANCELABLE_STATUSES.includes(order.orderStatus);

  return (
    <div className="admin-page">
      <div className="admin-page__header">
        <h1 className="page-title">주문 상세 — {order.orderNumber}</h1>
        <span className="admin-badge admin-badge--active">{formatOrderStatus(order.orderStatus)}</span>
      </div>
      <p className="muted">{formatDateTime(order.orderedAt)}</p>

      <section className="admin-detail-section">
        <h2>주문자</h2>
        <dl className="admin-detail-list">
          <div>
            <dt>이름</dt>
            <dd>{order.member.name}</dd>
          </div>
          <div>
            <dt>이메일</dt>
            <dd>{order.member.email}</dd>
          </div>
          <div>
            <dt>연락처</dt>
            <dd>{formatPhone(order.member.phone)}</dd>
          </div>
        </dl>
      </section>

      <section className="admin-detail-section">
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

      <section className="admin-detail-section">
        <h2>배송지</h2>
        <dl className="admin-detail-list">
          <div>
            <dt>받는 분</dt>
            <dd>
              {order.recipientName} / {formatPhone(order.recipientPhone)}
            </dd>
          </div>
          <div>
            <dt>주소</dt>
            <dd>{formatAddress({ zipCode: order.zipCode, roadAddress: order.roadAddress, detailAddress: order.detailAddress, extraAddress: order.extraAddress })}</dd>
          </div>
          {order.deliveryMessage && (
            <div>
              <dt>배송 메시지</dt>
              <dd>{order.deliveryMessage}</dd>
            </div>
          )}
        </dl>
      </section>

      <section className="admin-detail-section">
        <h2>결제/할인</h2>
        <dl className="admin-detail-list">
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
              <dt>쿠폰 할인{order.couponNameSnapshot ? ` (${order.couponNameSnapshot} / ${order.couponCodeSnapshot})` : ""}</dt>
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
          <div>
            <dt>결제상태</dt>
            <dd>{formatPaymentStatus(order.paymentStatus)}</dd>
          </div>
          <div>
            <dt>결제수단</dt>
            <dd>{formatPaymentMethod(order.paymentMethod)}</dd>
          </div>
        </dl>
      </section>

      <section className="admin-detail-section">
        <h2>배송 처리</h2>
        <p className="muted">배송상태: {formatDeliveryStatus(order.deliveryStatus)}</p>

        <div className="admin-detail-actions">
          {canPrepare && (
            <button type="button" className="btn btn--primary" disabled={actionPending} onClick={() => runAction(() => adminOrderApi.prepare(orderNumber), "상품 준비를 시작했습니다.")}>
              상품 준비 시작
            </button>
          )}
          {canShip && (
            <button type="button" className="btn btn--primary" disabled={actionPending} onClick={() => runAction(() => adminOrderApi.ship(orderNumber), "배송을 시작했습니다.")}>
              배송 시작
            </button>
          )}
          {canDeliver && (
            <button type="button" className="btn btn--primary" disabled={actionPending} onClick={() => runAction(() => adminOrderApi.deliver(orderNumber), "배송완료로 처리했습니다.")}>
              배송완료 처리
            </button>
          )}
        </div>

        {canRegisterDelivery && (
          <form className="admin-form admin-form--inline" onSubmit={handleRegisterDelivery}>
            <label htmlFor="delivery-carrier">택배사</label>
            <input
              id="delivery-carrier"
              value={deliveryForm.carrier}
              onChange={(event) => setDeliveryForm({ ...deliveryForm, carrier: event.target.value })}
              maxLength={100}
            />
            <label htmlFor="delivery-tracking">송장번호</label>
            <input
              id="delivery-tracking"
              value={deliveryForm.trackingNumber}
              onChange={(event) => setDeliveryForm({ ...deliveryForm, trackingNumber: event.target.value })}
              maxLength={100}
            />
            <button type="submit" className="btn btn--secondary" disabled={actionPending}>
              배송정보 저장
            </button>
          </form>
        )}

        {!canRegisterDelivery && order.trackingNumber && (
          <p>
            {order.carrier} / {order.trackingNumber}
          </p>
        )}
      </section>

      {canCancel && (
        <section className="admin-detail-section">
          <h2>주문 취소</h2>
          <input
            placeholder="취소 사유 (선택)"
            value={cancelReason}
            onChange={(event) => setCancelReason(event.target.value)}
            maxLength={200}
          />
          <button type="button" className="btn btn--danger" disabled={actionPending} onClick={() => setConfirmCancelOpen(true)}>
            주문 취소
          </button>
        </section>
      )}

      <ConfirmDialog
        open={confirmCancelOpen}
        title="주문을 취소하시겠습니까?"
        description="결제가 완료된 주문은 결제 취소까지 함께 진행됩니다."
        confirmLabel="주문 취소"
        onConfirm={handleCancelConfirmed}
        onCancel={() => setConfirmCancelOpen(false)}
      />
    </div>
  );
}
