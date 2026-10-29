export function ProgressBar({ value = 0, showLabel = true }) {
  const clamped = Math.min(100, Math.max(0, Math.round(value)));

  return (
    <div className="tc-progress-container">
      <div
        className="tc-progress-bar"
        role="progressbar"
        aria-valuenow={clamped}
        aria-valuemin="0"
        aria-valuemax="100"
      >
        <div className="tc-progress-bar__fill" style={{ width: `${clamped}%` }} />
      </div>
      {showLabel && <span className="tc-progress-bar__label">{clamped}%</span>}
    </div>
  );
}