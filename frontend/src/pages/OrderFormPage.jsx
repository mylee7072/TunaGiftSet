import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { addressApi } from "../api/addressApi";
import { orderApi } from "../api/orderApi";
import { couponApi } from "../api/couponApi";
import { requestPaymentWindow } from "../api/tossPayments";
import { AddressForm } from "../components/address/AddressForm";
import { CouponSelectModal } from "../components/common/CouponSelectModal";
import { EmptyState } from "../components/common/EmptyState";
import { Loading } from "../components/common/Loading";
import { useAuth } from "../context/useAuth";
import { useToast } from "../context/useToast";
import { ApiError } from "../api/apiClient";
import { formatAddress, formatCouponDiscount, formatPhone, formatPrice } from "../utils/format";

const DELIVERY_MESSAGES = ["", "배송 전 연락 부탁드립니다.", "문 앞에 놓아주세요.", "부재 시 경비실에 맡겨주세요.", "직접 입력"];

export function OrderFormPage() {
  const location = useLocation();
  const { member } = useAuth();
  const { showToast } = useToast();

  const selectedItems = location.state?.selectedItems;

  const [addresses, setAddresses] = useState([]);
  const [addressStatus, setAddressStatus] = useState("loading");
  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [useNewAddress, setUseNewAddress] = useState(false);
  const [newAddress, setNewAddress] = useState(null);
  const [messagePreset, setMessagePreset] = useState("");
  const [customMessage, setCustomMessage] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const cartItemIds = useMemo(() => (selectedItems || []).map((item) => item.cartItemId), [selectedItems]);
  const [selectedMemberCouponId, setSelectedMemberCouponId] = useState(null);
  const [preview, setPreview] = useState(null);
  const [previewStatus, setPreviewStatus] = useState("loading");
  const [couponModalOpen, setCouponModalOpen] = useState(false);

  const loadPreview = useCallback(
    (memberCouponId) => {
      if (cartItemIds.length === 0) return;
      setPreviewStatus("loading");
      couponApi
        .preview({ cartItemIds, memberCouponId })
        .then((data) => {
          setPreview(data);
          setPreviewStatus("ready");
        })
        .catch((previewError) => {
          if (memberCouponId !== null) {
            setSelectedMemberCouponId(null);
            showToast(
              previewError instanceof ApiError ? previewError.message : "선택한 쿠폰을 사용할 수 없어 해제했습니다.",
              "error"
            );
            return;
          }
          setPreviewStatus("error");
        });
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [cartItemIds.join(",")]
  );

  useEffect(() => {
    document.title = "주문서 - TunaGiftSet";
    loadPreview(selectedMemberCouponId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cartItemIds.join(","), selectedMemberCouponId]);

  const loadAddresses = useCallback(() => {
    setAddressStatus("loading");
    return addressApi
      .findAddresses()
      .then((data) => {
        setAddresses(data);
        const defaultAddress = data.find((address) => address.defaultAddress) || data[0];
        setSelectedAddressId((current) => current || defaultAddress?.id || null);
        setUseNewAddress(data.length === 0);
        setAddressStatus("ready");
      })
      .catch(() => {
        setAddressStatus("error");
        setUseNewAddress(true);
      });
  }, []);

  useEffect(() => {
    loadAddresses();
  }, [loadAddresses]);

  const selectedAddress = useMemo(
    () => addresses.find((address) => address.id === Number(selectedAddressId)) || null,
    [addresses, selectedAddressId]
  );

  if (!selectedItems || selectedItems.length === 0) {
    return (
      <div className="container section">
        <h1 className="page-title">주문서</h1>
        <EmptyState
          message="주문할 상품 정보를 찾을 수 없습니다. 장바구니에서 다시 선택해 주세요."
          action={<Link to="/cart" className="btn btn--primary">장바구니로 이동</Link>}
        />
      </div>
    );
  }

  const deliveryMessage = messagePreset === "직접 입력" ? customMessage.trim() : messagePreset;
  const selectedCoupon = preview?.coupons?.find((coupon) => coupon.memberCouponId === selectedMemberCouponId) || null;
  const usableCouponCount = preview?.coupons?.filter((coupon) => coupon.usable).length || 0;

  function handleNewAddressSubmit(payload) {
    setNewAddress(payload);
    setUseNewAddress(true);
    setError("");
    return Promise.resolve();
  }

  function validate() {
    if (!useNewAddress && !selectedAddress) return "배송지를 선택해 주세요.";
    if (useNewAddress && !newAddress) return "신규 배송지를 입력해 주세요.";
    if (deliveryMessage.length > 100) return "배송메시지는 100자 이하로 입력해 주세요.";
    return "";
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting) return;

    const validationMessage = validate();
    if (validationMessage) {
      setError(validationMessage);
      return;
    }

    setError("");
    setSubmitting(true);
    try {
      const payload = {
        cartItemIds: selectedItems.map((item) => item.cartItemId),
        memberCouponId: selectedMemberCouponId,
        deliveryMessage,
      };

      if (useNewAddress) {
        Object.assign(payload, newAddress);
      } else {
        payload.addressId = selectedAddress.id;
      }

      const order = await orderApi.createOrder(payload);

      await requestPaymentWindow({
        memberId: member.id,
        orderNumber: order.orderNumber,
        amount: Number(order.totalAmount),
        orderName: order.orderName,
        customerName: order.recipientName,
        customerEmail: member.email,
        customerMobilePhone: member.phone?.replace(/\D/g, ""),
      });
    } catch (submitError) {
      setSubmitting(false);
      if (submitError instanceof ApiError) {
        setError(submitError.message);
      } else {
        setError("결제창을 여는 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.");
      }
    }
  }

  return (
    <div className="container section order-form-page">
      <div className="checkout-progress" aria-label="주문 진행 단계">
        <span>장바구니</span>
        <strong>주문/결제</strong>
        <span>주문완료</span>
      </div>

      <div className="order-form-page__header">
        <p className="breadcrumb">홈 / 장바구니 / 주문서</p>
        <h1 className="page-title">주문서</h1>
        <p>배송지와 쿠폰을 확인한 뒤 결제를 진행해 주세요.</p>
      </div>

      <section className="order-form-page__items">
        <h2>주문 상품</h2>
        <ul>
          {selectedItems.map((item) => (
            <li key={item.cartItemId} className="order-form-page__item">
              <span>{item.productName}</span>
              <span>{item.quantity}개</span>
              <span>{formatPrice(item.itemAmount)}</span>
            </li>
          ))}
        </ul>
      </section>

      <form className="order-form" onSubmit={handleSubmit} noValidate>
        <section className="order-form__section">
          <div className="section-header">
            <h2>배송지</h2>
            <Link to="/mypage/addresses" className="btn btn--secondary">배송지 관리</Link>
          </div>

          {addressStatus === "loading" && <Loading />}
          {addressStatus === "error" && (
            <p className="form-error" role="alert">
              저장된 배송지를 불러오지 못했습니다. 신규 배송지로 주문할 수 있습니다.
            </p>
          )}

          {addressStatus === "ready" && addresses.length > 0 && (
            <div className="order-address-options">
              {addresses.map((address) => (
                <label key={address.id} className={`order-address-option${selectedAddressId === address.id && !useNewAddress ? " is-selected" : ""}`}>
                  <input
                    type="radio"
                    name="selectedAddressId"
                    checked={selectedAddressId === address.id && !useNewAddress}
                    onChange={() => {
                      setSelectedAddressId(address.id);
                      setUseNewAddress(false);
                    }}
                  />
                  <span>
                    <strong>{address.addressName || "배송지"}</strong>
                    {address.defaultAddress && <em>기본배송지</em>}
                    <span>{address.recipientName} / {formatPhone(address.recipientPhone)}</span>
                    <span>{formatAddress(address)}</span>
                  </span>
                </label>
              ))}
              <label className={`order-address-option${useNewAddress ? " is-selected" : ""}`}>
                <input type="radio" name="selectedAddressId" checked={useNewAddress} onChange={() => setUseNewAddress(true)} />
                <span>
                  <strong>신규 배송지</strong>
                  <span>이번 주문에 사용할 배송지를 직접 입력합니다.</span>
                </span>
              </label>
            </div>
          )}

          {useNewAddress && (
            <div className="order-new-address">
              {newAddress && (
                <div className="order-new-address__preview">
                  <strong>{newAddress.recipientName}</strong>
                  <span>{formatPhone(newAddress.recipientPhone)}</span>
                  <span>{formatAddress(newAddress)}</span>
                </div>
              )}
              <AddressForm initialValue={newAddress || undefined} submitLabel={newAddress ? "신규 배송지 수정" : "신규 배송지 적용"} onSubmit={handleNewAddressSubmit} />
              <p className="form-hint">신규 배송지는 이번 주문에만 사용됩니다. 저장하려면 마이페이지 배송지 관리에서 등록해 주세요.</p>
            </div>
          )}
        </section>

        <section className="order-form__section">
          <h2>배송메시지</h2>
          <label htmlFor="deliveryMessagePreset">배송메시지 선택</label>
          <select id="deliveryMessagePreset" value={messagePreset} onChange={(event) => setMessagePreset(event.target.value)}>
            {DELIVERY_MESSAGES.map((message) => (
              <option key={message || "empty"} value={message}>
                {message || "선택 안 함"}
              </option>
            ))}
          </select>
          {messagePreset === "직접 입력" && (
            <input
              aria-label="배송메시지 직접 입력"
              value={customMessage}
              maxLength={100}
              onChange={(event) => setCustomMessage(event.target.value)}
              placeholder="배송메시지를 입력해 주세요."
            />
          )}
        </section>

        <section className="order-form__section">
          <div className="section-header">
            <div>
              <h2>쿠폰 / 할인</h2>
              <p className="section__description">사용 가능 쿠폰 {usableCouponCount}장</p>
            </div>
            <button type="button" className="btn btn--secondary" onClick={() => setCouponModalOpen(true)} disabled={previewStatus === "loading" && !preview}>
              쿠폰 선택
            </button>
          </div>
          {selectedCoupon ? (
            <div className="order-form-page__coupon-selected">
              <p><strong>{selectedCoupon.name}</strong> 적용 중 ({formatCouponDiscount(selectedCoupon)})</p>
              <button type="button" className="link-button" onClick={() => setSelectedMemberCouponId(null)}>적용 취소</button>
            </div>
          ) : (
            <p className="muted">적용한 쿠폰이 없습니다.</p>
          )}
        </section>

        <section className="order-summary">
          <h2>최종 결제금액</h2>
          {previewStatus === "loading" && <Loading />}
          {previewStatus === "error" && (
            <p className="form-error" role="alert">
              결제금액을 계산하지 못했습니다. 잠시 후 다시 시도해 주세요.
            </p>
          )}
          {previewStatus === "ready" && preview && (
            <>
              <div className="order-summary__row">
                <span>상품금액</span>
                <span>{formatPrice(preview.productAmount)}</span>
              </div>
              {Number(preview.promotionDiscountAmount) > 0 && (
                <div className="order-summary__row">
                  <span>프로모션 할인</span>
                  <span>-{formatPrice(preview.promotionDiscountAmount)}</span>
                </div>
              )}
              {Number(preview.couponDiscountAmount) > 0 && (
                <div className="order-summary__row">
                  <span>쿠폰 할인</span>
                  <span>-{formatPrice(preview.couponDiscountAmount)}</span>
                </div>
              )}
              <div className="order-summary__row">
                <span>배송비</span>
                <span>{formatPrice(preview.shippingFee)}</span>
              </div>
              <div className="order-summary__row order-summary__row--total">
                <span>결제금액</span>
                <span>{formatPrice(preview.totalAmount)}</span>
              </div>
            </>
          )}
        </section>

        {error && <p className="form-error" role="alert">{error}</p>}

        <button type="submit" className="btn btn--primary btn--block" disabled={submitting || previewStatus !== "ready"}>
          {submitting && <span className="btn__spinner" aria-hidden="true" />}
          {submitting ? "결제창을 여는 중..." : "결제하기"}
        </button>
      </form>

      <CouponSelectModal
        open={couponModalOpen}
        coupons={preview?.coupons || []}
        selectedMemberCouponId={selectedMemberCouponId}
        onSelect={(memberCouponId) => {
          setSelectedMemberCouponId(memberCouponId);
          setCouponModalOpen(false);
        }}
        onClose={() => setCouponModalOpen(false)}
      />
    </div>
  );
}
