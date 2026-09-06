export function EmptyState({ message, action }) {
  return (
    <div className="state-block state-block--empty">
      <p>{message}</p>
      {action}
    </div>
  );
}
