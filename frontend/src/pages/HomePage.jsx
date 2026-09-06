import { useEffect } from "react";
import { Link } from "react-router-dom";
import { PRODUCT_CATEGORIES } from "../data/categories";
import { findProducts } from "../data/products";
import { StaticProductCard } from "../components/product/StaticProductCard";
import { Reveal } from "../components/common/Reveal";
import { siteConfig } from "../config/siteConfig";

export function HomePage() {
  useEffect(() => {
    document.title = `${siteConfig.siteName} - ${siteConfig.tagline}`;
  }, []);

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
          {PRODUCT_CATEGORIES.map((category) => (
            <Link key={category.id} to={`/products?categoryId=${category.id}`} className="category-quick-menu__item">
              <span className="category-quick-menu__mark" aria-hidden="true">{category.name.slice(0, 1)}</span>
              <span>{category.name}</span>
            </Link>
          ))}
        </div>
      </Reveal>

      {PRODUCT_CATEGORIES.map((category) => {
        const products = findProducts({ categoryId: category.id });
        if (products.length === 0) return null;
        return (
          <Reveal as="section" className="container section" key={category.id}>
            <div className="section-header">
              <div>
                <h2 className="section__title">{category.name}</h2>
                <p className="section__description">{category.description}</p>
              </div>
              <Link to={`/products?categoryId=${category.id}`} className="section-header__link">전체보기</Link>
            </div>
            <div className="product-grid">
              {products.map((product) => (
                <StaticProductCard key={product.id} product={product} />
              ))}
            </div>
          </Reveal>
        );
      })}

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
