export function FormField({
  id,
  label,
  required = false,
  error,
  hint,
  children
}) {
  const errorId = error ? `${id}-error` : undefined;
  const hintId = hint ? `${id}-hint` : undefined;

  return (
    <div className="tc-form-field">
      {label && (
        <label htmlFor={id} className="tc-form-field__label">
          {label}
          {required && <span className="tc-form-field__label-required" aria-hidden="true">*</span>}
        </label>
      )}
      {children({
        id,
        'aria-invalid': Boolean(error),
        'aria-describedby': [errorId, hintId].filter(Boolean).join(' ') || undefined,
        className: error ? 'tc-form-field__input tc-form-field__input--error' : 'tc-form-field__input'
      })}
      {hint && !error && (
        <p id={hintId} className="tc-form-field__hint">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="tc-form-field__error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}