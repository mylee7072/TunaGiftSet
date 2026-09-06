import { useEffect, useState } from "react";
import { Link, NavLink, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { siteConfig } from "../../config/siteConfig";
import { useAuth } from "../../context/useAuth";
import { useDismissibleOverlay } from "../../hooks/useDismissibleOverlay";
import { cartApi } from "../../api/cartApi";
import { categoryApi } from "../../api/productApi";

export function Header() {
  const { isAuthenticated, member, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get("keyword") || "");
  const [menuOpen, setMenuOpen] = useState(false);
  const [cartCount, setCartCount] = useState(0);
  const [scrolled, setScrolled] = useState(false);
  const { shouldRender: drawerMounted, closing: drawerClosing } = useDismissibleOverlay(menuOpen, () => setMenuOpen(false));

  useEffect(() => {
    setKeyword(searchParams.get("keyword") || "");
  }, [searchParams]);

  useEffect(() => {
    if (!isAuthenticated) {
      setCartCount(0);
      return;
    }
    let cancelled = false;
    cartApi
      .getCart()
      .then((cart) => {
        if (!cancelled) setCartCount(cart.summary.totalItemCount);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated]);

  useEffect(() => {
    function handleScroll() {
      setScrolled(window.scrollY > 4);
    }
    handleScroll();
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  function handleSearchSubmit(event) {
    event.preventDefault();
    const trimmed = keyword.trim();
    setMenuOpen(false);
    navigate(trimmed ? `/products?keyword=${encodeURIComponent(trimmed)}` : "/products");
  }

  function clearSearch() {
    setKeyword("");
    if (location.pathname === "/products" && searchParams.get("keyword")) {
      const params = new URLSearchParams(searchParams);
      params.delete("keyword");
      params.delete("page");
      navigate({ pathname: "/products", search: params.toString() ? `?${params.toString()}` : "" });
    }
  }

  function handleLogout() {
    logout();
    setMenuOpen(false);
    navigate("/");
  }

  const navInert = !menuOpen ? { inert: "" } : {};

  return (
    <header className={`site-header${scrolled ? " site-header--scrolled" : ""}`}>
      <div className="site-header__utility">
        <div className="container site-header__utility-inner">
          <span>선물세트 전문 쇼핑몰</span>
          <span>평일 10:00-17:00 고객지원</span>
        </div>
      </div>

      <div className="site-header__top container">
        <button
          type="button"
          className="site-header__menu-toggle"
          aria-label={menuOpen ? "메뉴 닫기" : "메뉴 열기"}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((open) => !open)}
        >
          <span className={menuOpen ? "is-open" : ""} />
          <span className={menuOpen ? "is-open" : ""} />
          <span className={menuOpen ? "is-open" : ""} />
        </button>

        <Link to="/" className="site-header__brand" onClick={() => setMenuOpen(false)}>
          <span className="site-header__brand-mark" aria-hidden="true">선</span>
          <span>{siteConfig.siteName}</span>
        </Link>

        <form className="site-header__search" role="search" onSubmit={handleSearchSubmit}>
          <input
            type="search"
            name="keyword"
            placeholder="어떤 선물세트를 찾으세요?"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            aria-label="상품 검색"
          />
          {keyword && (
            <button type="button" className="site-header__search-clear" onClick={clearSearch} aria-label="검색어 지우기">
              ×
            </button>
          )}
          <button type="submit">검색</button>
        </form>

        <nav className="site-header__actions" aria-label="사용자 메뉴">
          {isAuthenticated ? (
            <>
              {member?.role === "ADMIN" && <Link to="/admin">관리자</Link>}
              <Link to="/mypage">마이페이지</Link>
              <button type="button" onClick={handleLogout} className="link-button">
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link to="/login">로그인</Link>
              <Link to="/signup">회원가입</Link>
            </>
          )}
          <Link to="/mypage/wishlist" className="site-header__wishlist">
            찜
          </Link>
          <Link to="/cart" className="site-header__cart" aria-label={`장바구니, 상품 ${cartCount}개`}>
            장바구니
            {cartCount > 0 && (
              <span key={cartCount} className="site-header__cart-badge">
                {cartCount}
              </span>
            )}
          </Link>
        </nav>
      </div>

      {drawerMounted && <div className={`nav-backdrop${drawerClosing ? " nav-backdrop--closing" : ""}`} onClick={() => setMenuOpen(false)} />}

      <nav className={`site-header__gnb${menuOpen ? " site-header__gnb--open" : ""}`} aria-label="카테고리" {...navInert}>
        <NavLink to="/products" onClick={() => setMenuOpen(false)} className={({ isActive }) => (isActive && !searchParams.get("categoryId") ? "is-active" : "")}>
          전체상품
        </NavLink>
        <CategoryLinks activeCategoryId={searchParams.get("categoryId")} onNavigate={() => setMenuOpen(false)} />
        {!isAuthenticated && (
          <div className="site-header__gnb-auth">
            <Link to="/login" onClick={() => setMenuOpen(false)}>
              로그인
            </Link>
            <Link to="/signup" onClick={() => setMenuOpen(false)}>
              회원가입
            </Link>
          </div>
        )}
      </nav>
    </header>
  );
}

function CategoryLinks({ activeCategoryId, onNavigate }) {
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    categoryApi.findActiveCategories().then(setCategories).catch(() => setCategories([]));
  }, []);

  return categories
    .filter((category) => !category.parentId)
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map((category) => (
      <Link
        key={category.id}
        to={`/products?categoryId=${category.id}`}
        className={String(activeCategoryId) === String(category.id) ? "is-active" : ""}
        onClick={onNavigate}
      >
        {category.name}
      </Link>
    ));
}
