import { EmptyBoxIcon } from "./icons/StateIcons";

export function EmptyState({ message, action, icon: Icon = EmptyBoxIcon }) {
  return (
    <div className="state-block state-block--empty">
      <Icon className="state-block__icon" />
      <p>{message}</p>
      {action}
    </div>
  );
}
