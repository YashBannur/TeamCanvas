import { AlertCircle, RotateCcw } from 'lucide-react';

export function ErrorState({
  title = 'Something went wrong',
  message = 'An unexpected error occurred while loading this content.',
  onRetry
}) {
  return (
    <div className="tc-error-state" role="alert">
      <div className="tc-error-state__icon">
        <AlertCircle size={28} />
      </div>
      <h3 className="tc-error-state__title">{title}</h3>
      <p className="tc-error-state__description">{message}</p>
      {onRetry && (
        <button type="button" className="button-secondary" onClick={onRetry}>
          <RotateCcw size={16} style={{ marginRight: '6px' }} />
          Try again
        </button>
      )}
    </div>
  );
}