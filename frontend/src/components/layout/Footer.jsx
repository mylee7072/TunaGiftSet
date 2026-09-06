import { Link } from "react-router-dom";
import { siteConfig } from "../../config/siteConfig";

export function Footer() {
  return (
    <footer className="site-footer">
      <div className="container site-footer__inner">
        <div className="site-footer__brand">
          <strong>{siteConfig.siteName}</strong>
          <p>
            {siteConfig.siteNameEn}은 독립적으로 운영되는 선물세트 쇼핑몰입니다. 특정 제조사의 공식몰 또는 공식 제휴몰을
            사칭하지 않습니다.
          </p>
        </div>

        <nav className="site-footer__links" aria-label="고객 메뉴">
          <Link to="/products">상품 둘러보기</Link>
          <Link to="/mypage/orders">주문내역</Link>
          <Link to="/mypage/coupons">쿠폰함</Link>
          <Link to="/mypage/addresses">배송지 관리</Link>
        </nav>

        <dl className="site-footer__info">
          <div>
            <dt>고객센터</dt>
            <dd>{siteConfig.supportPhone}</dd>
          </div>
          <div>
            <dt>운영시간</dt>
            <dd>{siteConfig.supportHours}</dd>
          </div>
          <div>
            <dt>사업자 정보</dt>
            <dd>운영 전 등록 예정</dd>
          </div>
        </dl>

        <p className="site-footer__copyright">
          © {new Date().getFullYear()} {siteConfig.siteNameEn}. All rights reserved.
        </p>
      </div>
    </footer>
  );
}
