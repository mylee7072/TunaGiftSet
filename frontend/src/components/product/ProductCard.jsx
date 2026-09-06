import { Link } from "react-router-dom";
import { WishlistButton } from "./WishlistButton";
import { formatPrice } from "../../utils/format";

const PLACEHOLDER_IMAGE = "/placeholder-product.svg";

export function ProductCard({
  product,
  enableWishlist = false,
  initialWishlisted = false,
  onWishlistChange,
}) {
  const isSoldOut = product.status === "SOLD_OUT" || product.soldOut || product.stockQuantity === 0;
  const isDiscontinued = product.status === "DISCONTINUED";
  const imageUrl = product.thumbnailImageUrl || product.images?.[0]?.imageUrl || PLACEHOLDER_IMAGE;
  const hasDiscount = product.originalPrice && product.salePrice && Number(product.originalPrice) > Number(product.salePrice);
  const discountRate = computeDiscountRate(product);
  const ratingCount = Number(product.reviewCount || 0);

  return (
    <Link to={`/products/${product.id}`} className="product-card">
      <div className="product-card__image-wrap">
        <img
          src={imageUrl}
          alt={product.name}
          className="product-card__image"
          loading="lazy"
          onError={(event) => {
            event.currentTarget.onerror = null;
            event.currentTarget.src = PLACEHOLDER_IMAGE;
          }}
        />
        {isSoldOut && <span className="badge badge--soldout">품절</span>}
        {!isSoldOut && isDiscontinued && <span className="badge badge--soldout">판매종료</span>}
        {!isSoldOut && !isDiscontinued && product.featured && <span className="badge badge--featured">추천</span>}
        {enableWishlist && (
          <WishlistButton
            productId={product.id}
            initialWishlisted={initialWishlisted}
            initialCount={product.wishlistCount || 0}
            className="product-card__wishlist"
            onChange={onWishlistChange}
          />
        )}
      </div>
      <div className="product-card__body">
        {product.brandDisplayName && <p className="product-card__brand">{product.brandDisplayName}</p>}
        <p className="product-card__name">{product.name}</p>
        {product.shortDescription && <p className="product-card__description">{product.shortDescription}</p>}
        {ratingCount > 0 && (
          <p className="product-card__rating" aria-label={`평균 평점 ${Number(product.averageRating || 0).toFixed(1)}점, 리뷰 ${ratingCount}개`}>
            ★ {Number(product.averageRating || 0).toFixed(1)} <span>리뷰 {ratingCount}</span>
          </p>
        )}
        <div className="product-card__price">
          {hasDiscount && <span className="product-card__original-price">{formatPrice(product.originalPrice)}</span>}
          <span className="product-card__sale-price">
            {discountRate > 0 && <span className="product-card__discount">{discountRate}%</span>}
            {formatPrice(product.salePrice)}
          </span>
        </div>
      </div>
    </Link>
  );
}

function computeDiscountRate(product) {
  if (typeof product.discountRate === "number") {
    return product.discountRate;
  }
  const original = Number(product.originalPrice);
  const sale = Number(product.salePrice);
  if (!original || original <= 0 || !Number.isFinite(sale) || sale >= original) {
    return 0;
  }
  return Math.floor(((original - sale) / original) * 100);
}
