function getInitials(name) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}

function getAvatarColor(name) {
  const colors = [
    '#6258e8',
    '#059669',
    '#0284c7',
    '#d97706',
    '#dc2626',
    '#7c3aed',
    '#db2777',
    '#475569'
  ];
  if (!name) return colors[0];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
}

export function Avatar({ name, size = 32, style = {} }) {
  const initials = getInitials(name);
  const backgroundColor = getAvatarColor(name);

  return (
    <div
      className="tc-avatar"
      style={{
        width: `${size}px`,
        height: `${size}px`,
        fontSize: `${Math.round(size * 0.42)}px`,
        backgroundColor,
        ...style
      }}
      title={name || 'User'}
      aria-label={name || 'User'}
    >
      {initials}
    </div>
  );
}