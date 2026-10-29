import { FolderOpen } from 'lucide-react';

export function EmptyState({
  icon: Icon = FolderOpen,
  title = 'No items found',
  description = 'There are no items to display at this time.',
  action
}) {
  return (
    <div className="tc-empty-state">
      <div className="tc-empty-state__icon">
        <Icon size={28} />
      </div>
      <h3 className="tc-empty-state__title">{title}</h3>
      <p className="tc-empty-state__description">{description}</p>
      {action && <div>{action}</div>}
    </div>
  );
}