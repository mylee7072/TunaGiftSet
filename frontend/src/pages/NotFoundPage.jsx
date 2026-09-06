import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div className="container section state-block">
      <h1 className="page-title">페이지를 찾을 수 없습니다</h1>
      <Link to="/" className="btn btn--primary">
        메인으로 이동
      </Link>
    </div>
  );
}
