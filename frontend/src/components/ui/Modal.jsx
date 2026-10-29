import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';

export function Modal({ isOpen, onClose, title, children, maxWidth = 520 }) {
  const modalRef = useRef(null);
  const triggerElementRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      triggerElementRef.current = document.activeElement;
      document.body.style.overflow = 'hidden';

      const focusableSelector =
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';
      const handleKeyDown = (e) => {
        if (e.key === 'Escape') {
          onClose();
          return;
        }
        if (e.key === 'Tab' && modalRef.current) {
          const focusables = modalRef.current.querySelectorAll(focusableSelector);
          if (focusables.length === 0) return;
          const first = focusables[0];
          const last = focusables[focusables.length - 1];

          if (e.shiftKey && document.activeElement === first) {
            e.preventDefault();
            last.focus();
          } else if (!e.shiftKey && document.activeElement === last) {
            e.preventDefault();
            first.focus();
          }
        }
      };

      window.addEventListener('keydown', handleKeyDown);

      setTimeout(() => {
        if (modalRef.current) {
          const firstInput = modalRef.current.querySelector(focusableSelector);
          if (firstInput) {
            firstInput.focus();
          } else {
            modalRef.current.focus();
          }
        }
      }, 50);

      return () => {
        window.removeEventListener('keydown', handleKeyDown);
        document.body.style.overflow = '';
        if (triggerElementRef.current && typeof triggerElementRef.current.focus === 'function') {
          triggerElementRef.current.focus();
        }
      };
    }
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return createPortal(
    <div
      className="tc-modal-backdrop"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      role="presentation"
    >
      <div
        className="tc-modal"
        ref={modalRef}
        style={{ maxWidth: `${maxWidth}px` }}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? 'tc-modal-title' : undefined}
        tabIndex={-1}
      >
        <div className="tc-modal__header">
          {title && (
            <h2 id="tc-modal-title" className="tc-modal__title">
              {title}
            </h2>
          )}
          <button
            type="button"
            className="icon-button"
            onClick={onClose}
            aria-label="Close dialog"
            style={{ marginLeft: 'auto' }}
          >
            <X size={18} />
          </button>
        </div>
        <div className="tc-modal__body">{children}</div>
      </div>
    </div>,
    document.body
  );
}