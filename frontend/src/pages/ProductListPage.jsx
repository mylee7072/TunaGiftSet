import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { PRODUCT_CATEGORIES } from "../data/categories";
import { findProducts } from "../data/products";
import { StaticProductCard } from "../components/product/StaticProductCard";
import { EmptyState } from "../components/common/EmptyState";

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keywordInput, setKeywordInput] = useState(searchParams.get("keyword") || "");

  const keyword = searchParams.get("keyword") || "";
  const categoryId = searchParams.get("categoryId") || "";

  useEffect(() => {
    document.title = keyword ? `'${keyword}' 검색 결과 - SeyoungGiftSet` : "전체 상품 - SeyoungGiftSet";
  }, [keyword]);

  useEffect(() => {
    setKeywordInput(keyword);
  }, [keyword]);

  const selectedCategory = PRODUCT_CATEGORIES.find((category) => category.id === categoryId);
  const products = findProducts({ categoryId: categoryId || undefined, keyword: keyword || undefined });

  function updateParams(next) {
    const params = new URLSearchParams(searchParams);
    Object.entries(next).forEach(([key, value]) => {
      if (value === undefined || value === null || value === "") {
        params.delete(key);
      } else {
        params.set(key, value);
      }
    });
    setSearchParams(params);
  }

  function handleSearchSubmit(event) {
    event.preventDefault();
    updateParams({ keyword: keywordInput.trim() });
  }

  function resetFilters() {
    setKeywordInput("");
    setSearchParams({});
  }

  return (
    <div className="container section product-list-page">
      <div className="product-list-page__header">
        <p className="breadcrumb">홈 / 상품</p>
        <h1 className="page-title">{keyword ? `'${keyword}' 검색 결과` : "전체 상품"}</h1>
        <p className="section__description">
          {selectedCategory ? selectedCategory.description : "구성과 가격을 비교해 필요한 선물세트를 골라보세요."}
        </p>
      </div>

      <form className="product-list-page__search" onSubmit={handleSearchSubmit}>
        <input
          type="search"
          value={keywordInput}
          onChange={(event) => setKeywordInput(event.target.value)}
          placeholder="상품명으로 검색해 보세요"
          aria-label="상품 검색"
        />
        {keywordInput && (
          <button type="button" className="btn btn--secondary" onClick={() => setKeywordInput("")}>
            지우기
          </button>
        )}
        <button type="submit" className="btn btn--primary">
          검색
        </button>
      </form>

      <div className="category-tabs" role="tablist" aria-label="카테고리">
        <button
          type="button"
          role="tab"
          aria-selected={!categoryId}
          className={`category-tabs__item${!categoryId ? " is-active" : ""}`}
          onClick={() => updateParams({ categoryId: undefined })}
        >
          전체
        </button>
        {PRODUCT_CATEGORIES.map((category) => (
          <button
            key={category.id}
            type="button"
            role="tab"
            aria-selected={categoryId === category.id}
            className={`category-tabs__item${categoryId === category.id ? " is-active" : ""}`}
            onClick={() => updateParams({ categoryId: category.id })}
          >
            {category.name}
          </button>
        ))}
      </div>

      {(keyword || categoryId) && (
        <div className="filter-summary" aria-label="적용된 필터">
          <div>
            {keyword && <span className="filter-chip">검색어: {keyword}</span>}
            {selectedCategory && <span className="filter-chip">카테고리: {selectedCategory.name}</span>}
          </div>
          <button type="button" className="link-button" onClick={resetFilters}>
            필터 초기화
          </button>
        </div>
      )}

      {products.length === 0 ? (
        <EmptyState
          message="검색 결과가 없습니다."
          action={
            keyword || categoryId ? (
              <button type="button" className="btn btn--secondary" onClick={resetFilters}>
                전체 상품 보기
              </button>
            ) : null
          }
        />
      ) : (
        <>
          <p className="product-list-page__count">총 {products.length}개 상품</p>
          <div className="product-grid">
            {products.map((product) => (
              <StaticProductCard key={product.id} product={product} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
