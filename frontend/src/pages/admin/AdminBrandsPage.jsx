import { useEffect, useState } from "react";
import { adminBrandApi } from "../../api/productApi";
import { Loading } from "../../components/common/Loading";
import { ErrorState } from "../../components/common/ErrorState";
import { useToast } from "../../context/useToast";
import { ApiError } from "../../api/apiClient";

const EMPTY_FORM = { name: "", displayName: "", active: true };

export function AdminBrandsPage() {
  const { showToast } = useToast();
  const [brands, setBrands] = useState([]);
  const [status, setStatus] = useState("loading");
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setStatus("loading");
    adminBrandApi
      .findAllBrands()
      .then((data) => {
        setBrands(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }

  useEffect(() => {
    load();
  }, []);

  function startCreate() {
    setEditingId("new");
    setForm(EMPTY_FORM);
    setError("");
  }

  function startEdit(brand) {
    setEditingId(brand.id);
    setForm({ name: brand.name, displayName: brand.displayName, active: brand.active });
    setError("");
  }

  function cancelEdit() {
    setEditingId(null);
    setError("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting) return;
    if (!form.name.trim() || !form.displayName.trim()) {
      setError("브랜드 코드와 표시명을 입력해 주세요.");
      return;
    }

    const payload = { name: form.name.trim(), displayName: form.displayName.trim(), active: form.active };

    setSubmitting(true);
    setError("");
    try {
      if (editingId === "new") {
        await adminBrandApi.createBrand(payload);
        showToast("브랜드를 등록했습니다.");
      } else {
        await adminBrandApi.updateBrand(editingId, payload);
        showToast("브랜드를 수정했습니다.");
      }
      setEditingId(null);
      load();
    } catch (submitError) {
      setError(submitError instanceof ApiError ? submitError.message : "저장하지 못했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="admin-page">
      <div className="admin-page__header">
        <h1 className="page-title">브랜드 관리</h1>
        {editingId === null && (
          <button type="button" className="btn btn--primary" onClick={startCreate}>
            브랜드 등록
          </button>
        )}
      </div>

      {editingId !== null && (
        <form className="admin-form" onSubmit={handleSubmit} noValidate>
          <h2>{editingId === "new" ? "브랜드 등록" : "브랜드 수정"}</h2>

          <label htmlFor="brand-name">브랜드 코드</label>
          <input id="brand-name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} maxLength={100} />

          <label htmlFor="brand-display-name">브랜드 표시명</label>
          <input
            id="brand-display-name"
            value={form.displayName}
            onChange={(event) => setForm({ ...form, displayName: event.target.value })}
            maxLength={100}
          />

          <label className="admin-form__checkbox">
            <input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} />
            활성화
          </label>

          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}

          <div className="admin-form__actions">
            <button type="button" className="btn btn--secondary" onClick={cancelEdit}>
              취소
            </button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              저장
            </button>
          </div>
        </form>
      )}

      {status === "loading" && <Loading />}
      {status === "error" && <ErrorState message="브랜드를 불러오지 못했습니다." onRetry={load} />}

      {status === "ready" && (
        <div className="admin-table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>브랜드 코드</th>
                <th>표시명</th>
                <th>상태</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {brands.map((brand) => (
                <tr key={brand.id}>
                  <td>{brand.id}</td>
                  <td>{brand.name}</td>
                  <td>{brand.displayName}</td>
                  <td>
                    <span className={`admin-badge${brand.active ? " admin-badge--active" : ""}`}>{brand.active ? "활성" : "비활성"}</span>
                  </td>
                  <td>
                    <button type="button" className="btn btn--secondary" onClick={() => startEdit(brand)}>
                      수정
                    </button>
                  </td>
                </tr>
              ))}
              {brands.length === 0 && (
                <tr>
                  <td colSpan={5} className="admin-table__empty">
                    등록된 브랜드가 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
