import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { wishlistApi } from "../api/wishlistApi";
import { EmptyState } from "../components/common/EmptyState";
import { ErrorState } from "../components/common/ErrorState";
import { EmptyWishlistIcon } from "../components/common/icons/StateIcons";
import { ProductGridSkeleton } from "../components/common/Skeleton";
import { Pagination } from "../components/common/Pagination";
import { PageHeader } from "../components/common/PageHeader";
import { ProductCard } from "../components/product/ProductCard";

export function MyWishlistPage() {
  const [pageData, setPageData] = useState(null);
  const [page, setPage] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [status, setStatus] = useState("loading");

  useEffect(() => {
    setStatus("loading");
    wishlistApi
      .findMyWishlist({ page, size: 12 })
      .then((data) => {
        setPageData(data);
        setStatus("ready");
        document.title = "찜한 상품 - SeyoungGiftSet";
      })
      .catch(() => setStatus("error"));
  }, [page, reloadKey]);

  function handleWishlistChange(productId, response) {
    if (response.wishlisted) return;
    setPageData((current) => {
      if (!current) return current;
      return {
        ...current,
        content: current.content.filter((item) => item.productId !== productId),
        totalElements: Math.max(0, current.totalElements - 1),
      };
    });
  }

  return (
    <div className="container section wishlist-page">
      <PageHeader
        breadcrumb="홈 / 마이페이지 / 찜한 상품"
        title="찜한 상품"
        description="관심 있는 선물세트를 다시 확인하고 장바구니에 담아보세요."
      />
      {status === "loading" && <ProductGridSkeleton count={12} />}
      {status === "error" && (
        <ErrorState message="찜 목록을 불러오지 못했습니다." onRetry={() => setReloadKey((current) => current + 1)} />
      )}
      {status === "ready" && pageData.content.length === 0 && (
        <EmptyState
          icon={EmptyWishlistIcon}
          message="찜한 상품이 없습니다."
          action={<Link to="/products" className="btn btn--primary">상품 둘러보기</Link>}
        />
      )}
      {status === "ready" && pageData.content.length > 0 && (
        <>
          <div className="product-grid">
            {pageData.content.map((item) => (
              <ProductCard
                key={item.productId}
                product={toProductCardItem(item)}
                enableWishlist
                initialWishlisted
                onWishlistChange={(response) => handleWishlistChange(item.productId, response)}
              />
            ))}
          </div>
          <Pagination page={pageData.page} totalPages={pageData.totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}

function toProductCardItem(item) {
  return {
    id: item.productId,
    name: item.productName,
    brandDisplayName: item.brandDisplayName,
    categoryName: item.categoryName,
    thumbnailImageUrl: item.thumbnailImageUrl,
    originalPrice: item.originalPrice,
    salePrice: item.salePrice,
    stockQuantity: item.stockQuantity,
    status: item.status,
    featured: false,
    wishlistCount: 1,
  };
}
