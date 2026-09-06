import { useEffect, useState } from "react";
import { adminCategoryApi } from "../../api/productApi";
import { Loading } from "../../components/common/Loading";
import { ErrorState } from "../../components/common/ErrorState";
import { useToast } from "../../context/useToast";
import { ApiError } from "../../api/apiClient";

const EMPTY_FORM = { name: "", parentId: "", displayOrder: 0, active: true };

export function AdminCategoriesPage() {
  const { showToast } = useToast();
  const [categories, setCategories] = useState([]);
  const [status, setStatus] = useState("loading");
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setStatus("loading");
    adminCategoryApi
      .findAllCategories()
      .then((data) => {
        setCategories(data);
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

  function startEdit(category) {
    setEditingId(category.id);
    setForm({
      name: category.name,
      parentId: category.parentId ?? "",
      displayOrder: category.displayOrder,
      active: category.active,
    });
    setError("");
  }

  function cancelEdit() {
    setEditingId(null);
    setError("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (submitting) return;
    if (!form.name.trim()) {
      setError("카테고리명을 입력해 주세요.");
      return;
    }

    const payload = {
      parentId: form.parentId === "" ? null : Number(form.parentId),
      name: form.name.trim(),
      displayOrder: Number(form.displayOrder) || 0,
      active: form.active,
    };

    setSubmitting(true);
    setError("");
    try {
      if (editingId === "new") {
        await adminCategoryApi.createCategory(payload);
        showToast("카테고리를 등록했습니다.");
      } else {
        await adminCategoryApi.updateCategory(editingId, payload);
        showToast("카테고리를 수정했습니다.");
      }
      setEditingId(null);
      load();
    } catch (submitError) {
      setError(submitError instanceof ApiError ? submitError.message : "저장하지 못했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  const parentOptions = categories.filter((category) => category.id !== editingId);

  return (
    <div className="admin-page">
      <div className="admin-page__header">
        <h1 className="page-title">카테고리 관리</h1>
        {editingId === null && (
          <button type="button" className="btn btn--primary" onClick={startCreate}>
            카테고리 등록
          </button>
        )}
      </div>

      {editingId !== null && (
        <form className="admin-form" onSubmit={handleSubmit} noValidate>
          <h2>{editingId === "new" ? "카테고리 등록" : "카테고리 수정"}</h2>

          <label htmlFor="category-name">카테고리명</label>
          <input id="category-name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} maxLength={100} />

          <label htmlFor="category-parent">상위 카테고리</label>
          <select id="category-parent" value={form.parentId} onChange={(event) => setForm({ ...form, parentId: event.target.value })}>
            <option value="">없음 (최상위)</option>
            {parentOptions.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>

          <label htmlFor="category-order">표시 순서</label>
          <input
            id="category-order"
            type="number"
            min={0}
            value={form.displayOrder}
            onChange={(event) => setForm({ ...form, displayOrder: event.target.value })}
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
      {status === "error" && <ErrorState message="카테고리를 불러오지 못했습니다." onRetry={load} />}

      {status === "ready" && (
        <div className="admin-table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>카테고리명</th>
                <th>상위 카테고리</th>
                <th>표시 순서</th>
                <th>상태</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {categories.map((category) => (
                <tr key={category.id}>
                  <td>{category.id}</td>
                  <td>{category.name}</td>
                  <td>{categories.find((c) => c.id === category.parentId)?.name || "-"}</td>
                  <td>{category.displayOrder}</td>
                  <td>
                    <span className={`admin-badge${category.active ? " admin-badge--active" : ""}`}>
                      {category.active ? "활성" : "비활성"}
                    </span>
                  </td>
                  <td>
                    <button type="button" className="btn btn--secondary" onClick={() => startEdit(category)}>
                      수정
                    </button>
                  </td>
                </tr>
              ))}
              {categories.length === 0 && (
                <tr>
                  <td colSpan={6} className="admin-table__empty">
                    등록된 카테고리가 없습니다.
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
