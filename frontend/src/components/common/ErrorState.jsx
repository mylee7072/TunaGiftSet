import { ErrorIcon } from "./icons/StateIcons";

export function ErrorState({ message = "잠시 후 다시 시도해 주세요.", onRetry, action, icon: Icon = ErrorIcon }) {
  return (
    <div className="state-block state-block--error" role="alert">
      <Icon className="state-block__icon" />
      <p>{message}</p>
      <div className="state-block__actions">
        {onRetry && (
          <button type="button" className="btn btn--secondary" onClick={onRetry}>
            다시 시도
          </button>
        )}
        {action}
      </div>
    </div>
  );
}
