import { X, CheckCircle2, AlertCircle, Info } from 'lucide-react';

export function ToastItem({ toast, onClose }) {
  const getIcon = () => {
    switch (toast.type) {
      case 'success':
        return <CheckCircle2 size={18} color="#10b981" aria-hidden="true" />;
      case 'error':
        return <AlertCircle size={18} color="#ef4444" aria-hidden="true" />;
      default:
        return <Info size={18} color="var(--primary-color)" aria-hidden="true" />;
    }
  };

  return (
    <div
      className={`tc-toast tc-toast--${toast.type}`}
      role="status"
      aria-live="polite"
    >
      {getIcon()}
      <div className="tc-toast__content">{toast.message}</div>
      <button
        type="button"
        className="tc-toast__close"
        onClick={() => onClose(toast.id)}
        aria-label="Dismiss notification"
      >
        <X size={16} />
      </button>
    </div>
  );
}

export function ToastContainer({ toasts, onClose }) {
  if (toasts.length === 0) return null;

  return (
    <div className="tc-toast-container" aria-label="Notifications">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onClose={onClose} />
      ))}
    </div>
  );
}