import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { productApi, categoryApi, brandApi } from "../api/productApi";
import { wishlistApi } from "../api/wishlistApi";
import { ProductCard } from "../components/product/ProductCard";
import { ProductGridSkeleton } from "../components/common/Skeleton";
import { EmptyState } from "../components/common/EmptyState";
import { ErrorState } from "../components/common/ErrorState";
import { Pagination } from "../components/common/Pagination";
import { useAuth } from "../context/useAuth";

const SORT_OPTIONS = [
  { value: "LATEST", label: "최신순" },
  { value: "PRICE_ASC", label: "낮은 가격순" },
  { value: "PRICE_DESC", label: "높은 가격순" },
];

export function ProductListPage() {
  const { isAuthenticated } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [pageData, setPageData] = useState(null);
  const [wishlistedProductIds, setWishlistedProductIds] = useState(new Set());
  const [status, setStatus] = useState("loading");
  const [keywordInput, setKeywordInput] = useState(searchParams.get("keyword") || "");

  const keyword = searchParams.get("keyword") || "";
  const categoryId = searchParams.get("categoryId") || "";
  const brandId = searchParams.get("brandId") || "";
  const sort = searchParams.get("sort") || "LATEST";
  const page = Number(searchParams.get("page") || 0);

  useEffect(() => {
    document.title = keyword ? `'${keyword}' 검색 결과 - TunaGiftSet` : "전체 상품 - TunaGiftSet";
  }, [keyword]);

  useEffect(() => {
    categoryApi.findActiveCategories().then(setCategories).catch(() => setCategories([]));
    brandApi.findActiveBrands().then(setBrands).catch(() => setBrands([]));
  }, []);

  useEffect(() => {
    setKeywordInput(keyword);
  }, [keyword]);

  useEffect(() => {
    let cancelled = false;
    setStatus("loading");
    productApi
      .findProducts({ keyword, categoryId: categoryId || undefined, brandId: brandId || undefined, sort, page, size: 12 })
      .then(async (data) => {
        if (cancelled) return;
        setPageData(data);
        if (isAuthenticated && data.content.length > 0) {
          try {
            const response = await wishlistApi.findWishlistedProductIds(data.content.map((product) => product.id));
            if (!cancelled) setWishlistedProductIds(new Set(response.productIds));
          } catch {
            if (!cancelled) setWishlistedProductIds(new Set());
          }
        } else {
          setWishlistedProductIds(new Set());
        }
        if (!cancelled) setStatus("ready");
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, [keyword, categoryId, brandId, sort, page, isAuthenticated]);

  function handleWishlistChange(response) {
    setWishlistedProductIds((current) => {
      const next = new Set(current);
      if (response.wishlisted) {
        next.add(response.productId);
      } else {
        next.delete(response.productId);
      }
      return next;
    });
    setPageData((current) => {
      if (!current) return current;
      return {
        ...current,
        content: current.content.map((product) =>
          product.id === response.productId ? { ...product, wishlistCount: response.wishlistCount } : product
        ),
      };
    });
  }

  function updateParams(next) {
    const params = new URLSearchParams(searchParams);
    Object.entries(next).forEach(([key, value]) => {
      if (value === undefined || value === null || value === "") {
        params.delete(key);
      } else {
        params.set(key, value);
      }
    });
    if (!("page" in next)) {
      params.delete("page");
    }
    setSearchParams(params);
  }

  function resetFilters() {
    setKeywordInput("");
    setSearchParams({});
  }

  function handleSearchSubmit(event) {
    event.preventDefault();
    updateParams({ keyword: keywordInput.trim() });
  }

  const selectedCategory = categories.find((category) => String(category.id) === String(categoryId));
  const selectedBrand = brands.find((brand) => String(brand.id) === String(brandId));
  const selectedSort = SORT_OPTIONS.find((option) => option.value === sort)?.label || "최신순";
  const hasActiveFilter = Boolean(keyword || categoryId || brandId || sort !== "LATEST");
  const activeFilterLabels = useMemo(
    () => [
      keyword && `검색어: ${keyword}`,
      selectedCategory && `카테고리: ${selectedCategory.name}`,
      selectedBrand && `브랜드: ${selectedBrand.displayName}`,
      sort !== "LATEST" && `정렬: ${selectedSort}`,
    ].filter(Boolean),
    [keyword, selectedCategory, selectedBrand, sort, selectedSort]
  );

  return (
    <div className="container section product-list-page">
      <div className="product-list-page__header">
        <p className="breadcrumb">홈 / 상품</p>
        <h1 className="page-title">{keyword ? `'${keyword}' 검색 결과` : "전체 상품"}</h1>
        <p className="section__description">
          {selectedCategory ? `${selectedCategory.name} 카테고리` : "선물세트를 가격, 브랜드, 카테고리별로 비교해 보세요."}
          {selectedBrand ? ` · ${selectedBrand.displayName}` : ""}
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

      <div className="product-list-page__toolbar">
        <div className="product-list-page__filters">
          <label>
            <span>카테고리</span>
            <select value={categoryId} onChange={(event) => updateParams({ categoryId: event.target.value })} aria-label="카테고리 선택">
              <option value="">전체 카테고리</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>

          <label>
            <span>브랜드</span>
            <select value={brandId} onChange={(event) => updateParams({ brandId: event.target.value })} aria-label="브랜드 선택">
              <option value="">전체 브랜드</option>
              {brands.map((brand) => (
                <option key={brand.id} value={brand.id}>
                  {brand.displayName}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label className="product-list-page__sort">
          <span>정렬</span>
          <select value={sort} onChange={(event) => updateParams({ sort: event.target.value })} aria-label="정렬 방식 선택">
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {hasActiveFilter && (
        <div className="filter-summary" aria-label="적용된 필터">
          <div>
            {activeFilterLabels.map((label) => (
              <span key={label} className="filter-chip">{label}</span>
            ))}
          </div>
          <button type="button" className="link-button" onClick={resetFilters}>
            필터 초기화
          </button>
        </div>
      )}

      {status === "loading" && <ProductGridSkeleton count={12} />}
      {status === "error" && <ErrorState message="상품을 불러오지 못했습니다." onRetry={() => updateParams({})} />}
      {status === "ready" && pageData.content.length === 0 && (
        <EmptyState
          message="검색 결과가 없습니다."
          action={
            hasActiveFilter ? (
              <button type="button" className="btn btn--secondary" onClick={resetFilters}>
                전체 상품 보기
              </button>
            ) : null
          }
        />
      )}
      {status === "ready" && pageData.content.length > 0 && (
        <>
          <p className="product-list-page__count">총 {pageData.totalElements}개 상품</p>
          <div className="product-grid">
            {pageData.content.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                enableWishlist
                initialWishlisted={wishlistedProductIds.has(product.id)}
                onWishlistChange={handleWishlistChange}
              />
            ))}
          </div>
          <Pagination page={pageData.page} totalPages={pageData.totalPages} onPageChange={(nextPage) => updateParams({ page: nextPage })} />
        </>
      )}
    </div>
  );
}
