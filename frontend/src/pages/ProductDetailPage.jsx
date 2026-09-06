import { useCallback, useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { cartApi } from "../api/cartApi";
import { reviewApi, questionApi } from "../api/communityApi";
import { productApi } from "../api/productApi";
import { wishlistApi } from "../api/wishlistApi";
import { ConfirmDialog } from "../components/common/ConfirmDialog";
import { EmptyState } from "../components/common/EmptyState";
import { ErrorState } from "../components/common/ErrorState";
import { Loading } from "../components/common/Loading";
import { StarRatingInput } from "../components/common/StarRatingInput";
import { WishlistButton } from "../components/product/WishlistButton";
import { useAuth } from "../context/useAuth";
import { useToast } from "../context/useToast";
import { ApiError } from "../api/apiClient";
import { formatDateTime, formatPrice } from "../utils/format";

const PLACEHOLDER_IMAGE = "/placeholder-product.svg";
const REVIEW_FORM = { orderItemId: "", rating: 5, content: "" };
const QUESTION_FORM = { title: "", content: "", secret: false };

export function ProductDetailPage() {
  const { productId } = useParams();
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const [product, setProduct] = useState(null);
  const [status, setStatus] = useState("loading");
  const [quantity, setQuantity] = useState(1);
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [wishlistState, setWishlistState] = useState({ wishlisted: false, wishlistCount: 0 });
  const [reviews, setReviews] = useState(null);
  const [questions, setQuestions] = useState(null);
  const [reviewForm, setReviewForm] = useState(REVIEW_FORM);
  const [questionForm, setQuestionForm] = useState(QUESTION_FORM);
  const [communityError, setCommunityError] = useState("");
  const [pendingDelete, setPendingDelete] = useState(null);
  const [expandedQuestionIds, setExpandedQuestionIds] = useState(() => new Set());

  const loadCommunity = useCallback(() => {
    reviewApi.findProductReviews(productId).then(setReviews).catch(() => setReviews({ content: [] }));
    questionApi.findProductQuestions(productId).then(setQuestions).catch(() => setQuestions({ content: [] }));
  }, [productId]);

  useEffect(() => {
    setStatus("loading");
    setQuantity(1);
    setActiveImageIndex(0);
    productApi
      .findProduct(productId)
      .then((data) => {
        setProduct(data);
        setWishlistState((current) => ({ ...current, wishlistCount: data.wishlistCount || 0 }));
        setStatus("ready");
        document.title = `${data.name} - TunaGiftSet`;
      })
      .catch(() => setStatus("error"));
    loadCommunity();
  }, [productId, loadCommunity]);

  useEffect(() => {
    if (!isAuthenticated) {
      setWishlistState((current) => ({ ...current, wishlisted: false }));
      return;
    }
    wishlistApi
      .findStatus(productId)
      .then((data) => setWishlistState(data))
      .catch(() => setWishlistState((current) => ({ ...current, wishlisted: false })));
  }, [productId, isAuthenticated]);

  if (status === "loading") return <Loading />;
  if (status === "error" || !product) {
    return (
      <div className="container section">
        <ErrorState message="상품을 불러오지 못했습니다." onRetry={() => navigate(0)} />
      </div>
    );
  }

  const isSoldOut = product.soldOut;
  const images = product.images && product.images.length > 0 ? product.images : [];
  const activeImage = images[activeImageIndex]?.imageUrl || PLACEHOLDER_IMAGE;

  function requireLoginThenRun(action) {
    if (!isAuthenticated) {
      showToast("로그인이 필요한 기능입니다.", "info");
      navigate("/login", { state: { from: location } });
      return;
    }
    action();
  }

  function changeQuantity(delta) {
    setQuantity((current) => Math.min(Math.max(current + delta, 1), product.stockQuantity));
  }

  async function handleAddToCart() {
    requireLoginThenRun(async () => {
      setSubmitting(true);
      try {
        await cartApi.addItem(product.id, quantity);
        showToast("장바구니에 상품을 담았습니다.");
      } catch (error) {
        showToast(describeCartError(error), "error");
      } finally {
        setSubmitting(false);
      }
    });
  }

  async function handleBuyNow() {
    requireLoginThenRun(async () => {
      setSubmitting(true);
      try {
        const cart = await cartApi.addItem(product.id, quantity);
        const cartItem = cart.items.find((item) => item.productId === product.id);
        navigate("/cart", { state: { preselectCartItemId: cartItem?.cartItemId } });
      } catch (error) {
        showToast(describeCartError(error), "error");
      } finally {
        setSubmitting(false);
      }
    });
  }

  async function handleReviewSubmit(event) {
    event.preventDefault();
    requireLoginThenRun(async () => {
      setCommunityError("");
      try {
        await reviewApi.createReview(product.id, {
          orderItemId: Number(reviewForm.orderItemId),
          rating: Number(reviewForm.rating),
          content: reviewForm.content.trim(),
        });
        setReviewForm(REVIEW_FORM);
        showToast("리뷰를 등록했습니다.");
        loadCommunity();
        productApi.findProduct(productId).then(setProduct);
      } catch (error) {
        setCommunityError(error instanceof ApiError ? error.message : "리뷰를 등록하지 못했습니다.");
      }
    });
  }

  function handleReviewDelete(reviewId) {
    requireLoginThenRun(() => setPendingDelete({ type: "review", id: reviewId }));
  }

  async function handleConfirmedDelete() {
    const target = pendingDelete;
    setPendingDelete(null);
    if (!target) return;

    try {
      if (target.type === "review") {
        await reviewApi.deleteReview(target.id);
        showToast("리뷰를 삭제했습니다.");
        loadCommunity();
        productApi.findProduct(productId).then(setProduct);
      } else {
        await questionApi.deleteQuestion(target.id);
        showToast("상품문의를 삭제했습니다.");
        loadCommunity();
      }
    } catch (error) {
      const fallback = target.type === "review" ? "리뷰를 삭제하지 못했습니다." : "상품문의를 삭제하지 못했습니다.";
      showToast(error instanceof ApiError ? error.message : fallback, "error");
    }
  }

  async function handleQuestionSubmit(event) {
    event.preventDefault();
    requireLoginThenRun(async () => {
      setCommunityError("");
      try {
        await questionApi.createQuestion(product.id, {
          title: questionForm.title.trim(),
          content: questionForm.content.trim(),
          secret: questionForm.secret,
        });
        setQuestionForm(QUESTION_FORM);
        showToast("상품문의를 등록했습니다.");
        loadCommunity();
      } catch (error) {
        setCommunityError(error instanceof ApiError ? error.message : "상품문의를 등록하지 못했습니다.");
      }
    });
  }

  function handleQuestionDelete(questionId) {
    requireLoginThenRun(() => setPendingDelete({ type: "question", id: questionId }));
  }

  function toggleQuestionExpanded(questionId) {
    setExpandedQuestionIds((current) => {
      const next = new Set(current);
      if (next.has(questionId)) {
        next.delete(questionId);
      } else {
        next.add(questionId);
      }
      return next;
    });
  }

  return (
    <div className="container section product-detail-page">
      <p className="breadcrumb">홈 / {product.categoryName || "상품"} / 상품상세</p>

      <div className="product-detail">
        <div className="product-detail__gallery">
          <div className="product-detail__main-image-frame">
            <img
              key={activeImageIndex}
              src={activeImage}
              alt={product.name}
              className="product-detail__main-image"
              onError={(event) => {
                event.currentTarget.onerror = null;
                event.currentTarget.src = PLACEHOLDER_IMAGE;
              }}
            />
          </div>
          {images.length > 1 && (
            <div className="product-detail__thumbnails" aria-label="상품 이미지 목록">
              {images.map((image, index) => (
                <button
                  key={image.id}
                  type="button"
                  className={`product-detail__thumbnail${index === activeImageIndex ? " product-detail__thumbnail--active" : ""}`}
                  onClick={() => setActiveImageIndex(index)}
                  aria-label={`${index + 1}번째 이미지 보기`}
                >
                  <img src={image.imageUrl} alt="" />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="product-detail__info">
          {product.brandDisplayName && <p className="product-detail__brand">{product.brandDisplayName}</p>}
          <h1 className="product-detail__name">{product.name}</h1>
          <p className="product-detail__meta">상품코드 {product.productCode} · {product.categoryName}</p>
          <p className="product-detail__rating" aria-label={`평균 평점 ${product.averageRating || 0}점, 리뷰 ${product.reviewCount || 0}개`}>
            ★ {Number(product.averageRating || 0).toFixed(1)} <span>리뷰 {product.reviewCount || 0}개</span>
          </p>

          <div className="product-detail__wishlist">
            <WishlistButton
              productId={product.id}
              initialWishlisted={wishlistState.wishlisted}
              initialCount={wishlistState.wishlistCount || product.wishlistCount || 0}
              onChange={setWishlistState}
            />
          </div>

          <div className="product-detail__price">
            {product.discountRate > 0 && <span className="product-detail__original-price">{formatPrice(product.originalPrice)}</span>}
            <span className="product-detail__sale-price">
              {product.discountRate > 0 && <span className="product-card__discount">{product.discountRate}%</span>}
              {formatPrice(product.salePrice)}
            </span>
          </div>

          {product.shortDescription && <p className="product-detail__short-description">{product.shortDescription}</p>}

          <dl className="product-detail__notice">
            <div>
              <dt>배송</dt>
              <dd>주문서에서 배송비를 최종 확인합니다.</dd>
            </div>
            <div>
              <dt>재고</dt>
              <dd>{isSoldOut ? "품절" : `${product.stockQuantity}개 구매 가능`}</dd>
            </div>
          </dl>

          {isSoldOut ? (
            <p className="product-detail__soldout">품절된 상품입니다.</p>
          ) : (
            <>
              <div className="quantity-selector">
                <span>수량</span>
                <button type="button" onClick={() => changeQuantity(-1)} aria-label="수량 감소" disabled={quantity <= 1}>-</button>
                <input type="text" value={quantity} readOnly aria-label="선택한 수량" />
                <button type="button" onClick={() => changeQuantity(1)} aria-label="수량 증가" disabled={quantity >= product.stockQuantity}>+</button>
              </div>
              <p className="product-detail__total">총 상품금액 <strong>{formatPrice(Number(product.salePrice) * quantity)}</strong></p>
              <div className="product-detail__actions">
                <button type="button" className="btn btn--secondary btn--large" onClick={handleAddToCart} disabled={submitting}>
                  장바구니 담기
                </button>
                <button type="button" className="btn btn--primary btn--large" onClick={handleBuyNow} disabled={submitting}>
                  구매하기
                </button>
              </div>
            </>
          )}
        </div>
      </div>

      <nav className="detail-tabs" aria-label="상품 상세 섹션">
        <a href="#product-info">상품정보</a>
        <a href="#product-reviews">리뷰</a>
        <a href="#product-questions">상품문의</a>
      </nav>

      <section id="product-info" className="product-detail__description">
        <h2>상품정보</h2>
        <p>{product.description || product.shortDescription || "등록된 상세 설명이 없습니다."}</p>
      </section>

      <section id="product-reviews" className="community-section">
        <div className="section-header">
          <div>
            <h2>고객 리뷰</h2>
            <p className="section__description">실제 구매자가 남긴 후기를 확인해 보세요.</p>
          </div>
          <div className="review-summary">★ {Number(product.averageRating || 0).toFixed(1)} / 5 · 리뷰 {product.reviewCount || 0}개</div>
        </div>
        {reviews?.content?.length > 0 ? (
          <ul className="community-list">
            {reviews.content.map((review) => (
              <li key={review.id} className="community-item">
                <div className="community-item__header">
                  <strong>{"★".repeat(review.rating)}{"☆".repeat(5 - review.rating)}</strong>
                  <span>{review.authorName} · 구매확인 · {formatDateTime(review.createdAt)}</span>
                </div>
                <p>{review.content}</p>
                {review.editable && (
                  <button type="button" className="link-button" onClick={() => handleReviewDelete(review.id)}>삭제</button>
                )}
              </li>
            ))}
          </ul>
        ) : (
          <EmptyState message="아직 작성된 리뷰가 없습니다." />
        )}
        <form className="community-form" onSubmit={handleReviewSubmit}>
          <h3>리뷰 작성</h3>
          <label htmlFor="reviewOrderItemId">주문상품 번호</label>
          <input id="reviewOrderItemId" value={reviewForm.orderItemId} onChange={(event) => setReviewForm((current) => ({ ...current, orderItemId: event.target.value }))} />
          <label htmlFor="reviewRating">평점</label>
          <StarRatingInput
            id="reviewRating"
            value={reviewForm.rating}
            onChange={(rating) => setReviewForm((current) => ({ ...current, rating }))}
          />
          <label htmlFor="reviewContent">내용</label>
          <textarea id="reviewContent" value={reviewForm.content} onChange={(event) => setReviewForm((current) => ({ ...current, content: event.target.value }))} minLength={10} maxLength={2000} />
          <button type="submit" className="btn btn--primary">리뷰 등록</button>
        </form>
      </section>

      <section id="product-questions" className="community-section">
        <div className="section-header">
          <div>
            <h2>상품문의</h2>
            <p className="section__description">상품 구성이나 배송 전 확인할 내용을 문의할 수 있습니다.</p>
          </div>
        </div>
        {questions?.content?.length > 0 ? (
          <ul className="community-list">
            {questions.content.map((question) => {
              const expanded = expandedQuestionIds.has(question.id);
              const panelId = `question-panel-${question.id}`;
              return (
                <li key={question.id} className="community-item">
                  <button
                    type="button"
                    className="community-item__toggle"
                    aria-expanded={expanded}
                    aria-controls={panelId}
                    onClick={() => toggleQuestionExpanded(question.id)}
                  >
                    <span>
                      <div className="community-item__header">
                        <strong>{question.secret ? "비밀글" : "공개글"} · {question.status === "ANSWERED" ? "답변완료" : "답변대기"}</strong>
                        <span>{question.authorName} · {formatDateTime(question.createdAt)}</span>
                      </div>
                      <h3>{question.title}</h3>
                    </span>
                    <span className="community-item__toggle-icon" aria-hidden="true">▾</span>
                  </button>
                  <div id={panelId} className={`community-item__collapsible${expanded ? " is-expanded" : ""}`}>
                    <div className="community-item__collapsible-inner">
                      {question.content ? <p>{question.content}</p> : <p className="muted">비밀글입니다.</p>}
                      {question.answer && <div className="question-answer"><strong>판매자 답변</strong><p>{question.answer.content}</p></div>}
                      {question.editable && (
                        <button type="button" className="link-button" onClick={() => handleQuestionDelete(question.id)}>삭제</button>
                      )}
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
        ) : (
          <EmptyState message="등록된 상품문의가 없습니다." />
        )}
        <form className="community-form" onSubmit={handleQuestionSubmit}>
          <h3>상품문의 작성</h3>
          <label htmlFor="questionTitle">제목</label>
          <input id="questionTitle" value={questionForm.title} onChange={(event) => setQuestionForm((current) => ({ ...current, title: event.target.value }))} maxLength={150} />
          <label htmlFor="questionContent">내용</label>
          <textarea id="questionContent" value={questionForm.content} onChange={(event) => setQuestionForm((current) => ({ ...current, content: event.target.value }))} maxLength={2000} />
          <label className="community-form__checkbox">
            <input type="checkbox" checked={questionForm.secret} onChange={(event) => setQuestionForm((current) => ({ ...current, secret: event.target.checked }))} />
            비밀글
          </label>
          <button type="submit" className="btn btn--primary">문의 등록</button>
        </form>
      </section>

      {communityError && <p className="form-error" role="alert">{communityError}</p>}

      <ConfirmDialog
        open={pendingDelete !== null}
        title={pendingDelete?.type === "review" ? "리뷰를 삭제하시겠습니까?" : "상품문의를 삭제하시겠습니까?"}
        confirmLabel="삭제"
        onConfirm={handleConfirmedDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}

function describeCartError(error) {
  if (error instanceof ApiError) {
    if (error.code === "INSUFFICIENT_STOCK" || error.code === "PRODUCT_SOLD_OUT") {
      return "재고가 부족해 담을 수 없습니다.";
    }
    return error.message;
  }
  return "장바구니에 담지 못했습니다. 잠시 후 다시 시도해 주세요.";
}
