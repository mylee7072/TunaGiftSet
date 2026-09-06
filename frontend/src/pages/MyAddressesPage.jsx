import { useCallback, useEffect, useState } from "react";
import { addressApi } from "../api/addressApi";
import { AddressForm } from "../components/address/AddressForm";
import { ConfirmDialog } from "../components/common/ConfirmDialog";
import { EmptyState } from "../components/common/EmptyState";
import { ErrorState } from "../components/common/ErrorState";
import { Loading } from "../components/common/Loading";
import { useToast } from "../context/useToast";
import { ApiError } from "../api/apiClient";
import { formatAddress, formatPhone } from "../utils/format";

export function MyAddressesPage() {
  const { showToast } = useToast();
  const [addresses, setAddresses] = useState([]);
  const [status, setStatus] = useState("loading");
  const [mode, setMode] = useState("list");
  const [editingAddress, setEditingAddress] = useState(null);
  const [pendingDeleteId, setPendingDeleteId] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const loadAddresses = useCallback(() => {
    setStatus("loading");
    return addressApi
      .findAddresses()
      .then((data) => {
        setAddresses(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, []);

  useEffect(() => {
    loadAddresses();
  }, [loadAddresses]);

  async function handleCreate(payload) {
    setSubmitting(true);
    try {
      await addressApi.createAddress(payload);
      showToast("배송지를 등록했습니다.");
      setMode("list");
      await loadAddresses();
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : "배송지를 등록하지 못했습니다.", "error");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleUpdate(payload) {
    setSubmitting(true);
    try {
      await addressApi.updateAddress(editingAddress.id, payload);
      showToast("배송지를 수정했습니다.");
      setEditingAddress(null);
      setMode("list");
      await loadAddresses();
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : "배송지를 수정하지 못했습니다.", "error");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSetDefault(addressId) {
    try {
      await addressApi.setDefaultAddress(addressId);
      showToast("기본배송지를 변경했습니다.");
      await loadAddresses();
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : "기본배송지를 변경하지 못했습니다.", "error");
    }
  }

  async function handleDeleteConfirmed() {
    const addressId = pendingDeleteId;
    setPendingDeleteId(null);
    try {
      await addressApi.deleteAddress(addressId);
      showToast("배송지를 삭제했습니다.");
      await loadAddresses();
    } catch (error) {
      showToast(error instanceof ApiError ? error.message : "배송지를 삭제하지 못했습니다.", "error");
    }
  }

  if (status === "loading" && addresses.length === 0) return <Loading />;
  if (status === "error") {
    return (
      <div className="container section">
        <ErrorState message="배송지를 불러오지 못했습니다." onRetry={loadAddresses} />
      </div>
    );
  }

  return (
    <div className="container section addresses-page">
      <div className="page-header">
        <h1 className="page-title">배송지 관리</h1>
        {mode === "list" && (
          <button type="button" className="btn btn--primary" onClick={() => setMode("create")}>
            배송지 추가
          </button>
        )}
      </div>

      {mode === "create" && (
        <section className="address-editor">
          <h2>새 배송지</h2>
          <AddressForm submitLabel="등록" submitting={submitting} onSubmit={handleCreate} onCancel={() => setMode("list")} />
        </section>
      )}

      {mode === "edit" && editingAddress && (
        <section className="address-editor">
          <h2>배송지 수정</h2>
          <AddressForm
            initialValue={editingAddress}
            submitLabel="수정"
            submitting={submitting}
            onSubmit={handleUpdate}
            onCancel={() => {
              setEditingAddress(null);
              setMode("list");
            }}
          />
        </section>
      )}

      {mode === "list" && addresses.length === 0 && (
        <EmptyState
          message="저장된 배송지가 없습니다."
          action={
            <button type="button" className="btn btn--primary" onClick={() => setMode("create")}>
              배송지 등록
            </button>
          }
        />
      )}

      {mode === "list" && addresses.length > 0 && (
        <ul className="address-list">
          {addresses.map((address) => (
            <li key={address.id} className="address-card">
              <div className="address-card__header">
                <strong>{address.addressName || "배송지"}</strong>
                {address.defaultAddress && <span className="address-card__badge">기본배송지</span>}
              </div>
              <p>{address.recipientName}</p>
              <p>{formatPhone(address.recipientPhone)}</p>
              <p className="address-card__address">{formatAddress(address)}</p>
              <div className="address-card__actions">
                {!address.defaultAddress && (
                  <button type="button" className="btn btn--secondary" onClick={() => handleSetDefault(address.id)}>
                    기본배송지로 설정
                  </button>
                )}
                <button
                  type="button"
                  className="btn btn--secondary"
                  onClick={() => {
                    setEditingAddress(address);
                    setMode("edit");
                  }}
                >
                  수정
                </button>
                <button type="button" className="btn btn--secondary" onClick={() => setPendingDeleteId(address.id)}>
                  삭제
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <ConfirmDialog
        open={pendingDeleteId !== null}
        title="배송지를 삭제하시겠습니까?"
        description="삭제한 배송지는 다시 불러올 수 없습니다. 이전 주문의 배송정보는 그대로 유지됩니다."
        confirmLabel="삭제"
        onConfirm={handleDeleteConfirmed}
        onCancel={() => setPendingDeleteId(null)}
      />
    </div>
  );
}
