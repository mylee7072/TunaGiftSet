// Sized to roughly match the real content it stands in for (see each usage site)
// so the layout doesn't visibly jump once the real data replaces it.
export function SkeletonBlock({ className = "", style }) {
  return <span className={`skeleton ${className}`} style={style} aria-hidden="true" />;
}

// Mirrors ProductCard's structure (image-wrap + brand/name/price lines) closely
// enough that swapping it for the real card doesn't shift the grid around it.
export function ProductCardSkeleton() {
  return (
    <div className="product-card product-card--skeleton" aria-hidden="true">
      <div className="product-card__image-wrap">
        <SkeletonBlock className="skeleton--fill" />
      </div>
      <div className="product-card__body">
        <SkeletonBlock className="skeleton--text" style={{ width: "40%" }} />
        <SkeletonBlock className="skeleton--text" style={{ width: "85%", marginTop: 8 }} />
        <SkeletonBlock className="skeleton--text" style={{ width: "60%", marginTop: 6 }} />
        <SkeletonBlock className="skeleton--text" style={{ width: "45%", marginTop: 10, height: 18 }} />
      </div>
    </div>
  );
}

export function ProductGridSkeleton({ count = 8 }) {
  return (
    <div className="product-grid" role="status" aria-label="상품을 불러오는 중입니다">
      {Array.from({ length: count }).map((_, index) => (
        <ProductCardSkeleton key={index} />
      ))}
    </div>
  );
}

// A handful of list-row placeholders — used for order/wishlist/coupon lists whose
// real rows are a simple horizontal strip rather than a grid of cards.
export function ListRowSkeleton({ count = 3, height = 84 }) {
  return (
    <div className="list-row-skeleton" role="status" aria-label="목록을 불러오는 중입니다">
      {Array.from({ length: count }).map((_, index) => (
        <SkeletonBlock key={index} className="skeleton--row" style={{ height }} />
      ))}
    </div>
  );
}
