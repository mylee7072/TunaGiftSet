// Shared header for list-style pages (마이페이지 계열 등) — breadcrumb, title,
// description, and an optional right-aligned action all in one consistent block,
// so each page doesn't reimplement its own header markup with small variations.
export function PageHeader({ breadcrumb, title, description, action }) {
  return (
    <div className="page-header">
      <div>
        {breadcrumb && <p className="breadcrumb">{breadcrumb}</p>}
        <h1 className="page-title">{title}</h1>
        {description && <p className="page-header__description">{description}</p>}
      </div>
      {action}
    </div>
  );
}
