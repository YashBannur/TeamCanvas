import { useState } from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Calendar, MoreVertical } from 'lucide-react';
import { PriorityBadge } from '../../components/ui/Badge';
import { Avatar } from '../../components/ui/Avatar';
import { formatDate, isOverdue } from '../../utils/date';
import { TASK_STATUS_ORDER, TASK_STATUS_LABELS } from '../../constants/taskStatus';

export function TaskCard({
  task,
  onOpenTask,
  canMove,
  isArchived,
  onMoveToStatus
}) {
  const [showMenu, setShowMenu] = useState(false);

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging
  } = useSortable({
    id: task.id,
    disabled: !canMove || isArchived,
    data: { task }
  });

  const style = {
    transform: CSS.Translate.toString(transform),
    transition
  };

  const overdue = isOverdue(task.dueDate, task.status);

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`task-card ${isDragging ? 'is-dragging' : ''}`}
      onClick={() => onOpenTask(task)}
      {...attributes}
      {...listeners}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter') onOpenTask(task);
      }}
    >
      <div className="task-card-header">
        <PriorityBadge priority={task.priority} />

        {canMove && !isArchived && (
          <div
            style={{ position: 'relative' }}
            onClick={(e) => e.stopPropagation()}
            onPointerDown={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              className="icon-button"
              style={{ padding: '2px' }}
              onClick={() => setShowMenu((prev) => !prev)}
              aria-label="Move task"
            >
              <MoreVertical size={14} />
            </button>

            {showMenu && (
              <div
                style={{
                  position: 'absolute',
                  right: 0,
                  top: '100%',
                  backgroundColor: '#ffffff',
                  border: '1px solid var(--border-color)',
                  borderRadius: '8px',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                  zIndex: 20,
                  width: '140px',
                  display: 'flex',
                  flexDirection: 'column',
                  padding: '4px'
                }}
              >
                <span
                  style={{
                    fontSize: '0.7rem',
                    fontWeight: 700,
                    padding: '4px 8px',
                    color: 'var(--text-secondary)'
                  }}
                >
                  Move to...
                </span>
                {TASK_STATUS_ORDER.map((statusKey) => {
                  if (statusKey === task.status) return null;
                  return (
                    <button
                      key={statusKey}
                      type="button"
                      style={{
                        background: 'none',
                        border: 'none',
                        textAlign: 'left',
                        padding: '6px 8px',
                        fontSize: '0.8rem',
                        cursor: 'pointer',
                        borderRadius: '4px'
                      }}
                      onClick={() => {
                        setShowMenu(false);
                        onMoveToStatus(task, statusKey);
                      }}
                    >
                      {TASK_STATUS_LABELS[statusKey]}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>

      <h4 className="task-card-title">{task.title}</h4>

      <div className="task-card-footer">
        <div className={`task-card-date ${overdue ? 'is-overdue' : ''}`}>
          {task.dueDate && (
            <>
              <Calendar size={13} />
              <span>{formatDate(task.dueDate)}</span>
            </>
          )}
        </div>

        <div>
          {task.assigneeName ? (
            <Avatar name={task.assigneeName} size={24} />
          ) : (
            <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              Unassigned
            </span>
          )}
        </div>
      </div>
    </div>
  );
}