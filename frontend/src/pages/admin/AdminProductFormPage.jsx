import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  adminProductApi,
  adminProductImageApi,
  adminBrandApi,
  adminCategoryApi,
  adminInventoryApi,
} from "../../api/productApi";
import { Loading } from "../../components/common/Loading";
import { ErrorState } from "../../components/common/ErrorState";
import { Pagination } from "../../components/common/Pagination";
import { useToast } from "../../context/useToast";
import { ApiError } from "../../api/apiClient";
import { formatDateTime } from "../../utils/format";

const STATUS_OPTIONS = [
  { value: "ACTIVE", label: "판매중" },
  { value: "SOLD_OUT", label: "품절" },
  { value: "HIDDEN", label: "숨김" },
  { value: "DISCONTINUED", label: "단종" },
];

const CHANGE_TYPE_LABEL = {
  ORDER: "주문 차감",
  ORDER_CANCEL: "취소/복구",
  ADMIN_INCREASE: "관리자 증가",
  ADMIN_DECREASE: "관리자 감소",
};

const EMPTY_FORM = {
  brandId: "",
  categoryId: "",
  productCode: "",
  name: "",
  shortDescription: "",
  description: "",
  originalPrice: "",
  salePrice: "",
  stockQuantity: "0",
  status: "ACTIVE",
  featured: false,
};

export function AdminProductFormPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const isEdit = Boolean(productId);

  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [status, setStatus] = useState(isEdit ? "loading" : "ready");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [images, setImages] = useState([]);
  const [uploadFile, setUploadFile] = useState(null);
  const [uploadType, setUploadType] = useState("DETAIL");
  const [uploading, setUploading] = useState(false);

  const [inventory, setInventory] = useState(null);
  const [historyPage, setHistoryPage] = useState(null);
  const [historyPageNumber, setHistoryPageNumber] = useState(0);
  const [adjustForm, setAdjustForm] = useState({ type: "INCREASE", quantity: "1", reason: "" });
  const [adjusting, setAdjusting] = useState(false);

  useEffect(() => {
    adminCategoryApi.findAllCategories().then(setCategories).catch(() => setCategories([]));
    adminBrandApi.findAllBrands().then(setBrands).catch(() => setBrands([]));
  }, []);

  const loadProduct = useCallback(() => {
    if (!isEdit) return;
    setStatus("loading");
    adminProductApi
      .findProduct(productId)
      .then((data) => {
        setForm({
          brandId: data.brandId,
          categoryId: data.categoryId,
          productCode: data.productCode,
          name: data.name,
          shortDescription: data.shortDescription || "",
          description: data.description || "",
          originalPrice: data.originalPrice,
          salePrice: data.salePrice,
          stockQuantity: data.stockQuantity,
          status: data.status,
          featured: data.featured,
        });
        setImages(data.images || []);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, [isEdit, productId]);

  const loadInventory = useCallback(() => {
    if (!isEdit) return;
    adminInventoryApi.getInventory(productId).then(setInventory).catch(() => setInventory(null));
  }, [isEdit, productId]);

  const loadHistory = useCallback(() => {
    if (!isEdit) return;
    adminInventoryApi
      .getHistory(productId, historyPageNumber, 10)
      .then(setHistoryPage)
      .catch(() => setHistoryPage(null));
  }, [isEdit, productId, historyPageNumber]);

  useEffect(() => {
    loadProduct();
  }, [loadProduct]);

  useEffect(() => {
    loadInventory();
    loadHistory();
  }, [loadInventory, loadHistory]);

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting) return;

    if (!form.brandId || !form.categoryId || !form.productCode.trim() || !form.name.trim() || form.originalPrice === "" || form.salePrice === "") {
      setError("필수 항목을 모두 입력해 주세요.");
      return;
    }

    const payload = {
      brandId: Number(form.brandId),
      categoryId: Number(form.categoryId),
      productCode: form.productCode.trim(),
      name: form.name.trim(),
      shortDescription: form.shortDescription.trim() || null,
      description: form.description.trim() || null,
      originalPrice: Number(form.originalPrice),
      salePrice: Number(form.salePrice),
      stockQuantity: Number(form.stockQuantity) || 0,
      status: form.status,
      featured: form.featured,
    };

    setSubmitting(true);
    setError("");
    try {
      if (isEdit) {
        await adminProductApi.updateProduct(productId, payload);
        showToast("상품 정보를 저장했습니다.");
        loadProduct();
      } else {
        const created = await adminProductApi.createProduct(payload);
        showToast("상품을 등록했습니다. 이어서 이미지를 등록해 주세요.");
        navigate(`/admin/products/${created.id}/edit`, { replace: true });
      }
    } catch (submitError) {
      setError(submitError instanceof ApiError ? submitError.message : "저장하지 못했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleUpload(event) {
    event.preventDefault();
    if (!uploadFile || uploading) return;
    setUploading(true);
    try {
      await adminProductImageApi.upload(productId, uploadFile, uploadType, images.length);
      showToast("이미지를 업로드했습니다.");
      setUploadFile(null);
      event.target.reset();
      const refreshed = await adminProductApi.findProduct(productId);
      setImages(refreshed.images || []);
    } catch (uploadError) {
      showToast(uploadError instanceof ApiError ? uploadError.message : "이미지 업로드에 실패했습니다.", "error");
    } finally {
      setUploading(false);
    }
  }

  async function handleSetMain(imageId) {
    try {
      await adminProductImageApi.setMain(productId, imageId);
      const refreshed = await adminProductApi.findProduct(productId);
      setImages(refreshed.images || []);
    } catch (setMainError) {
      showToast(setMainError instanceof ApiError ? setMainError.message : "대표이미지 지정에 실패했습니다.", "error");
    }
  }

  async function handleDeleteImage(imageId) {
    try {
      await adminProductImageApi.delete(productId, imageId);
      setImages((current) => current.filter((image) => image.id !== imageId));
      showToast("이미지를 삭제했습니다.");
    } catch (deleteError) {
      showToast(deleteError instanceof ApiError ? deleteError.message : "이미지 삭제에 실패했습니다.", "error");
    }
  }

  async function handleMoveImage(index, direction) {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= images.length) return;
    const reordered = [...images];
    [reordered[index], reordered[targetIndex]] = [reordered[targetIndex], reordered[index]];
    setImages(reordered);
    try {
      const updated = await adminProductImageApi.reorder(productId, reordered.map((image) => image.id));
      setImages(updated);
    } catch (reorderError) {
      showToast(reorderError instanceof ApiError ? reorderError.message : "순서 변경에 실패했습니다.", "error");
      loadProduct();
    }
  }

  async function handleAdjustSubmit(event) {
    event.preventDefault();
    if (adjusting) return;
    const quantity = Number(adjustForm.quantity);
    if (!quantity || quantity <= 0 || !adjustForm.reason.trim()) {
      showToast("수량과 사유를 올바르게 입력해 주세요.", "error");
      return;
    }
    setAdjusting(true);
    try {
      await adminInventoryApi.adjust(productId, { type: adjustForm.type, quantity, reason: adjustForm.reason.trim() });
      showToast("재고를 조정했습니다.");
      setAdjustForm({ type: "INCREASE", quantity: "1", reason: "" });
      loadInventory();
      setHistoryPageNumber(0);
      loadHistory();
      loadProduct();
    } catch (adjustError) {
      showToast(adjustError instanceof ApiError ? adjustError.message : "재고 조정에 실패했습니다.", "error");
    } finally {
      setAdjusting(false);
    }
  }

  if (status === "loading") return <Loading />;
  if (status === "error") {
    return (
      <div className="admin-page">
        <ErrorState message="상품 정보를 불러오지 못했습니다." onRetry={loadProduct} />
      </div>
    );
  }

  return (
    <div className="admin-page">
      <h1 className="page-title">{isEdit ? "상품 수정" : "상품 등록"}</h1>

      <form className="admin-form" onSubmit={handleSubmit} noValidate>
        <label htmlFor="product-brand">브랜드</label>
        <select id="product-brand" value={form.brandId} onChange={(event) => setForm({ ...form, brandId: event.target.value })}>
          <option value="">선택</option>
          {brands.map((brand) => (
            <option key={brand.id} value={brand.id}>
              {brand.displayName}
            </option>
          ))}
        </select>

        <label htmlFor="product-category">카테고리</label>
        <select id="product-category" value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
          <option value="">선택</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>

        <label htmlFor="product-code">상품 코드</label>
        <input id="product-code" value={form.productCode} onChange={(event) => setForm({ ...form, productCode: event.target.value })} maxLength={50} />

        <label htmlFor="product-name">상품명</label>
        <input id="product-name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} maxLength={200} />

        <label htmlFor="product-short-desc">짧은 설명</label>
        <input
          id="product-short-desc"
          value={form.shortDescription}
          onChange={(event) => setForm({ ...form, shortDescription: event.target.value })}
          maxLength={500}
        />

        <label htmlFor="product-desc">상세 설명</label>
        <textarea id="product-desc" rows={6} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />

        <label htmlFor="product-original-price">정상가격 (원)</label>
        <input
          id="product-original-price"
          type="number"
          min={0}
          value={form.originalPrice}
          onChange={(event) => setForm({ ...form, originalPrice: event.target.value })}
        />

        <label htmlFor="product-sale-price">판매가격 (원)</label>
        <input
          id="product-sale-price"
          type="number"
          min={0}
          value={form.salePrice}
          onChange={(event) => setForm({ ...form, salePrice: event.target.value })}
        />

        <label htmlFor="product-stock">
          재고수량{isEdit && " (정확한 이력 추적이 필요하면 아래 재고 조정을 이용하세요)"}
        </label>
        <input
          id="product-stock"
          type="number"
          min={0}
          value={form.stockQuantity}
          onChange={(event) => setForm({ ...form, stockQuantity: event.target.value })}
        />

        <label htmlFor="product-status">상태</label>
        <select id="product-status" value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <label className="admin-form__checkbox">
          <input type="checkbox" checked={form.featured} onChange={(event) => setForm({ ...form, featured: event.target.checked })} />
          추천 상품으로 노출
        </label>

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        <div className="admin-form__actions">
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            {isEdit ? "저장" : "등록"}
          </button>
        </div>
      </form>

      {isEdit && (
        <>
          <section className="admin-detail-section">
            <h2>상품 이미지</h2>
            <ul className="admin-image-list">
              {images.map((image, index) => (
                <li key={image.id} className="admin-image-list__item">
                  <img src={image.imageUrl} alt="" />
                  <div className="admin-image-list__meta">
                    <span>{image.imageType === "MAIN" ? "대표" : "상세"}</span>
                    <span>{image.displayOrder}</span>
                  </div>
                  <div className="admin-image-list__actions">
                    <button type="button" className="btn btn--secondary" onClick={() => handleMoveImage(index, -1)} disabled={index === 0}>
                      ↑
                    </button>
                    <button type="button" className="btn btn--secondary" onClick={() => handleMoveImage(index, 1)} disabled={index === images.length - 1}>
                      ↓
                    </button>
                    <button type="button" className="btn btn--secondary" onClick={() => handleSetMain(image.id)}>
                      대표 지정
                    </button>
                    <button type="button" className="btn btn--danger" onClick={() => handleDeleteImage(image.id)}>
                      삭제
                    </button>
                  </div>
                </li>
              ))}
              {images.length === 0 && <li className="admin-table__empty">등록된 이미지가 없습니다.</li>}
            </ul>

            <form className="admin-form admin-form--inline" onSubmit={handleUpload}>
              <input type="file" accept="image/*" onChange={(event) => setUploadFile(event.target.files?.[0] || null)} />
              <select value={uploadType} onChange={(event) => setUploadType(event.target.value)}>
                <option value="MAIN">대표</option>
                <option value="DETAIL">상세</option>
              </select>
              <button type="submit" className="btn btn--secondary" disabled={uploading || !uploadFile}>
                업로드
              </button>
            </form>
          </section>

          <section className="admin-detail-section">
            <h2>재고 관리</h2>
            {inventory && (
              <p>
                현재 재고: <strong>{inventory.stockQuantity}</strong>개
              </p>
            )}

            <form className="admin-form admin-form--inline" onSubmit={handleAdjustSubmit}>
              <select value={adjustForm.type} onChange={(event) => setAdjustForm({ ...adjustForm, type: event.target.value })}>
                <option value="INCREASE">입고 (증가)</option>
                <option value="DECREASE">출고 (감소)</option>
              </select>
              <input
                type="number"
                min={1}
                value={adjustForm.quantity}
                onChange={(event) => setAdjustForm({ ...adjustForm, quantity: event.target.value })}
                aria-label="조정 수량"
              />
              <input
                placeholder="조정 사유"
                value={adjustForm.reason}
                onChange={(event) => setAdjustForm({ ...adjustForm, reason: event.target.value })}
                maxLength={200}
              />
              <button type="submit" className="btn btn--secondary" disabled={adjusting}>
                조정
              </button>
            </form>

            {historyPage && (
              <>
                <div className="admin-table-wrapper">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th>구분</th>
                        <th>변경 전</th>
                        <th>변경량</th>
                        <th>변경 후</th>
                        <th>사유</th>
                        <th>일시</th>
                      </tr>
                    </thead>
                    <tbody>
                      {historyPage.content.map((entry, index) => (
                        <tr key={index}>
                          <td>{CHANGE_TYPE_LABEL[entry.type] || entry.type}</td>
                          <td>{entry.quantityBefore}</td>
                          <td>{entry.changeQuantity > 0 ? `+${entry.changeQuantity}` : entry.changeQuantity}</td>
                          <td>{entry.quantityAfter}</td>
                          <td>{entry.reason}</td>
                          <td>{formatDateTime(entry.createdAt)}</td>
                        </tr>
                      ))}
                      {historyPage.content.length === 0 && (
                        <tr>
                          <td colSpan={6} className="admin-table__empty">
                            재고 변경 이력이 없습니다.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
                <Pagination page={historyPage.page} totalPages={historyPage.totalPages} onPageChange={setHistoryPageNumber} />
              </>
            )}
          </section>
        </>
      )}
    </div>
  );
}
