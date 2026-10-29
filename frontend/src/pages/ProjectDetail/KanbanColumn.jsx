import { useState } from 'react';
import { useDroppable } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { Plus } from 'lucide-react';
import { TaskCard } from './TaskCard';
import { TASK_STATUS_LABELS } from '../../constants/taskStatus';

export function KanbanColumn({
  status,
  tasks = [],
  onOpenTask,
  canMoveTask,
  isArchived,
  onQuickAddTask,
  onMoveToStatus
}) {
  const { setNodeRef } = useDroppable({ id: status });
  const [isAdding, setIsAdding] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [addingSubmitting, setAddingSubmitting] = useState(false);

  const handleQuickAdd = async (e) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    setAddingSubmitting(true);
    try {
      await onQuickAddTask(status, newTitle.trim());
      setNewTitle('');
      setIsAdding(false);
    } finally {
      setAddingSubmitting(false);
    }
  };

  return (
    <div className="kanban-column" ref={setNodeRef}>
      <div className="kanban-column-header">
        <div className="kanban-column-title">
          <span>{TASK_STATUS_LABELS[status]}</span>
          <span className="kanban-column-count">{tasks.length}</span>
        </div>
      </div>

      <div className="kanban-column-tasks">
        <SortableContext
          items={tasks.map((t) => t.id)}
          strategy={verticalListSortingStrategy}
        >
          {tasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onOpenTask={onOpenTask}
              canMove={canMoveTask(task)}
              isArchived={isArchived}
              onMoveToStatus={onMoveToStatus}
            />
          ))}
        </SortableContext>
      </div>

      {!isArchived && (
        <div className="kanban-column-footer">
          {isAdding ? (
            <form onSubmit={handleQuickAdd} className="inline-add-task-form">
              <input
                type="text"
                className="inline-add-task-input"
                placeholder="Task title..."
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                disabled={addingSubmitting}
                autoFocus
              />
              <div className="inline-add-task-actions">
                <button
                  type="submit"
                  className="button-primary"
                  style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                  disabled={addingSubmitting || !newTitle.trim()}
                >
                  Add
                </button>
                <button
                  type="button"
                  className="button-secondary"
                  style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                  onClick={() => setIsAdding(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <button
              type="button"
              className="button-secondary"
              style={{
                width: '100%',
                padding: '6px 12px',
                fontSize: '0.85rem',
                borderStyle: 'dashed'
              }}
              onClick={() => setIsAdding(true)}
            >
              <Plus size={15} />
              Add Task
            </button>
          )}
        </div>
      )}
    </div>
  );
}