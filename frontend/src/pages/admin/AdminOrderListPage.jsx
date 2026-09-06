import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { adminOrderApi } from "../../api/adminApi";
import { Loading } from "../../components/common/Loading";
import { ErrorState } from "../../components/common/ErrorState";
import { Pagination } from "../../components/common/Pagination";
import { formatDateTime, formatOrderStatus, formatPaymentStatus, formatDeliveryStatus, formatPrice } from "../../utils/format";

const ORDER_STATUS_OPTIONS = ["PAYMENT_PENDING", "PAID", "PREPARING", "SHIPPING", "DELIVERED", "CANCELED", "EXPIRED"];
const PAYMENT_STATUS_OPTIONS = ["READY", "IN_PROGRESS", "PAID", "CANCELED", "FAILED"];
const DELIVERY_STATUS_OPTIONS = ["READY", "PREPARING", "SHIPPING", "DELIVERED", "RETURNED", "CANCELED"];

export function AdminOrderListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get("page") || 0);
  const filters = {
    orderNumber: searchParams.get("orderNumber") || "",
    memberEmail: searchParams.get("memberEmail") || "",
    recipientName: searchParams.get("recipientName") || "",
    recipientPhone: searchParams.get("recipientPhone") || "",
    orderStatus: searchParams.get("orderStatus") || "",
    paymentStatus: searchParams.get("paymentStatus") || "",
    deliveryStatus: searchParams.get("deliveryStatus") || "",
    startDate: searchParams.get("startDate") || "",
    endDate: searchParams.get("endDate") || "",
  };

  const [draft, setDraft] = useState(filters);
  const [pageData, setPageData] = useState(null);
  const [status, setStatus] = useState("loading");

  function load() {
    setStatus("loading");
    adminOrderApi
      .findOrders({ ...filters, page, size: 20 })
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

  function resetFilters() {
    setDraft({
      orderNumber: "",
      memberEmail: "",
      recipientName: "",
      recipientPhone: "",
      orderStatus: "",
      paymentStatus: "",
      deliveryStatus: "",
      startDate: "",
      endDate: "",
    });
    setSearchParams({});
  }

  return (
    <div className="admin-page">
      <h1 className="page-title">주문 관리</h1>

      <form className="admin-filter-bar admin-filter-bar--wrap" onSubmit={applyFilters}>
        <input placeholder="주문번호" value={draft.orderNumber} onChange={(event) => setDraft({ ...draft, orderNumber: event.target.value })} />
        <input placeholder="주문자 이메일" value={draft.memberEmail} onChange={(event) => setDraft({ ...draft, memberEmail: event.target.value })} />
        <input placeholder="수령인" value={draft.recipientName} onChange={(event) => setDraft({ ...draft, recipientName: event.target.value })} />
        <input placeholder="수령인 연락처" value={draft.recipientPhone} onChange={(event) => setDraft({ ...draft, recipientPhone: event.target.value })} />
        <select value={draft.orderStatus} onChange={(event) => setDraft({ ...draft, orderStatus: event.target.value })}>
          <option value="">주문상태 전체</option>
          {ORDER_STATUS_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {formatOrderStatus(option)}
            </option>
          ))}
        </select>
        <select value={draft.paymentStatus} onChange={(event) => setDraft({ ...draft, paymentStatus: event.target.value })}>
          <option value="">결제상태 전체</option>
          {PAYMENT_STATUS_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {formatPaymentStatus(option)}
            </option>
          ))}
        </select>
        <select value={draft.deliveryStatus} onChange={(event) => setDraft({ ...draft, deliveryStatus: event.target.value })}>
          <option value="">배송상태 전체</option>
          {DELIVERY_STATUS_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {formatDeliveryStatus(option)}
            </option>
          ))}
        </select>
        <input type="date" value={draft.startDate} onChange={(event) => setDraft({ ...draft, startDate: event.target.value })} />
        <span className="admin-filter-bar__sep">~</span>
        <input type="date" value={draft.endDate} onChange={(event) => setDraft({ ...draft, endDate: event.target.value })} />
        <button type="submit" className="btn btn--primary">
          검색
        </button>
        <button type="button" className="btn btn--secondary" onClick={resetFilters}>
          초기화
        </button>
      </form>

      {status === "loading" && <Loading />}
      {status === "error" && <ErrorState message="주문 목록을 불러오지 못했습니다." onRetry={load} />}

      {status === "ready" && pageData && (
        <>
          <div className="admin-table-wrapper">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>주문번호</th>
                  <th>주문자</th>
                  <th>상품</th>
                  <th>결제금액</th>
                  <th>주문상태</th>
                  <th>결제상태</th>
                  <th>배송상태</th>
                  <th>주문일시</th>
                </tr>
              </thead>
              <tbody>
                {pageData.content.map((order) => (
                  <tr key={order.orderNumber}>
                    <td>
                      <Link to={`/admin/orders/${order.orderNumber}`}>{order.orderNumber}</Link>
                    </td>
                    <td>{order.memberEmail}</td>
                    <td>
                      {order.representativeProductName} ({order.itemCount})
                    </td>
                    <td>{formatPrice(order.totalAmount)}</td>
                    <td>{formatOrderStatus(order.orderStatus)}</td>
                    <td>{formatPaymentStatus(order.paymentStatus)}</td>
                    <td>{formatDeliveryStatus(order.deliveryStatus)}</td>
                    <td>{formatDateTime(order.orderedAt)}</td>
                  </tr>
                ))}
                {pageData.content.length === 0 && (
                  <tr>
                    <td colSpan={8} className="admin-table__empty">
                      조건에 맞는 주문이 없습니다.
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
