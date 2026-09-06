import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { orderApi } from "../api/orderApi";
import { ListRowSkeleton } from "../components/common/Skeleton";
import { EmptyState } from "../components/common/EmptyState";
import { ErrorState } from "../components/common/ErrorState";
import { Pagination } from "../components/common/Pagination";
import { PageHeader } from "../components/common/PageHeader";
import { formatDateTime, formatOrderStatus, formatPrice } from "../utils/format";

export function MyOrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get("page") || 0);
  const [pageData, setPageData] = useState(null);
  const [status, setStatus] = useState("loading");

  useEffect(() => {
    document.title = "주문내역 - SeyoungGiftSet";
    setStatus("loading");
    orderApi
      .findOrders(page, 10)
      .then((data) => {
        setPageData(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, [page]);

  return (
    <div className="container section orders-page">
      <PageHeader
        breadcrumb="홈 / 마이페이지 / 주문내역"
        title="주문내역"
        description="주문 상태와 결제금액을 확인할 수 있습니다."
      />

      {status === "loading" && <ListRowSkeleton count={4} />}
      {status === "error" && <ErrorState message="주문내역을 불러오지 못했습니다." />}
      {status === "ready" && pageData.content.length === 0 && (
        <EmptyState
          message="주문 내역이 없습니다."
          action={<Link to="/products" className="btn btn--primary">상품 보러가기</Link>}
        />
      )}

      {status === "ready" && pageData.content.length > 0 && (
        <>
          <ul className="order-list">
            {pageData.content.map((order) => (
              <li key={order.orderNumber} className="order-list__item">
                <Link to={`/mypage/orders/${order.orderNumber}`} className="order-list__link">
                  <div className="order-list__header">
                    <span>{formatDateTime(order.orderedAt)}</span>
                    <span className="order-list__status">{formatOrderStatus(order.orderStatus)}</span>
                  </div>
                  <div className="order-list__body">
                    <span className="order-list__product">{order.representativeProductName}</span>
                    <span className="order-list__amount">{formatPrice(order.totalAmount)}</span>
                  </div>
                  <span className="order-list__number">주문번호 {order.orderNumber}</span>
                </Link>
              </li>
            ))}
          </ul>
          <Pagination page={pageData.page} totalPages={pageData.totalPages} onPageChange={(nextPage) => setSearchParams({ page: nextPage })} />
        </>
      )}
    </div>
  );
}
