// page/totalPages are 0-indexed/absolute counts as returned by the backend's
// Page<T> responses (see ProductPageResponse / OrderPageResponse).
export function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;

  const pages = Array.from({ length: totalPages }, (_, index) => index);

  return (
    <nav className="pagination" aria-label="페이지 네비게이션">
      <button
        type="button"
        className="pagination__nav"
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 0}
        aria-label="이전 페이지"
      >
        이전
      </button>
      <ul className="pagination__list">
        {pages.map((pageNumber) => (
          <li key={pageNumber}>
            <button
              type="button"
              className={`pagination__page${pageNumber === page ? " pagination__page--active" : ""}`}
              onClick={() => onPageChange(pageNumber)}
              aria-current={pageNumber === page ? "page" : undefined}
            >
              {pageNumber + 1}
            </button>
          </li>
        ))}
      </ul>
      <button
        type="button"
        className="pagination__nav"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        aria-label="다음 페이지"
      >
        다음
      </button>
    </nav>
  );
}
