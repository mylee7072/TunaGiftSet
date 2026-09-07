import { useState } from "react";
import { Link } from "react-router-dom";
import { siteConfig } from "../../config/siteConfig";
import { getCompanyInfoEntries } from "../../config/company";

export function Footer() {
  const [expanded, setExpanded] = useState(false);
  const entries = getCompanyInfoEntries();

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

        <div className="site-footer__business">
          <button
            type="button"
            className="community-item__toggle site-footer__business-toggle"
            aria-expanded={expanded}
            aria-controls="footer-business-info"
            onClick={() => setExpanded((current) => !current)}
          >
            <span>사업자정보확인</span>
            <span className="community-item__toggle-icon" aria-hidden="true">▾</span>
          </button>
          <div id="footer-business-info" className={`community-item__collapsible${expanded ? " is-expanded" : ""}`}>
            <div className="community-item__collapsible-inner">
              <dl className="site-footer__business-table">
                {entries.map((entry) => (
                  <div key={entry.label}>
                    <dt>{entry.label}</dt>
                    <dd>{entry.value}</dd>
                  </div>
                ))}
              </dl>
              <Link to="/company" className="link-button">사업자 정보 전체 보기</Link>
            </div>
          </div>
        </div>

        <p className="site-footer__copyright">
          © {new Date().getFullYear()} {siteConfig.siteNameEn}. All rights reserved.
        </p>
      </div>
    </footer>
  );
}
