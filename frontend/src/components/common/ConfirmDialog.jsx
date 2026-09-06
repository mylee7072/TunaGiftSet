import { useEffect, useRef } from "react";
import { useDismissibleOverlay } from "../../hooks/useDismissibleOverlay";

export function ConfirmDialog({ open, title, description, confirmLabel = "확인", cancelLabel = "취소", onConfirm, onCancel }) {
  const confirmButtonRef = useRef(null);
  const modalRef = useRef(null);
  const { shouldRender, closing } = useDismissibleOverlay(open, onCancel, modalRef);

  useEffect(() => {
    if (open) {
      confirmButtonRef.current?.focus();
    }
  }, [open]);

  if (!shouldRender) return null;

  return (
    <div
      className={`modal-overlay${closing ? " modal-overlay--closing" : ""}`}
      role="presentation"
      onClick={onCancel}
    >
      <div
        ref={modalRef}
        className={`modal${closing ? " modal--closing" : ""}`}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="confirm-dialog-title">{title}</h2>
        {description && <p>{description}</p>}
        <div className="modal__actions">
          <button type="button" className="btn btn--secondary" onClick={onCancel}>
            {cancelLabel}
          </button>
          <button type="button" className="btn btn--primary" ref={confirmButtonRef} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
