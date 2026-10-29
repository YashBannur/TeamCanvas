export function Skeleton({ width = '100%', height = '16px', circle = false, style = {}, className = '' }) {
  return (
    <div
      className={`tc-skeleton ${circle ? 'tc-skeleton--circle' : ''} ${className}`}
      style={{
        width,
        height,
        ...style
      }}
      aria-hidden="true"
    />
  );
}

export function CardSkeleton() {
  return (
    <div
      style={{
        padding: '24px',
        backgroundColor: '#ffffff',
        border: '1px solid var(--border-color)',
        borderRadius: '16px',
        display: 'flex',
        flexDirection: 'column',
        gap: '14px'
      }}
      aria-hidden="true"
    >
      <Skeleton width="40%" height="22px" />
      <Skeleton width="80%" height="14px" />
      <Skeleton width="60%" height="14px" />
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '12px' }}>
        <Skeleton width="60px" height="24px" />
        <Skeleton width="80px" height="24px" />
      </div>
    </div>
  );
}

export function BoardColumnSkeleton() {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        minWidth: '280px',
        flex: 1,
        backgroundColor: '#f8fafc',
        borderRadius: '14px',
        padding: '16px'
      }}
      aria-hidden="true"
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
        <Skeleton width="100px" height="20px" />
        <Skeleton width="24px" height="20px" circle />
      </div>
      <Skeleton height="70px" style={{ borderRadius: '12px' }} />
      <Skeleton height="70px" style={{ borderRadius: '12px' }} />
      <Skeleton height="70px" style={{ borderRadius: '12px' }} />
    </div>
  );
}   