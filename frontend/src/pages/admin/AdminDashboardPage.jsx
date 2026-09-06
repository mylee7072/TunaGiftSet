import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { adminDashboardApi } from "../../api/adminApi";
import { Loading } from "../../components/common/Loading";
import { ErrorState } from "../../components/common/ErrorState";
import { formatPrice } from "../../utils/format";

export function AdminDashboardPage() {
  const [summary, setSummary] = useState(null);
  const [status, setStatus] = useState("loading");

  function load() {
    setStatus("loading");
    adminDashboardApi
      .getSummary()
      .then((data) => {
        setSummary(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="admin-dashboard">
      <h1 className="page-title">대시보드</h1>

      {status === "loading" && <Loading />}
      {status === "error" && <ErrorState message="대시보드 정보를 불러오지 못했습니다." onRetry={load} />}

      {status === "ready" && summary && (
        <div className="admin-stat-grid">
          <Link to="/admin/orders" className="admin-stat-card">
            <span className="admin-stat-card__label">오늘 주문 수</span>
            <span className="admin-stat-card__value">{summary.todayOrderCount.toLocaleString()}건</span>
          </Link>
          <div className="admin-stat-card">
            <span className="admin-stat-card__label">오늘 결제금액</span>
            <span className="admin-stat-card__value">{formatPrice(summary.todayPaidAmount)}</span>
          </div>
          <Link to="/admin/orders?orderStatus=PREPARING" className="admin-stat-card">
            <span className="admin-stat-card__label">상품 준비중 주문</span>
            <span className="admin-stat-card__value">{summary.preparingOrderCount.toLocaleString()}건</span>
          </Link>
          <Link to="/admin/orders?orderStatus=SHIPPING" className="admin-stat-card">
            <span className="admin-stat-card__label">배송중 주문</span>
            <span className="admin-stat-card__value">{summary.shippingOrderCount.toLocaleString()}건</span>
          </Link>
          <Link to="/admin/products?sort=LATEST" className="admin-stat-card admin-stat-card--warning">
            <span className="admin-stat-card__label">재고 부족 상품 (5개 이하)</span>
            <span className="admin-stat-card__value">{summary.lowStockProductCount.toLocaleString()}개</span>
          </Link>
        </div>
      )}
    </div>
  );
}
