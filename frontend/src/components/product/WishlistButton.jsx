import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { ApiError } from "../../api/apiClient";
import { wishlistApi } from "../../api/wishlistApi";
import { useAuth } from "../../context/useAuth";
import { useToast } from "../../context/useToast";

export function WishlistButton({
  productId,
  initialWishlisted = false,
  initialCount = 0,
  className = "",
  onChange,
  showText = false,
}) {
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [wishlisted, setWishlisted] = useState(initialWishlisted);
  const [wishlistCount, setWishlistCount] = useState(Number(initialCount || 0));
  const [submitting, setSubmitting] = useState(false);
  const [pulse, setPulse] = useState(false);

  useEffect(() => {
    setWishlisted(Boolean(initialWishlisted));
  }, [initialWishlisted, productId]);

  useEffect(() => {
    setWishlistCount(Number(initialCount || 0));
  }, [initialCount, productId]);

  async function handleClick(event) {
    event.preventDefault();
    event.stopPropagation();

    if (!isAuthenticated) {
      showToast("로그인이 필요한 기능입니다.", "info");
      navigate("/login", { state: { from: location } });
      return;
    }

    if (submitting) return;

    const previous = wishlisted;
    setSubmitting(true);
    try {
      const response = previous
        ? await wishlistApi.remove(productId)
        : await wishlistApi.add(productId);
      setWishlisted(response.wishlisted);
      setWishlistCount(response.wishlistCount);
      onChange?.(response);
      showToast(response.wishlisted ? "찜한 상품에 추가했습니다." : "찜한 상품에서 삭제했습니다.");
      setPulse(false);
      requestAnimationFrame(() => setPulse(true));
    } catch (error) {
      const message = error instanceof ApiError ? error.message : "찜 처리 중 오류가 발생했습니다.";
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <button
      type="button"
      className={`wishlist-button${wishlisted ? " wishlist-button--active" : ""}${pulse ? " wishlist-button--pulse" : ""}${showText ? " wishlist-button--with-text" : ""}${className ? ` ${className}` : ""}`}
      onClick={handleClick}
      onAnimationEnd={() => setPulse(false)}
      disabled={submitting}
      aria-label={wishlisted ? "찜 취소" : "찜하기"}
      aria-pressed={wishlisted}
    >
      <span aria-hidden="true" className="wishlist-button__icon">{wishlisted ? "♥" : "♡"}</span>
      {showText && <span className="wishlist-button__label">{wishlisted ? "찜 취소" : "찜하기"}</span>}
      {wishlistCount > 0 && <span className="wishlist-button__count">{wishlistCount}</span>}
    </button>
  );
}
