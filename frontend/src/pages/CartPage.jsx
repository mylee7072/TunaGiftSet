import { useCallback, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { cartApi } from "../api/cartApi";
import { useToast } from "../context/useToast";
import { Loading } from "../components/common/Loading";
import { EmptyState } from "../components/common/EmptyState";
import { ErrorState } from "../components/common/ErrorState";
import { ConfirmDialog } from "../components/common/ConfirmDialog";
import { formatPrice } from "../utils/format";
import { ApiError } from "../api/apiClient";

const PLACEHOLDER_IMAGE = "/placeholder-product.svg";

export function CartPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { showToast } = useToast();

  const [cart, setCart] = useState(null);
  const [status, setStatus] = useState("loading");
  const [selectedIds, setSelectedIds] = useState(() => new Set());
  const [pendingDeleteId, setPendingDeleteId] = useState(null);
  const [removingIds, setRemovingIds] = useState(() => new Set());

  const loadCart = useCallback(
    (applyDefaultSelection) => {
      setStatus("loading");
      return cartApi
        .getCart()
        .then((data) => {
          setCart(data);
          setStatus("ready");
          if (applyDefaultSelection) applyDefaultSelection(data);
          return data;
        })
        .catch(() => setStatus("error"));
    },
    []
  );

  useEffect(() => {
    document.title = "장바구니 - TunaGiftSet";
    const preselectCartItemId = location.state?.preselectCartItemId;
    loadCart((data) => {
      if (preselectCartItemId) {
        setSelectedIds(new Set([preselectCartItemId]));
      } else {
        setSelectedIds(new Set(data.items.filter((item) => item.available).map((item) => item.cartItemId)));
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function toggleSelected(cartItemId) {
    setSelectedIds((current) => {
      const next = new Set(current);
      if (next.has(cartItemId)) next.delete(cartItemId);
      else next.add(cartItemId);
      return next;
    });
  }

  function toggleSelectAll() {
    if (!cart) return;
    const availableIds = cart.items.filter((item) => item.available).map((item) => item.cartItemId);
    const allSelected = availableIds.every((id) => selectedIds.has(id)) && availableIds.length > 0;
    setSelectedIds(allSelected ? new Set() : new Set(availableIds));
  }

  async function handleQuantityChange(cartItemId, nextQuantity) {
    if (nextQuantity < 1) return;
    try {
      const updated = await cartApi.updateQuantity(cartItemId, nextQuantity);
      setCart(updated);
    } catch (error) {
      showToast(describeQuantityError(error), "error");
      loadCart();
    }
  }

  async function handleDeleteConfirmed() {
    const cartItemId = pendingDeleteId;
    setPendingDeleteId(null);
    setRemovingIds((current) => new Set(current).add(cartItemId));
    try {
      await cartApi.deleteItem(cartItemId);
      showToast("장바구니에서 삭제했습니다.");
      setTimeout(() => {
        setSelectedIds((current) => {
          const next = new Set(current);
          next.delete(cartItemId);
          return next;
        });
        loadCart();
      }, 180);
    } catch {
      setRemovingIds((current) => {
        const next = new Set(current);
        next.delete(cartItemId);
        return next;
      });
      showToast("삭제하지 못했습니다. 잠시 후 다시 시도해 주세요.", "error");
    }
  }

  function handleOrder() {
    if (!cart) return;
    const selectedItems = cart.items.filter((item) => selectedIds.has(item.cartItemId));
    if (selectedItems.length === 0) {
      showToast("주문할 상품을 선택해 주세요.", "error");
      return;
    }
    navigate("/order", {
      state: {
        selectedItems,
        allAvailableCount: cart.items.filter((item) => item.available).length,
        cartSummary: cart.summary,
      },
    });
  }

  if (status === "loading" && !cart) return <Loading />;
  if (status === "error") {
    return (
      <div className="container section">
        <ErrorState message="장바구니를 불러오지 못했습니다." onRetry={() => loadCart()} />
      </div>
    );
  }
  if (!cart || cart.items.length === 0) {
    return (
      <div className="container section">
        <h1 className="page-title">장바구니</h1>
        <EmptyState
          message="장바구니가 비어 있습니다."
          action={
            <button type="button" className="btn btn--primary" onClick={() => navigate("/products")}>
              상품 둘러보기
            </button>
          }
        />
      </div>
    );
  }

  const availableItems = cart.items.filter((item) => item.available);
  const allSelected = availableItems.length > 0 && availableItems.every((item) => selectedIds.has(item.cartItemId));
  const selectedItems = cart.items.filter((item) => selectedIds.has(item.cartItemId));
  const selectedAmount = selectedItems.reduce((sum, item) => sum + Number(item.itemAmount), 0);
  const isFullSelection = selectedIds.size === availableItems.length && availableItems.length > 0;

  return (
    <div className="container section cart-page">
      <div className="cart-page__header">
        <p className="breadcrumb">홈 / 장바구니</p>
        <h1 className="page-title">장바구니</h1>
        <p className="cart-page__lead">구매할 선물세트를 확인하고 수량을 조정해 주세요.</p>
      </div>

      <div className="cart-page__select-all">
        <label>
          <input type="checkbox" checked={allSelected} onChange={toggleSelectAll} />
          전체 선택
        </label>
        <span>{selectedItems.length}개 선택</span>
      </div>

      <ul className="cart-list">
        {cart.items.map((item) => (
          <li
            key={item.cartItemId}
            className={`cart-item${item.available ? "" : " cart-item--unavailable"}${removingIds.has(item.cartItemId) ? " cart-item--removing" : ""}`}
          >
            <input
              type="checkbox"
              checked={selectedIds.has(item.cartItemId)}
              disabled={!item.available}
              onChange={() => toggleSelected(item.cartItemId)}
              aria-label={`${item.productName} 선택`}
            />
            <img
              src={item.thumbnailUrl || PLACEHOLDER_IMAGE}
              alt={item.productName}
              className="cart-item__image"
              onError={(event) => {
                event.currentTarget.onerror = null;
                event.currentTarget.src = PLACEHOLDER_IMAGE;
              }}
            />
            <div className="cart-item__info">
              <p className="cart-item__name">{item.productName}</p>
              <p className="cart-item__price">판매가 {formatPrice(item.salePrice)}</p>
              {!item.available && <p className="cart-item__unavailable-reason">{describeUnavailable(item.unavailableReason)}</p>}
            </div>
            <div className="quantity-selector quantity-selector--compact">
              <button type="button" onClick={() => handleQuantityChange(item.cartItemId, item.quantity - 1)} disabled={!item.available || item.quantity <= 1} aria-label="수량 감소">
                -
              </button>
              <input type="text" readOnly value={item.quantity} aria-label="수량" />
              <button type="button" onClick={() => handleQuantityChange(item.cartItemId, item.quantity + 1)} disabled={!item.available || item.quantity >= item.stockQuantity} aria-label="수량 증가">
                +
              </button>
            </div>
            <p className="cart-item__amount">{formatPrice(item.itemAmount)}</p>
            <button type="button" className="cart-item__delete" onClick={() => setPendingDeleteId(item.cartItemId)}>
              삭제
            </button>
          </li>
        ))}
      </ul>

      <aside className="cart-summary" aria-label="주문 예상 금액">
        <h2>결제 예상금액</h2>
        <div className="cart-summary__row">
          <span>선택 상품금액</span>
          <span>{formatPrice(selectedAmount)}</span>
        </div>
        {isFullSelection ? (
          <>
            <div className="cart-summary__row">
              <span>배송비</span>
              <span>{formatPrice(cart.summary.shippingFee)}</span>
            </div>
            <div className="cart-summary__row cart-summary__row--total">
              <span>총 주문금액</span>
              <span>{formatPrice(cart.summary.totalAmount)}</span>
            </div>
          </>
        ) : (
          <p className="cart-summary__note">배송비와 할인은 주문서에서 선택 상품 기준으로 다시 계산됩니다.</p>
        )}
        <button type="button" className="btn btn--primary btn--block" onClick={handleOrder}>
          선택 상품 주문하기
        </button>
      </aside>

      <ConfirmDialog
        open={pendingDeleteId !== null}
        title="상품을 삭제하시겠습니까?"
        confirmLabel="삭제"
        onConfirm={handleDeleteConfirmed}
        onCancel={() => setPendingDeleteId(null)}
      />
    </div>
  );
}

function describeUnavailable(reason) {
  switch (reason) {
    case "SOLD_OUT":
      return "품절된 상품입니다.";
    case "INSUFFICIENT_STOCK":
      return "재고가 부족합니다.";
    case "HIDDEN":
    case "DISCONTINUED":
      return "더 이상 구매할 수 없는 상품입니다.";
    default:
      return "구매할 수 없는 상품입니다.";
  }
}

function describeQuantityError(error) {
  if (error instanceof ApiError && error.code === "INSUFFICIENT_STOCK") {
    return "재고가 부족해 수량을 변경할 수 없습니다.";
  }
  return "수량을 변경하지 못했습니다.";
}
