import { Link } from "react-router-dom";
import { PLACEHOLDER_IMAGE, computeDiscountRate, getProductImageUrl } from "../../data/products";
import { formatPrice } from "../../utils/format";

// Card for the static product catalog (src/data/products.js) — kept separate from
// ProductCard.jsx, which renders backend-shaped products (wishlist/rating/stock
// fields) for MyWishlistPage and still needs that exact shape.
export function StaticProductCard({ product }) {
  const discountRate = computeDiscountRate(product);

  return (
    <Link to={`/products/${product.id}`} className="product-card">
      <div className="product-card__image-wrap">
        <img
          src={getProductImageUrl(product)}
          alt={product.name}
          className="product-card__image"
          loading="lazy"
          onError={(event) => {
            event.currentTarget.onerror = null;
            event.currentTarget.src = PLACEHOLDER_IMAGE;
          }}
        />
        {product.badge && <span className="badge badge--featured">{product.badge}</span>}
      </div>
      <div className="product-card__body">
        {product.brandName && <p className="product-card__brand">{product.brandName}</p>}
        <p className="product-card__name">{product.name}</p>
        <div className="product-card__price">
          {discountRate > 0 && <span className="product-card__original-price">{formatPrice(product.originalPrice)}</span>}
          <span className="product-card__sale-price">
            {discountRate > 0 && <span className="product-card__discount">{discountRate}%</span>}
            {formatPrice(product.price)}
          </span>
        </div>
      </div>
    </Link>
  );
}
