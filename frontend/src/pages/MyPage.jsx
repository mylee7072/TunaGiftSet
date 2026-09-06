import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { formatPhone } from "../utils/format";

export function MyPage() {
  const { member, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/");
  }

  if (!member) return null;

  return (
    <div className="container section mypage">
      <div className="mypage__header">
        <p className="breadcrumb">홈 / 마이페이지</p>
        <h1 className="page-title">마이페이지</h1>
        <p>{member.name}님의 쇼핑 정보를 한곳에서 확인하세요.</p>
      </div>

      <section className="mypage__profile" aria-labelledby="mypage-profile-title">
        <h2 id="mypage-profile-title">회원 정보</h2>
        <dl>
          <div>
            <dt>이름</dt>
            <dd>{member.name}</dd>
          </div>
          <div>
            <dt>이메일</dt>
            <dd>{member.email}</dd>
          </div>
          <div>
            <dt>휴대전화</dt>
            <dd>{formatPhone(member.phone) || "-"}</dd>
          </div>
        </dl>
      </section>

      <nav className="mypage__menu" aria-label="마이페이지 메뉴">
        <Link to="/mypage/orders" className="mypage__menu-item">
          <strong>주문내역</strong>
          <span>주문 상태와 결제 정보를 확인합니다.</span>
        </Link>
        <Link to="/mypage/addresses" className="mypage__menu-item">
          <strong>배송지 관리</strong>
          <span>자주 쓰는 배송지를 저장합니다.</span>
        </Link>
        <Link to="/mypage/wishlist" className="mypage__menu-item">
          <strong>찜한 상품</strong>
          <span>관심 있는 선물세트를 모아봅니다.</span>
        </Link>
        <Link to="/mypage/coupons" className="mypage__menu-item">
          <strong>쿠폰함</strong>
          <span>사용 가능한 쿠폰을 확인합니다.</span>
        </Link>
        <button type="button" className="mypage__menu-item mypage__menu-item--button" onClick={handleLogout}>
          <strong>로그아웃</strong>
          <span>현재 계정에서 안전하게 나갑니다.</span>
        </button>
      </nav>
    </div>
  );
}
