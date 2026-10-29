import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within
} from '@testing-library/react';
import { KanbanBoard } from '../pages/ProjectDetail/KanbanBoard';
import { taskService } from '../services/taskService';
import { ToastProvider } from '../context/ToastContext';

const initialBoardData = {
  columns: {
    TODO: [
      {
        id: 1,
        title: 'Sample Move Task',
        status: 'TODO',
        priority: 'LOW',
        version: 1
      }
    ],
    IN_PROGRESS: [],
    IN_REVIEW: [],
    DONE: []
  }
};

function renderBoard(onReloadBoard = vi.fn()) {
  render(
    <ToastProvider>
      <KanbanBoard
        boardData={initialBoardData}
        onOpenTask={vi.fn()}
        canMoveTask={() => true}
        isArchived={false}
        onReloadBoard={onReloadBoard}
        projectId={10}
      />
    </ToastProvider>
  );

  return onReloadBoard;
}

async function moveTaskToInProgress() {
  fireEvent.click(screen.getByRole('button', { name: 'Move task' }));
  fireEvent.click(screen.getByRole('button', { name: 'In Progress' }));
}

describe('KanbanBoard Move & Rollback', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the task in its initial column', () => {
    renderBoard();

    expect(screen.getByText('Sample Move Task')).toBeInTheDocument();
    expect(screen.getByText('To Do')).toBeInTheDocument();
  });

  it('moves a task successfully and reloads the board', async () => {
    const reloadBoard = renderBoard();
    const moveSpy = vi.spyOn(taskService, 'move').mockResolvedValue({});

    await moveTaskToInProgress();

    await waitFor(() => {
      expect(moveSpy).toHaveBeenCalledWith(1, {
        status: 'IN_PROGRESS',
        afterTaskId: undefined,
        version: 1
      });
      expect(reloadBoard).toHaveBeenCalledTimes(1);
    });

    expect(await screen.findByText('Task moved to In Progress')).toBeInTheDocument();
  });

  it('puts the task back in its original column and shows an error when moving fails', async () => {
    const reloadBoard = renderBoard();
    vi.spyOn(taskService, 'move').mockRejectedValue({
      response: {
        status: 500,
        data: { message: 'Move failed' }
      }
    });

    await moveTaskToInProgress();

    expect(await screen.findByText('Move failed')).toBeInTheDocument();

    await waitFor(() => {
      const taskCard = screen.getByText('Sample Move Task').closest('.task-card');
      const taskColumn = taskCard.closest('.kanban-column');

      expect(within(taskColumn).getByText('To Do')).toBeInTheDocument();
      expect(reloadBoard).toHaveBeenCalledTimes(1);
    });
  });

  it('reloads the board after a 409 conflict', async () => {
    const reloadBoard = renderBoard();
    vi.spyOn(taskService, 'move').mockRejectedValue({
      response: {
        status: 409,
        data: { message: 'Task version is stale' }
      }
    });

    await moveTaskToInProgress();

    expect(await screen.findByText('Task version is stale')).toBeInTheDocument();

    await waitFor(() => {
      expect(reloadBoard).toHaveBeenCalledTimes(1);
    });
  });
});