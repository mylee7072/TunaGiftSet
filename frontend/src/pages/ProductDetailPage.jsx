import { useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import { PLACEHOLDER_IMAGE, computeDiscountRate, findProductById, getProductImageUrl } from "../data/products";
import { findCategoryById } from "../data/categories";
import { EmptyState } from "../components/common/EmptyState";
import { formatPrice } from "../utils/format";

export function ProductDetailPage() {
  const { productId } = useParams();
  const product = findProductById(productId);
  const category = product ? findCategoryById(product.categoryId) : null;

  useEffect(() => {
    document.title = product ? `${product.name} - TunaGiftSet` : "상품을 찾을 수 없습니다 - TunaGiftSet";
  }, [product]);

  if (!product) {
    return (
      <div className="container section">
        <EmptyState
          message="상품을 찾을 수 없습니다."
          action={<Link to="/products" className="btn btn--secondary">목록으로</Link>}
        />
      </div>
    );
  }

  const discountRate = computeDiscountRate(product);

  return (
    <div className="container section product-detail-page">
      <p className="breadcrumb">홈 / {category?.name || "상품"} / 상품상세</p>

      <div className="product-detail">
        <div className="product-detail__gallery">
          <div className="product-detail__main-image-frame">
            <img
              src={getProductImageUrl(product)}
              alt={product.name}
              className="product-detail__main-image"
              onError={(event) => {
                event.currentTarget.onerror = null;
                event.currentTarget.src = PLACEHOLDER_IMAGE;
              }}
            />
          </div>
        </div>

        <div className="product-detail__info">
          {product.brandName && <p className="product-detail__brand">{product.brandName}</p>}
          <h1 className="product-detail__name">
            {product.name}
            {product.badge && <span className="badge badge--featured product-detail__badge">{product.badge}</span>}
          </h1>
          <p className="product-detail__meta">
            {category?.name}
            {product.barcode ? ` · 바코드 ${product.barcode}` : ""}
            {product.boxUnit ? ` · ${product.boxUnit}` : ""}
          </p>

          <div className="product-detail__price">
            {discountRate > 0 && <span className="product-detail__original-price">{formatPrice(product.originalPrice)}</span>}
            <span className="product-detail__sale-price">
              {discountRate > 0 && <span className="product-card__discount">{discountRate}%</span>}
              {formatPrice(product.price)}
            </span>
          </div>

          {product.note && <p className="product-detail__short-description">{product.note}</p>}

          <div className="product-detail__purchase-notice" role="note">
            현재 테스트 단계로 온라인 주문 기능은 준비 중입니다. 구성과 가격을 먼저 확인해 주세요.
          </div>

          <div className="product-detail__actions">
            <Link to="/products" className="btn btn--secondary btn--large">
              목록으로
            </Link>
          </div>
        </div>
      </div>

      <section id="product-info" className="product-detail__description">
        <h2>상품 구성</h2>
        <div className="table-scroll">
          <table className="composition-table">
            <thead>
              <tr>
                <th>구성품</th>
                <th>용량</th>
                <th>수량</th>
              </tr>
            </thead>
            <tbody>
              {product.composition.map((item, index) => (
                <tr key={`${item.name}-${index}`}>
                  <td>{item.name}</td>
                  <td>{item.weight}</td>
                  <td>{item.count}개</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <h2>상품 설명</h2>
        <p>{product.description}</p>
      </section>
    </div>
  );
}
