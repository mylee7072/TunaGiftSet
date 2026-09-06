import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { adminProductApi, adminBrandApi, adminCategoryApi } from "../../api/productApi";
import { Loading } from "../../components/common/Loading";
import { ErrorState } from "../../components/common/ErrorState";
import { Pagination } from "../../components/common/Pagination";
import { formatPrice } from "../../utils/format";

const STATUS_OPTIONS = ["ACTIVE", "SOLD_OUT", "HIDDEN", "DISCONTINUED"];
const STATUS_LABEL = { ACTIVE: "판매중", SOLD_OUT: "품절", HIDDEN: "숨김", DISCONTINUED: "단종" };

export function AdminProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get("page") || 0);
  const filters = {
    keyword: searchParams.get("keyword") || "",
    categoryId: searchParams.get("categoryId") || "",
    brandId: searchParams.get("brandId") || "",
    status: searchParams.get("status") || "",
    sort: searchParams.get("sort") || "LATEST",
  };

  const [draft, setDraft] = useState(filters);
  const [pageData, setPageData] = useState(null);
  const [status, setStatus] = useState("loading");
  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);

  useEffect(() => {
    adminCategoryApi.findAllCategories().then(setCategories).catch(() => setCategories([]));
    adminBrandApi.findAllBrands().then(setBrands).catch(() => setBrands([]));
  }, []);

  function load() {
    setStatus("loading");
    adminProductApi
      .findProducts({ ...filters, page, size: 20 })
      .then((data) => {
        setPageData(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }

  useEffect(() => {
    setDraft(filters);
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  function applyFilters(event) {
    event.preventDefault();
    const next = {};
    Object.entries(draft).forEach(([key, value]) => {
      if (value) next[key] = value;
    });
    setSearchParams(next);
  }

  return (
    <div className="admin-page">
      <div className="admin-page__header">
        <h1 className="page-title">상품 관리</h1>
        <Link to="/admin/products/new" className="btn btn--primary">
          상품 등록
        </Link>
      </div>

      <form className="admin-filter-bar admin-filter-bar--wrap" onSubmit={applyFilters}>
        <input placeholder="상품명 검색" value={draft.keyword} onChange={(event) => setDraft({ ...draft, keyword: event.target.value })} />
        <select value={draft.categoryId} onChange={(event) => setDraft({ ...draft, categoryId: event.target.value })}>
          <option value="">카테고리 전체</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        <select value={draft.brandId} onChange={(event) => setDraft({ ...draft, brandId: event.target.value })}>
          <option value="">브랜드 전체</option>
          {brands.map((brand) => (
            <option key={brand.id} value={brand.id}>
              {brand.displayName}
            </option>
          ))}
        </select>
        <select value={draft.status} onChange={(event) => setDraft({ ...draft, status: event.target.value })}>
          <option value="">상태 전체</option>
          {STATUS_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {STATUS_LABEL[option]}
            </option>
          ))}
        </select>
        <button type="submit" className="btn btn--primary">
          검색
        </button>
      </form>

      {status === "loading" && <Loading />}
      {status === "error" && <ErrorState message="상품 목록을 불러오지 못했습니다." onRetry={load} />}

      {status === "ready" && pageData && (
        <>
          <div className="admin-table-wrapper">
            <table className="admin-table">
              <thead>
                <tr>
                  <th></th>
                  <th>상품코드</th>
                  <th>상품명</th>
                  <th>브랜드</th>
                  <th>카테고리</th>
                  <th>판매가</th>
                  <th>재고</th>
                  <th>상태</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((product) => (
                  <tr key={product.id}>
                    <td>
                      <img
                        src={product.thumbnailImageUrl || "/placeholder-product.svg"}
                        alt=""
                        className="admin-table__thumb"
                        onError={(event) => {
                          event.currentTarget.onerror = null;
                          event.currentTarget.src = "/placeholder-product.svg";
                        }}
                      />
                    </td>
                    <td>{product.productCode}</td>
                    <td>{product.name}</td>
                    <td>{product.brandDisplayName}</td>
                    <td>{product.categoryName}</td>
                    <td>{formatPrice(product.salePrice)}</td>
                    <td className={product.stockQuantity <= 5 ? "admin-table__low-stock" : undefined}>{product.stockQuantity}</td>
                    <td>
                      <span className={`admin-badge${product.status === "ACTIVE" ? " admin-badge--active" : ""}`}>
                        {STATUS_LABEL[product.status] || product.status}
                      </span>
                    </td>
                    <td>
                      <Link to={`/admin/products/${product.id}/edit`} className="btn btn--secondary">
                        수정
                      </Link>
                    </td>
                  </tr>
                ))}
                {pageData.content.length === 0 && (
                  <tr>
                    <td colSpan={9} className="admin-table__empty">
                      조건에 맞는 상품이 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <Pagination page={pageData.page} totalPages={pageData.totalPages} onPageChange={(nextPage) => setSearchParams({ ...filters, page: nextPage })} />
        </>
      )}
    </div>
  );
}
