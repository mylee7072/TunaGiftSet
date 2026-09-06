import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { ApiError } from "../api/apiClient";
import { cartApi } from "../api/cartApi";
import { questionApi } from "../api/communityApi";
import { wishlistApi } from "../api/wishlistApi";
import { EmptyState } from "../components/common/EmptyState";
import { ErrorState } from "../components/common/ErrorState";
import { WishlistButton } from "../components/product/WishlistButton";
import { siteConfig } from "../config/siteConfig";
import { fetchProductById } from "../data/products";
import { useAuth } from "../context/useAuth";
import { useToast } from "../context/useToast";
import { useDocumentMeta } from "../hooks/useDocumentMeta";
import { useProductJsonLd } from "../hooks/useProductJsonLd";
import { formatDateTime, formatPrice } from "../utils/format";

const PLACEHOLDER_IMAGE = "/placeholder-product.svg";
const MAX_QUANTITY = 99;

export function ProductDetailPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();

  const [product, setProduct] = useState(null);
  const [reviews, setReviews] = useState(null);
  const [questions, setQuestions] = useState(null);
  const [wishlisted, setWishlisted] = useState(false);
  const [status, setStatus] = useState("loading");
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [cartSubmitting, setCartSubmitting] = useState(false);
  const [questionSubmitting, setQuestionSubmitting] = useState(false);
  const [questionForm, setQuestionForm] = useState({ title: "", content: "", secret: false });
  const imageDialogRef = useRef(null);

  const loadProduct = useCallback(async () => {
    setStatus("loading");
    try {
      const productData = await fetchProductById(productId);
      if (!productData) {
        setStatus("not-found");
        return;
      }

      setProduct(productData);
      setReviews({ content: [], totalElements: 0 });
      setQuestions({ content: [], totalElements: 0 });
      setSelectedImageIndex(0);
      setQuantity(1);
      setStatus("ready");
    } catch (error) {
      setStatus(error instanceof ApiError && error.status === 404 ? "not-found" : "error");
    }
  }, [productId]);

  useEffect(() => {
    loadProduct();
  }, [loadProduct]);

  useDocumentMeta({
    title: product ? `${product.name} - ${siteConfig.siteName}` : `상품 상세 - ${siteConfig.siteName}`,
    description: product?.shortDescription,
    image: product?.thumbnailImageUrl ? new URL(product.thumbnailImageUrl, window.location.origin).toString() : undefined,
  });
  useProductJsonLd(product);

  useEffect(() => {
    let ignore = false;
    if (!isAuthenticated || !productId) {
      setWishlisted(false);
      return;
    }
    wishlistApi
      .findStatus(productId)
      .then((data) => {
        if (!ignore) setWishlisted(Boolean(data.wishlisted));
      })
      .catch(() => {
        if (!ignore) setWishlisted(false);
      });
    return () => {
      ignore = true;
    };
  }, [isAuthenticated, productId]);

  const images = useMemo(() => normalizeImages(product), [product]);
  const selectedImage = images[selectedImageIndex] || images[0];
  const isSoldOut = product?.soldOut || product?.status === "SOLD_OUT" || product?.stockQuantity === 0;
  const isPurchasable = product?.status === "ACTIVE" && !isSoldOut;
  const hasDiscount = product?.originalPrice && product?.salePrice && Number(product.originalPrice) > Number(product.salePrice);
  const discountRate = Number(product?.discountRate || 0);
  const totalProductAmount = Number(product?.salePrice || 0) * quantity;
  const reviewCount = Number(product?.reviewCount || reviews?.totalElements || 0);
  const averageRating = Number(product?.averageRating || 0);
  const maxQuantity = Math.max(1, Math.min(MAX_QUANTITY, Number(product?.stockQuantity || MAX_QUANTITY)));

  function requireLogin() {
    if (isAuthenticated) return true;
    showToast("로그인이 필요한 기능입니다.", "info");
    navigate("/login", { state: { from: location } });
    return false;
  }

  function changeQuantity(nextQuantity) {
    if (!isPurchasable) return;
    const next = Math.min(maxQuantity, Math.max(1, nextQuantity));
    setQuantity(next);
  }

  async function addToCart({ goToCart = false } = {}) {
    if (!requireLogin() || !isPurchasable || cartSubmitting) return null;
    setCartSubmitting(true);
    try {
      const cart = await cartApi.addItem(product.id, quantity);
      showToast("장바구니에 상품을 담았습니다.");
      if (goToCart) {
        const addedItem = cart?.items?.find((item) => item.productId === product.id);
        navigate("/cart", addedItem ? { state: { preselectCartItemId: addedItem.cartItemId } } : undefined);
      }
      return cart;
    } catch (error) {
      const message = error instanceof ApiError ? error.message : "장바구니 처리 중 오류가 발생했습니다.";
      showToast(message, "error");
      return null;
    } finally {
      setCartSubmitting(false);
    }
  }

  async function handleBuyNow() {
    await addToCart({ goToCart: true });
  }

  async function handleQuestionSubmit(event) {
    event.preventDefault();
    if (!requireLogin() || questionSubmitting) return;

    const title = questionForm.title.trim();
    const content = questionForm.content.trim();
    if (!title || !content) {
      showToast("문의 제목과 내용을 입력해 주세요.", "error");
      return;
    }

    setQuestionSubmitting(true);
    try {
      await questionApi.createQuestion(product.id, { title, content, secret: questionForm.secret });
      showToast("상품문의가 등록되었습니다.");
      setQuestionForm({ title: "", content: "", secret: false });
      const nextQuestions = await questionApi.findProductQuestions(product.id, { page: 0, size: 5 });
      setQuestions(nextQuestions);
    } catch (error) {
      const message = error instanceof ApiError ? error.message : "상품문의 등록 중 오류가 발생했습니다.";
      showToast(message, "error");
    } finally {
      setQuestionSubmitting(false);
    }
  }

  if (status === "loading") {
    return <ProductDetailSkeleton />;
  }

  if (status === "not-found") {
    return (
      <div className="container section">
        <EmptyState
          message="상품을 찾을 수 없습니다."
          action={<Link to="/products" className="btn btn--primary">상품 목록으로</Link>}
        />
      </div>
    );
  }

  if (status === "error" || !product) {
    return (
      <div className="container section">
        <ErrorState
          message="상품 정보를 불러오지 못했습니다."
          onRetry={loadProduct}
          action={<Link to="/products" className="btn btn--tertiary">상품 목록으로</Link>}
        />
      </div>
    );
  }

  return (
    <div className="container section product-detail-page">
      <nav className="breadcrumb product-detail-breadcrumb" aria-label="현재 위치">
        <Link to="/">홈</Link>
        <span aria-hidden="true">/</span>
        {product.categoryId ? (
          <Link to={`/products?categoryId=${product.categoryId}`}>{product.categoryName || "상품"}</Link>
        ) : (
          <Link to="/products">상품</Link>
        )}
        <span aria-hidden="true">/</span>
        <span className="product-detail-breadcrumb__current">{product.name}</span>
      </nav>

      <section className="product-detail-hero" aria-labelledby="product-title">
        <div className="product-detail-gallery">
          <button
            type="button"
            className="product-detail-gallery__main"
            onClick={() => imageDialogRef.current?.showModal()}
            aria-label="상품 이미지 크게 보기"
          >
            <img
              src={selectedImage?.imageUrl || PLACEHOLDER_IMAGE}
              alt={product.name}
              onError={(event) => {
                event.currentTarget.onerror = null;
                event.currentTarget.src = PLACEHOLDER_IMAGE;
              }}
            />
          </button>

          {images.length > 1 && (
            <div className="product-detail-gallery__thumbs" role="list" aria-label="상품 이미지 선택">
              {images.map((image, index) => (
                <button
                  key={`${image.imageUrl}-${index}`}
                  type="button"
                  className={`product-detail-gallery__thumb${index === selectedImageIndex ? " is-active" : ""}`}
                  onClick={() => setSelectedImageIndex(index)}
                  aria-label={`상품 이미지 ${index + 1}번 보기`}
                  aria-current={index === selectedImageIndex ? "true" : undefined}
                >
                  <img
                    src={image.imageUrl}
                    alt=""
                    loading="lazy"
                    onError={(event) => {
                      event.currentTarget.onerror = null;
                      event.currentTarget.src = PLACEHOLDER_IMAGE;
                    }}
                  />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="product-detail-summary">
          <div className="product-detail-summary__top">
            {product.brandDisplayName && <p className="product-detail-summary__brand">{product.brandDisplayName}</p>}
            <h1 id="product-title" className="product-detail-summary__name">{product.name}</h1>
            <div className="product-detail-summary__meta">
              {product.productCode && <span>상품코드 {product.productCode}</span>}
              {product.featured && <span className="badge badge--featured">추천</span>}
              {isSoldOut && <span className="badge badge--soldout">품절</span>}
              {!isSoldOut && product.status === "DISCONTINUED" && <span className="badge badge--soldout">판매종료</span>}
            </div>
          </div>

          {product.shortDescription && <p className="product-detail-summary__lead">{product.shortDescription}</p>}

          {reviewCount > 0 && (
            <a href="#product-reviews" className="product-detail-summary__rating" aria-label={`평균 평점 ${averageRating.toFixed(1)}점, 리뷰 ${reviewCount}개 보기`}>
              <span aria-hidden="true">★</span>
              <strong>{averageRating.toFixed(1)}</strong>
              <span>리뷰 {reviewCount}개</span>
            </a>
          )}

          <div className="product-detail-price" aria-label="상품 가격">
            {hasDiscount && <del className="product-detail-price__original">{formatPrice(product.originalPrice)}</del>}
            <div className="product-detail-price__final">
              {discountRate > 0 && hasDiscount && <span className="product-detail-price__discount">{discountRate}%</span>}
              <strong className="product-detail-price__sale">{formatPrice(product.salePrice)}</strong>
            </div>
          </div>

          {product.boxUnit && <p className="product-detail-box-unit">박스 단위 · {product.boxUnit}</p>}

          <dl className="product-detail-info-list">
            <div>
              <dt>배송</dt>
              <dd>주문서에서 배송비와 최종 결제금액을 확인할 수 있습니다.</dd>
            </div>
            <div>
              <dt>상품상태</dt>
              <dd>{describeProductStatus(product.status, isSoldOut)}</dd>
            </div>
            <div>
              <dt>혜택</dt>
              <dd>쿠폰 및 할인 적용 가능 여부는 주문서에서 서버 기준으로 계산됩니다.</dd>
            </div>
          </dl>

          <div className="product-detail-quantity">
            <label htmlFor="product-detail-quantity">수량</label>
            <div className="quantity-selector">
              <button type="button" onClick={() => changeQuantity(quantity - 1)} disabled={!isPurchasable || quantity <= 1} aria-label="수량 감소">
                -
              </button>
              <input
                id="product-detail-quantity"
                type="text"
                inputMode="numeric"
                value={quantity}
                onChange={(event) => changeQuantity(Number(event.target.value.replace(/\D/g, "")) || 1)}
                disabled={!isPurchasable}
                aria-label="구매 수량"
              />
              <button type="button" onClick={() => changeQuantity(quantity + 1)} disabled={!isPurchasable || quantity >= maxQuantity} aria-label="수량 증가">
                +
              </button>
            </div>
          </div>

          <div className="product-detail-total">
            <span>총 상품금액</span>
            <strong>{formatPrice(totalProductAmount)}</strong>
          </div>

          <div className="product-detail-actions">
            <WishlistButton
              productId={product.id}
              initialWishlisted={wishlisted}
              initialCount={product.wishlistCount}
              showText
              className="product-detail-actions__wishlist"
              onChange={(response) => setWishlisted(response.wishlisted)}
            />
            <button type="button" className="btn btn--secondary btn--large" onClick={() => addToCart()} disabled={!isPurchasable || cartSubmitting}>
              {cartSubmitting && <span className="btn__spinner" aria-hidden="true" />}
              {cartSubmitting ? "담는 중..." : "장바구니"}
            </button>
            <button type="button" className="btn btn--primary btn--large" onClick={handleBuyNow} disabled={!isPurchasable || cartSubmitting}>
              {cartSubmitting && <span className="btn__spinner" aria-hidden="true" />}
              구매하기
            </button>
          </div>

          {!isPurchasable && (
            <p className="product-detail-summary__disabled" role="status">
              현재 구매할 수 없는 상품입니다. 찜은 가능하며, 상품 상태가 변경되면 다시 확인해 주세요.
            </p>
          )}
        </div>
      </section>

      <nav className="detail-tabs" aria-label="상품 상세 메뉴">
        <a href="#product-info">상품정보</a>
        <a href="#product-reviews">리뷰 {reviewCount > 0 ? reviewCount : ""}</a>
        <a href="#product-questions">상품문의 {questions?.totalElements ? questions.totalElements : ""}</a>
      </nav>

      <section id="product-info" className="product-detail-section product-detail-description" aria-labelledby="product-info-title">
        <div className="section-header">
          <h2 id="product-info-title">상품정보</h2>
          <p>실제 등록된 상품 설명과 기본 정보를 기준으로 안내합니다.</p>
        </div>
        {product.description ? (
          <p className="product-detail-description__text">{product.description}</p>
        ) : (
          <EmptyState message="등록된 상세 설명이 없습니다." />
        )}

        {product.composition?.length > 0 && (
          <div className="product-detail-composition">
            <h3>상품 구성</h3>
            <div className="table-scroll">
              <table className="composition-table">
                <thead>
                  <tr>
                    <th>품목</th>
                    <th>중량</th>
                    <th>수량</th>
                  </tr>
                </thead>
                <tbody>
                  {product.composition.map((item, index) => (
                    <tr key={`${item.name}-${index}`}>
                      <td>{item.name}</td>
                      <td>{item.weight}</td>
                      <td>{item.count}개</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        <div className="product-detail-spec">
          <h3>상품 기본정보</h3>
          <dl>
            <div>
              <dt>상품명</dt>
              <dd>{product.name}</dd>
            </div>
            {product.brandDisplayName && (
              <div>
                <dt>브랜드</dt>
                <dd>{product.brandDisplayName}</dd>
              </div>
            )}
            {product.categoryName && (
              <div>
                <dt>카테고리</dt>
                <dd>{product.categoryName}</dd>
              </div>
            )}
            {product.productCode && (
              <div>
                <dt>상품코드</dt>
                <dd>{product.productCode}</dd>
              </div>
            )}
          </dl>
        </div>

        <div className="product-detail-policy">
          <h3>배송/교환/반품 안내</h3>
          <p>
            배송비, 쿠폰, 최종 결제금액은 주문서 단계에서 서버 계산 결과로 확인합니다.
            교환/반품 및 식품 고시정보는 운영 전 정책과 상품 데이터 등록이 필요합니다.
          </p>
        </div>
      </section>

      <section id="product-reviews" className="product-detail-section" aria-labelledby="product-reviews-title">
        <div className="section-header section-header--split">
          <div>
            <h2 id="product-reviews-title">고객 리뷰</h2>
            <p>실제 구매 후 작성된 리뷰만 표시합니다.</p>
          </div>
          {reviewCount > 0 && (
            <div className="review-summary">
              <strong>★ {averageRating.toFixed(1)}</strong>
              <span>{reviewCount}개 리뷰</span>
            </div>
          )}
        </div>
        <ReviewList reviews={reviews?.content || []} />
      </section>

      <section id="product-questions" className="product-detail-section" aria-labelledby="product-questions-title">
        <div className="section-header section-header--split">
          <div>
            <h2 id="product-questions-title">상품문의</h2>
            <p>상품 구성이나 구매 전 확인할 내용을 문의할 수 있습니다.</p>
          </div>
        </div>

        <QuestionForm
          form={questionForm}
          submitting={questionSubmitting}
          onChange={setQuestionForm}
          onSubmit={handleQuestionSubmit}
        />
        <QuestionList questions={questions?.content || []} />
      </section>

      <div className="mobile-purchase-bar" aria-label="모바일 구매 바로가기">
        <WishlistButton productId={product.id} initialWishlisted={wishlisted} initialCount={product.wishlistCount} onChange={(response) => setWishlisted(response.wishlisted)} />
        <button type="button" className="btn btn--secondary" onClick={() => addToCart()} disabled={!isPurchasable || cartSubmitting}>
          {cartSubmitting && <span className="btn__spinner" aria-hidden="true" />}
          장바구니
        </button>
        <button type="button" className="btn btn--primary" onClick={handleBuyNow} disabled={!isPurchasable || cartSubmitting}>
          {cartSubmitting && <span className="btn__spinner" aria-hidden="true" />}
          구매하기
        </button>
      </div>

      <dialog ref={imageDialogRef} className="image-viewer" onClick={(event) => event.target === imageDialogRef.current && imageDialogRef.current.close()}>
        <button type="button" className="image-viewer__close" onClick={() => imageDialogRef.current?.close()} aria-label="이미지 확대보기 닫기">
          ×
        </button>
        <img src={selectedImage?.imageUrl || PLACEHOLDER_IMAGE} alt={product.name} />
      </dialog>
    </div>
  );
}

function ProductDetailSkeleton() {
  return (
    <div className="container section product-detail-page" aria-label="상품 상세 로딩 중">
      <div className="product-detail-skeleton">
        <div className="skeleton-block product-detail-skeleton__image" />
        <div className="product-detail-skeleton__info">
          <div className="skeleton-line skeleton-line--short" />
          <div className="skeleton-line skeleton-line--title" />
          <div className="skeleton-line" />
          <div className="skeleton-line skeleton-line--price" />
          <div className="skeleton-line" />
          <div className="skeleton-line skeleton-line--button" />
        </div>
      </div>
    </div>
  );
}

function ReviewList({ reviews }) {
  if (!reviews.length) {
    return <EmptyState message="아직 작성된 리뷰가 없습니다." />;
  }

  return (
    <ul className="detail-community-list">
      {reviews.map((review) => (
        <li key={review.id} className="detail-community-item">
          <div className="detail-community-item__head">
            <strong aria-label={`별점 ${review.rating}점`}>{"★".repeat(review.rating)}{"☆".repeat(5 - review.rating)}</strong>
            <span>{review.authorName}</span>
            <time dateTime={review.createdAt}>{formatDateTime(review.createdAt)}</time>
          </div>
          <p>{review.content}</p>
          {review.verifiedPurchase && <span className="badge badge--muted">구매확인</span>}
        </li>
      ))}
    </ul>
  );
}

function QuestionForm({ form, submitting, onChange, onSubmit }) {
  return (
    <form className="question-write-form" onSubmit={onSubmit}>
      <div className="form-field">
        <label htmlFor="question-title">문의 제목</label>
        <input
          id="question-title"
          value={form.title}
          onChange={(event) => onChange({ ...form, title: event.target.value })}
          maxLength={100}
          placeholder="궁금한 내용을 간단히 입력해 주세요."
        />
      </div>
      <div className="form-field">
        <label htmlFor="question-content">문의 내용</label>
        <textarea
          id="question-content"
          value={form.content}
          onChange={(event) => onChange({ ...form, content: event.target.value })}
          maxLength={2000}
          rows={4}
          placeholder="상품 구성, 포장, 배송 전 확인할 내용을 남겨 주세요."
        />
      </div>
      <div className="question-write-form__footer">
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={form.secret}
            onChange={(event) => onChange({ ...form, secret: event.target.checked })}
          />
          비밀글로 문의
        </label>
        <button type="submit" className="btn btn--secondary" disabled={submitting}>
          {submitting ? "등록 중..." : "문의 등록"}
        </button>
      </div>
    </form>
  );
}

function QuestionList({ questions }) {
  if (!questions.length) {
    return <EmptyState message="등록된 상품문의가 없습니다." />;
  }

  return (
    <ul className="detail-community-list">
      {questions.map((question) => (
        <li key={question.id} className="detail-community-item detail-community-item--question">
          <div className="detail-community-item__head">
            <span className={`badge ${question.status === "ANSWERED" ? "badge--success" : "badge--muted"}`}>
              {question.status === "ANSWERED" ? "답변완료" : "답변대기"}
            </span>
            {question.secret && <span className="badge badge--muted">비밀글</span>}
            <span>{question.authorName}</span>
            <time dateTime={question.createdAt}>{formatDateTime(question.createdAt)}</time>
          </div>
          <strong className="detail-community-item__title">{question.title || "비밀글입니다."}</strong>
          {question.content ? <p>{question.content}</p> : <p className="text-muted">비밀글입니다.</p>}
          {question.answer && (
            <div className="question-answer">
              <strong>판매자 답변</strong>
              <p>{question.answer.content}</p>
            </div>
          )}
        </li>
      ))}
    </ul>
  );
}

function normalizeImages(product) {
  const images = product?.images?.length
    ? [...product.images].sort((a, b) => {
        if (a.imageType === "MAIN" && b.imageType !== "MAIN") return -1;
        if (a.imageType !== "MAIN" && b.imageType === "MAIN") return 1;
        return Number(a.displayOrder || 0) - Number(b.displayOrder || 0);
      })
    : [];

  if (!images.length) {
    return [{ imageUrl: PLACEHOLDER_IMAGE }];
  }
  return images.map((image) => ({ ...image, imageUrl: image.imageUrl || PLACEHOLDER_IMAGE }));
}

function describeProductStatus(status, soldOut) {
  if (soldOut || status === "SOLD_OUT") return "품절";
  if (status === "DISCONTINUED") return "판매종료";
  if (status === "ACTIVE") return "판매중";
  return "구매 가능 여부를 확인 중입니다.";
}
