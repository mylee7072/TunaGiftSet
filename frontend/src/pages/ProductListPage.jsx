import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { ApiError } from "../api/apiClient";
import { brandApi, categoryApi, productApi } from "../api/productApi";
import { wishlistApi } from "../api/wishlistApi";
import { EmptyState } from "../components/common/EmptyState";
import { ErrorState } from "../components/common/ErrorState";
import { Loading } from "../components/common/Loading";
import { Pagination } from "../components/common/Pagination";
import { ProductCard } from "../components/product/ProductCard";
import { siteConfig } from "../config/siteConfig";
import { useAuth } from "../context/useAuth";

const PAGE_SIZE = 12;

export function ProductListPage() {
  const { isAuthenticated } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [keywordInput, setKeywordInput] = useState(searchParams.get("keyword") || "");
  const [products, setProducts] = useState(null);
  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [wishlistedProductIds, setWishlistedProductIds] = useState(() => new Set());
  const [status, setStatus] = useState("loading");
  const [errorMessage, setErrorMessage] = useState("");

  const keyword = searchParams.get("keyword") || "";
  const categoryId = searchParams.get("categoryId") || "";
  const brandId = searchParams.get("brandId") || "";
  const sort = searchParams.get("sort") || "LATEST";
  const page = Number(searchParams.get("page") || 0);

  const selectedCategory = useMemo(
    () => categories.find((category) => String(category.id) === String(categoryId)),
    [categories, categoryId]
  );
  const selectedBrand = useMemo(
    () => brands.find((brand) => String(brand.id) === String(brandId)),
    [brands, brandId]
  );

  const loadProducts = useCallback(async () => {
    setStatus("loading");
    setErrorMessage("");
    try {
      const [productPage, categoryList, brandList] = await Promise.all([
        productApi.findProducts({
          keyword,
          categoryId,
          brandId,
          sort,
          page: Number.isFinite(page) ? page : 0,
          size: PAGE_SIZE,
        }),
        categoryApi.findActiveCategories().catch(() => []),
        brandApi.findActiveBrands().catch(() => []),
      ]);

      setProducts(productPage);
      setCategories(Array.isArray(categoryList) ? categoryList : []);
      setBrands(Array.isArray(brandList) ? brandList : []);
      setStatus("ready");

      const productIds = productPage?.content?.map((product) => product.id).filter(Boolean) || [];
      if (isAuthenticated && productIds.length > 0) {
        const wishlistResponse = await wishlistApi.findWishlistedProductIds(productIds).catch(() => null);
        setWishlistedProductIds(new Set(wishlistResponse?.productIds || []));
      } else {
        setWishlistedProductIds(new Set());
      }
    } catch (error) {
      setStatus("error");
      setProducts(null);
      setErrorMessage(error instanceof ApiError ? error.message : "상품 정보를 불러오지 못했습니다.");
    }
  }, [brandId, categoryId, isAuthenticated, keyword, page, sort]);

  useEffect(() => {
    document.title = keyword ? `'${keyword}' 검색 결과 - ${siteConfig.siteName}` : `전체 상품 - ${siteConfig.siteName}`;
  }, [keyword]);

  useEffect(() => {
    setKeywordInput(keyword);
  }, [keyword]);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  function updateParams(next) {
    const params = new URLSearchParams(searchParams);
    Object.entries(next).forEach(([key, value]) => {
      if (value === undefined || value === null || value === "") {
        params.delete(key);
      } else {
        params.set(key, value);
      }
    });
    if (!Object.prototype.hasOwnProperty.call(next, "page")) {
      params.delete("page");
    }
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

  const content = products?.content || [];

  return (
    <div className="container section product-list-page">
      <div className="product-list-page__header">
        <p className="breadcrumb">홈 / 상품</p>
        <h1 className="page-title">{keyword ? `'${keyword}' 검색 결과` : "전체 상품"}</h1>
        <p className="section__description">
          {selectedCategory ? selectedCategory.description || `${selectedCategory.name} 상품입니다.` : "구성과 가격을 비교해 필요한 선물세트를 골라보세요."}
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
            카테고리
            <select value={categoryId} onChange={(event) => updateParams({ categoryId: event.target.value })}>
              <option value="">전체상품</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            브랜드
            <select value={brandId} onChange={(event) => updateParams({ brandId: event.target.value })}>
              <option value="">전체 브랜드</option>
              {brands.map((brand) => (
                <option key={brand.id} value={brand.id}>
                  {brand.displayName || brand.name}
                </option>
              ))}
            </select>
          </label>
        </div>
        <label className="product-list-page__sort">
          정렬
          <select value={sort} onChange={(event) => updateParams({ sort: event.target.value })}>
            <option value="LATEST">최신순</option>
            <option value="PRICE_ASC">낮은 가격순</option>
            <option value="PRICE_DESC">높은 가격순</option>
          </select>
        </label>
      </div>

      {(keyword || categoryId || brandId || sort !== "LATEST") && (
        <div className="filter-summary" aria-label="적용된 필터">
          <div>
            {keyword && <span className="filter-chip">검색어: {keyword}</span>}
            {selectedCategory && <span className="filter-chip">카테고리: {selectedCategory.name}</span>}
            {selectedBrand && <span className="filter-chip">브랜드: {selectedBrand.displayName || selectedBrand.name}</span>}
            {sort !== "LATEST" && <span className="filter-chip">정렬: {sort === "PRICE_ASC" ? "낮은 가격순" : "높은 가격순"}</span>}
          </div>
          <button type="button" className="link-button" onClick={resetFilters}>
            필터 초기화
          </button>
        </div>
      )}

      {status === "loading" && !products && <Loading label="상품을 불러오는 중입니다..." />}

      {status === "error" && (
        <ErrorState message={errorMessage || "상품 정보를 불러오지 못했습니다."} onRetry={loadProducts} />
      )}

      {status === "ready" && content.length === 0 && (
        <EmptyState
          message="검색 결과가 없습니다."
          action={
            keyword || categoryId || brandId ? (
              <button type="button" className="btn btn--secondary" onClick={resetFilters}>
                전체 상품 보기
              </button>
            ) : null
          }
        />
      )}

      {status === "ready" && content.length > 0 && (
        <>
          <p className="product-list-page__count">총 {products.totalElements}개 상품</p>
          <div className="product-grid">
            {content.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                enableWishlist
                initialWishlisted={wishlistedProductIds.has(product.id)}
              />
            ))}
          </div>
          <Pagination page={products.page} totalPages={products.totalPages} onPageChange={(nextPage) => updateParams({ page: nextPage })} />
        </>
      )}
    </div>
  );
}
