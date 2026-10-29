import { Modal } from './Modal';

export function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title = 'Confirm action',
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  isDestructive = false,
  isLoading = false
}) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} maxWidth={440}>
      <p style={{ margin: '0 0 24px 0', fontSize: '0.95rem', color: 'var(--text-color)', lineHeight: 1.5 }}>
        {message}
      </p>
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
        <button
          type="button"
          className="button-secondary"
          onClick={onClose}
          disabled={isLoading}
        >
          {cancelText}
        </button>
        <button
          type="button"
          className="button-primary"
          style={isDestructive ? { backgroundColor: '#ef4444' } : undefined}
          onClick={onConfirm}
          disabled={isLoading}
        >
          {isLoading ? 'Processing...' : confirmText}
        </button>
      </div>
    </Modal>
  );
}