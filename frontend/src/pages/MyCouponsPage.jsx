import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { couponApi } from "../api/couponApi";
import { ListRowSkeleton } from "../components/common/Skeleton";
import { EmptyState } from "../components/common/EmptyState";
import { ErrorState } from "../components/common/ErrorState";
import { Pagination } from "../components/common/Pagination";
import { classifyMemberCoupon, formatCouponCondition, formatCouponDiscount, formatDateTime } from "../utils/format";

const TABS = [
  { key: "AVAILABLE", label: "사용 가능" },
  { key: "USED", label: "사용 완료" },
  { key: "EXPIRED", label: "기간 만료" },
];

const STATUS_BADGE_LABEL = {
  AVAILABLE: "사용 가능",
  RESERVED: "주문에 적용중",
  USED: "사용 완료",
  EXPIRED: "기간 만료",
};

export function MyCouponsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get("page") || 0);
  const activeTab = searchParams.get("tab") || "AVAILABLE";

  const [pageData, setPageData] = useState(null);
  const [status, setStatus] = useState("loading");

  useEffect(() => {
    document.title = "쿠폰함 - SeyoungGiftSet";
    setStatus("loading");
    couponApi
      .findMyCoupons(page, 50)
      .then((data) => {
        setPageData(data);
        setStatus("ready");
      })
      .catch(() => setStatus("error"));
  }, [page]);

  function selectTab(tab) {
    setSearchParams({ tab });
  }

  const classified = pageData ? pageData.content.map((coupon) => ({ ...coupon, group: classifyMemberCoupon(coupon) })) : [];
  const visibleCoupons = classified.filter((coupon) => coupon.group === activeTab);

  return (
    <div className="container section coupon-wallet-page">
      <div className="page-header">
        <div>
          <p className="breadcrumb">홈 / 마이페이지 / 쿠폰함</p>
          <h1 className="page-title">내 쿠폰함</h1>
          <p>주문서에서 사용할 수 있는 쿠폰과 유효기간을 확인하세요.</p>
        </div>
      </div>

      <div className="coupon-wallet-page__tabs" role="tablist">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.key}
            className={`coupon-wallet-page__tab${activeTab === tab.key ? " is-active" : ""}`}
            onClick={() => selectTab(tab.key)}
          >
            {tab.label}
            {status === "ready" && (
              <span className="coupon-wallet-page__tab-count">
                {classified.filter((coupon) => coupon.group === tab.key).length}
              </span>
            )}
          </button>
        ))}
      </div>

      {status === "loading" && <ListRowSkeleton count={3} height={110} />}
      {status === "error" && <ErrorState message="쿠폰함을 불러오지 못했습니다." />}
      {status === "ready" && visibleCoupons.length === 0 && <EmptyState message="해당 쿠폰이 없습니다." />}

      {status === "ready" && visibleCoupons.length > 0 && (
        <ul className="coupon-wallet-list">
          {visibleCoupons.map((coupon) => (
            <li key={coupon.memberCouponId} className={`coupon-card${coupon.group !== "AVAILABLE" ? " coupon-card--inactive" : ""}`}>
              <div className="coupon-card__header">
                <span className="coupon-card__name">{coupon.name}</span>
                <span className="coupon-card__badge">{STATUS_BADGE_LABEL[coupon.group]}</span>
              </div>
              <p className="coupon-card__discount">{formatCouponDiscount(coupon)}</p>
              <p className="coupon-card__condition">{formatCouponCondition(coupon)}</p>
              <p className="coupon-card__period">
                {formatDateTime(coupon.validFrom)} ~ {formatDateTime(coupon.validUntil)}
              </p>
            </li>
          ))}
        </ul>
      )}

      {status === "ready" && pageData.totalPages > 1 && (
        <Pagination page={pageData.page} totalPages={pageData.totalPages} onPageChange={(nextPage) => setSearchParams({ tab: activeTab, page: nextPage })} />
      )}
    </div>
  );
}
