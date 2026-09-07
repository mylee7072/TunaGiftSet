import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { categoryApi, productApi } from "../api/productApi";
import { EmptyState } from "../components/common/EmptyState";
import { ErrorState } from "../components/common/ErrorState";
import { Loading } from "../components/common/Loading";
import { ProductCard } from "../components/product/ProductCard";
import { Reveal } from "../components/common/Reveal";
import { siteConfig } from "../config/siteConfig";
import { useDocumentMeta } from "../hooks/useDocumentMeta";

export function HomePage() {
  const [categories, setCategories] = useState([]);
  const [featuredProducts, setFeaturedProducts] = useState([]);
  const [latestProducts, setLatestProducts] = useState([]);
  const [status, setStatus] = useState("loading");

  useDocumentMeta({
    title: `${siteConfig.siteName} - ${siteConfig.tagline}`,
    description: "정성을 담은 선물, 세영선물세트에서 준비했습니다. 튜나리챔 세트, 스페셜 세트 등 명절과 답례 선물로 좋은 구성을 만나보세요.",
  });

  useEffect(() => {
    loadHome();
  }, []);

  async function loadHome() {
    setStatus("loading");
    try {
      const [categoryList, featuredPage, latestPage] = await Promise.all([
        categoryApi.findActiveCategories().catch(() => []),
        productApi.findProducts({ featured: true, page: 0, size: 4 }).catch(() => ({ content: [] })),
        productApi.findProducts({ sort: "LATEST", page: 0, size: 8 }),
      ]);
      setCategories(Array.isArray(categoryList) ? categoryList.filter((category) => !category.parentId) : []);
      setFeaturedProducts(featuredPage.content || []);
      setLatestProducts(latestPage.content || []);
      setStatus("ready");
    } catch {
      setStatus("error");
    }
  }

  return (
    <div className="home-page">
      <section className="hero">
        <div className="container hero__inner">
          <div className="hero__text">
            <p className="hero__eyebrow">정성을 고르는 시간</p>
            <h1>마음을 담은 선물세트를 한곳에서</h1>
            <p>
              명절 선물, 기업 답례, 가족을 위한 실속 구성까지. 실제 등록된 상품을 기준으로 구성과 가격을 비교해 보세요.
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

      {status === "loading" && (
        <div className="container section">
          <Loading label="상품을 불러오는 중입니다..." />
        </div>
      )}

      {status === "error" && (
        <div className="container section">
          <ErrorState message="상품 정보를 불러오지 못했습니다." onRetry={loadHome} />
        </div>
      )}

      {status === "ready" && (
        <>
          {categories.length > 0 && (
            <Reveal as="section" className="container section">
              <div className="section-header">
                <div>
                  <h2 className="section__title">카테고리로 찾기</h2>
                  <p className="section__description">받는 분과 용도에 맞는 상품 구성을 빠르게 찾아보세요.</p>
                </div>
                <Link to="/products" className="section-header__link">전체보기</Link>
              </div>
              <div className="category-quick-menu">
                {categories.map((category) => (
                  <Link key={category.id} to={`/products?categoryId=${category.id}`} className="category-quick-menu__item">
                    <span className="category-quick-menu__mark" aria-hidden="true">{category.name.slice(0, 1)}</span>
                    <span>{category.name}</span>
                  </Link>
                ))}
              </div>
            </Reveal>
          )}

          {featuredProducts.length > 0 && (
            <Reveal as="section" className="container section">
              <div className="section-header">
                <div>
                  <h2 className="section__title">추천 선물세트</h2>
                  <p className="section__description">운영자가 추천으로 등록한 상품입니다.</p>
                </div>
                <Link to="/products?featured=true" className="section-header__link">전체보기</Link>
              </div>
              <div className="product-grid">
                {featuredProducts.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            </Reveal>
          )}

          <Reveal as="section" className="container section">
            <div className="section-header">
              <div>
                <h2 className="section__title">새로 준비한 상품</h2>
                <p className="section__description">최근 등록된 선물세트를 확인해 보세요.</p>
              </div>
              <Link to="/products" className="section-header__link">전체보기</Link>
            </div>
            {latestProducts.length > 0 ? (
              <div className="product-grid">
                {latestProducts.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            ) : (
              <EmptyState message="등록된 상품이 없습니다." action={<Link to="/products" className="btn btn--secondary">상품 목록으로</Link>} />
            )}
          </Reveal>
        </>
      )}
    </div>
  );
}
