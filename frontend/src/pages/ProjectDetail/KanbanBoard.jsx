import { useState, useEffect } from 'react';
import {
  DndContext,
  PointerSensor,
  KeyboardSensor,
  useSensor,
  useSensors,
  closestCorners
} from '@dnd-kit/core';
import { sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import { KanbanColumn } from './KanbanColumn';
import {
  TASK_STATUS_LABELS,
  TASK_STATUS_ORDER
} from '../../constants/taskStatus';
import { taskService } from '../../services/taskService';
import { useToast } from '../../context/ToastContext';
import { parseApiError } from '../../utils/apiError';

export function KanbanBoard({
  boardData,
  onOpenTask,
  canMoveTask,
  isArchived,
  onReloadBoard,
  projectId
}) {
  const { error: showErrorToast, success } = useToast();
  const [columns, setColumns] = useState(boardData?.columns || {});

  useEffect(() => {
    if (boardData?.columns) {
      setColumns(boardData.columns);
    }
  }, [boardData]);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5
      }
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates
    })
  );

  const findColumnOfTask = (taskId) => {
    for (const [columnStatus, tasks] of Object.entries(columns)) {
      if (tasks.some((task) => task.id === taskId)) {
        return columnStatus;
      }
    }
    return null;
  };

  const handleDragEnd = async (event) => {
    const { active, over } = event;
    if (!over || isArchived) return;

    const activeId = active.id;
    const overId = over.id;

    if (activeId === overId) return;

    const sourceColumn = findColumnOfTask(activeId);
    if (!sourceColumn) return;

    const targetColumn = TASK_STATUS_ORDER.includes(overId)
      ? overId
      : findColumnOfTask(overId);

    if (!targetColumn) return;

    const taskToMove = (columns[sourceColumn] || []).find(
      (task) => task.id === activeId
    );
    if (!taskToMove) return;

    const previousColumns = { ...columns };
    const nextSourceTasks = (columns[sourceColumn] || []).filter(
      (task) => task.id !== activeId
    );

    const nextTargetTasks =
      sourceColumn === targetColumn
        ? [...nextSourceTasks]
        : [...(columns[targetColumn] || [])];

    let targetIndex;

    if (TASK_STATUS_ORDER.includes(overId)) {
      // Dropping on the column itself places the task at the bottom.
      targetIndex = nextTargetTasks.length;
    } else {
      // Find the target after removing the active task. This handles
      // reordering within the same column without an off-by-one index.
      targetIndex = nextTargetTasks.findIndex((task) => task.id === overId);
      if (targetIndex < 0) return;
    }

    const movedTask = { ...taskToMove, status: targetColumn };
    nextTargetTasks.splice(targetIndex, 0, movedTask);

    if (sourceColumn === targetColumn) {
      const oldOrder = (columns[sourceColumn] || []).map((task) => task.id);
      const newOrder = nextTargetTasks.map((task) => task.id);
      const orderUnchanged = oldOrder.every((id, index) => id === newOrder[index]);

      if (orderUnchanged) return;
    }

    const nextColumns = {
      ...columns,
      [sourceColumn]: nextSourceTasks,
      [targetColumn]: nextTargetTasks
    };

    setColumns(nextColumns);

    const newIndex = nextTargetTasks.findIndex((task) => task.id === activeId);
    const afterTaskId = newIndex > 0 ? nextTargetTasks[newIndex - 1].id : undefined;

    try {
      await taskService.move(activeId, {
        status: targetColumn,
        afterTaskId,
        version: taskToMove.version
      });
      onReloadBoard();
    } catch (err) {
      setColumns(previousColumns);
      const parsed = parseApiError(err);

      if (parsed.status === 409) {
        showErrorToast('Board updated by someone else. Refreshing...');
        onReloadBoard();
      } else {
        showErrorToast(parsed.message || 'Failed to move task');
      }
    }
  };

  const handleMoveToStatus = async (task, targetStatus) => {
    const sourceColumn = task.status;
    const previousColumns = { ...columns };

    const nextSourceTasks = (columns[sourceColumn] || []).filter(
      (item) => item.id !== task.id
    );
    const targetTasks = columns[targetStatus] || [];
    const nextTargetTasks = [...targetTasks, { ...task, status: targetStatus }];

    setColumns({
      ...columns,
      [sourceColumn]: nextSourceTasks,
      [targetStatus]: nextTargetTasks
    });

    const afterTaskId =
      targetTasks.length > 0 ? targetTasks[targetTasks.length - 1].id : undefined;

    try {
      await taskService.move(task.id, {
        status: targetStatus,
        afterTaskId,
        version: task.version
      });
      success(`Task moved to ${TASK_STATUS_LABELS[targetStatus]}`);
      onReloadBoard();
    } catch (err) {
      setColumns(previousColumns);
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to move task');
      onReloadBoard();
    }
  };

  const handleQuickAdd = async (status, title) => {
    try {
      await taskService.create(projectId, { title, status });
      success('Task created');
      onReloadBoard();
    } catch (err) {
      const parsed = parseApiError(err);
      showErrorToast(parsed.message || 'Failed to create task');
    }
  };

  return (
    <div className="kanban-board-scroll">
      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragEnd={handleDragEnd}
      >
        <div className="kanban-board">
          {TASK_STATUS_ORDER.map((statusKey) => (
            <KanbanColumn
              key={statusKey}
              status={statusKey}
              tasks={columns[statusKey] || []}
              onOpenTask={onOpenTask}
              canMoveTask={canMoveTask}
              isArchived={isArchived}
              onQuickAddTask={handleQuickAdd}
              onMoveToStatus={handleMoveToStatus}
            />
          ))}
        </div>
      </DndContext>
    </div>
  );
}