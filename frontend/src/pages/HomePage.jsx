import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { productApi, categoryApi } from "../api/productApi";
import { ProductCard } from "../components/product/ProductCard";
import { ProductGridSkeleton } from "../components/common/Skeleton";
import { ErrorState } from "../components/common/ErrorState";
import { Reveal } from "../components/common/Reveal";
import { siteConfig } from "../config/siteConfig";

export function HomePage() {
  const [categories, setCategories] = useState([]);
  const [featured, setFeatured] = useState(null);
  const [latest, setLatest] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    document.title = `${siteConfig.siteName} - ${siteConfig.tagline}`;
  }, []);

  useEffect(() => {
    categoryApi.findActiveCategories().then(setCategories).catch(() => setCategories([]));

    Promise.all([
      productApi.findProducts({ featured: true, size: 4, sort: "LATEST" }),
      productApi.findProducts({ size: 8, sort: "LATEST" }),
    ])
      .then(([featuredPage, latestPage]) => {
        setFeatured(featuredPage.content);
        setLatest(latestPage.content);
      })
      .catch(() => setError("상품을 불러오지 못했습니다."));
  }, []);

  const rootCategories = categories
    .filter((category) => !category.parentId)
    .sort((a, b) => a.displayOrder - b.displayOrder);

  return (
    <div className="home-page">
      <section className="hero">
        <div className="container hero__inner">
          <div className="hero__text">
            <p className="hero__eyebrow">정성을 고르는 시간</p>
            <h1>마음을 담은 선물세트를 한곳에서</h1>
            <p>
              명절 선물, 기업 답례, 가족을 위한 실속 구성까지. 상품 구성과 가격을 차분하게 비교하고 필요한 수량만큼 주문할 수 있습니다.
            </p>
            <div className="hero__actions">
              <Link to="/products" className="btn btn--primary btn--large">
                선물세트 둘러보기
              </Link>
              <Link to="/products?sort=LATEST" className="btn btn--secondary btn--large">
                새 상품 보기
              </Link>
            </div>
          </div>
          <div className="hero__visual" aria-hidden="true">
            <img src="/hero-placeholder.svg" alt="" className="hero__image" />
          </div>
        </div>
      </section>

      <Reveal as="section" className="container section">
        <div className="section-header">
          <div>
            <h2 className="section__title">카테고리로 찾기</h2>
            <p className="section__description">받는 분과 용도에 맞는 상품 구성을 빠르게 찾아보세요.</p>
          </div>
          <Link to="/products" className="section-header__link">전체보기</Link>
        </div>
        <div className="category-quick-menu">
          {rootCategories.map((category) => (
            <Link key={category.id} to={`/products?categoryId=${category.id}`} className="category-quick-menu__item">
              <span className="category-quick-menu__mark" aria-hidden="true">{category.name.slice(0, 1)}</span>
              <span>{category.name}</span>
            </Link>
          ))}
          {categories.length === 0 && <p className="muted">카테고리 정보를 준비 중입니다.</p>}
        </div>
      </Reveal>

      {error && (
        <div className="container">
          <ErrorState message={error} />
        </div>
      )}

      {!error && featured === null && (
        <div className="container section">
          <ProductGridSkeleton count={4} />
        </div>
      )}

      {featured && featured.length > 0 && (
        <Reveal as="section" className="container section">
          <div className="section-header">
            <div>
              <h2 className="section__title">추천 선물세트</h2>
              <p className="section__description">선물용으로 고르기 좋은 대표 상품입니다.</p>
            </div>
            <Link to="/products?featured=true" className="section-header__link">전체보기</Link>
          </div>
          <div className="product-grid">
            {featured.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        </Reveal>
      )}

      {latest && latest.length > 0 && (
        <Reveal as="section" className="container section">
          <div className="section-header">
            <div>
              <h2 className="section__title">새로 준비한 상품</h2>
              <p className="section__description">최근 등록된 선물세트를 확인해 보세요.</p>
            </div>
            <Link to="/products?sort=LATEST" className="section-header__link">전체보기</Link>
          </div>
          <div className="product-grid">
            {latest.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        </Reveal>
      )}

      <Reveal as="section" className="container section gift-info">
        <div>
          <p className="gift-info__eyebrow">Gift Guide</p>
          <h2 className="section__title">상황에 맞는 선물 선택</h2>
          <p>
            명절 선물, 기업 답례, 가족 선물까지 다양한 구성의 선물세트를 준비하고 있습니다. 상품 구성과 가격을 비교해 필요한 상품을 골라보세요.
          </p>
        </div>
        <Link to="/products" className="btn btn--secondary">상품 비교하기</Link>
      </Reveal>
    </div>
  );
}
