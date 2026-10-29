import { ArrowUpDown } from 'lucide-react';
import { PriorityBadge, StatusBadge } from '../../components/ui/Badge';
import { Avatar } from '../../components/ui/Avatar';
import { formatDate } from '../../utils/date';

export function TaskListView({
  pagedData,
  page,
  size,
  sortBy,
  direction,
  onSortChange,
  onPageChange,
  onOpenTask
}) {
  const tasks = pagedData?.content || [];
  const totalPages = pagedData?.totalPages || 0;
  const totalElements = pagedData?.totalElements || 0;

  const handleHeaderClick = (columnKey) => {
    if (sortBy === columnKey) {
      onSortChange(columnKey, direction === 'asc' ? 'desc' : 'asc');
    } else {
      onSortChange(columnKey, 'asc');
    }
  };

  return (
    <div className="task-list-view">
      <div className="task-list-table-container">
        <table className="task-table">
          <thead>
            <tr>
              <th onClick={() => handleHeaderClick('title')}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span>Title</span>
                  <ArrowUpDown size={13} />
                </div>
              </th>
              <th>Status</th>
              <th>Priority</th>
              <th>Assignee</th>
              <th onClick={() => handleHeaderClick('dueDate')}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span>Due Date</span>
                  <ArrowUpDown size={13} />
                </div>
              </th>
              <th onClick={() => handleHeaderClick('updatedAt')}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span>Updated</span>
                  <ArrowUpDown size={13} />
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            {tasks.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-secondary)', padding: '32px' }}>
                  No tasks matched the filters.
                </td>
              </tr>
            ) : (
              tasks.map((task) => (
                <tr key={task.id} onClick={() => onOpenTask(task)}>
                  <td style={{ fontWeight: 600 }}>{task.title}</td>
                  <td>
                    <StatusBadge status={task.status} />
                  </td>
                  <td>
                    <PriorityBadge priority={task.priority} />
                  </td>
                  <td>
                    {task.assigneeName ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Avatar name={task.assigneeName} size={24} />
                        <span>{task.assigneeName}</span>
                      </div>
                    ) : (
                      <span style={{ color: 'var(--text-secondary)' }}>Unassigned</span>
                    )}
                  </td>
                  <td>{task.dueDate ? formatDate(task.dueDate) : '-'}</td>
                  <td style={{ color: 'var(--text-secondary)' }}>{formatDate(task.updatedAt?.split('T')[0])}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="task-pagination">
        <span>
          Showing {tasks.length} of {totalElements} tasks
        </span>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            type="button"
            className="button-secondary"
            style={{ padding: '4px 10px', fontSize: '0.8rem' }}
            disabled={page <= 0}
            onClick={() => onPageChange(page - 1)}
          >
            Previous
          </button>
          <span style={{ display: 'flex', alignItems: 'center', padding: '0 6px' }}>
            Page {page + 1} of {totalPages || 1}
          </span>
          <button
            type="button"
            className="button-secondary"
            style={{ padding: '4px 10px', fontSize: '0.8rem' }}
            disabled={page + 1 >= totalPages}
            onClick={() => onPageChange(page + 1)}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
}