import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TaskCard } from '../pages/ProjectDetail/TaskCard';

// Mock dnd-kit sortable hook
vi.mock('@dnd-kit/sortable', () => ({
  useSortable: () => ({
    attributes: {},
    listeners: {},
    setNodeRef: vi.fn(),
    transform: null,
    transition: null,
    isDragging: false
  })
}));

describe('TaskCard Component', () => {
  const baseTask = {
    id: 42,
    title: 'Implement OAuth refresh token',
    priority: 'HIGH',
    status: 'IN_PROGRESS',
    assigneeName: 'Alex Rivers',
    dueDate: '2026-10-30'
  };

  it('renders title, priority badge, and assignee name', () => {
    render(
      <TaskCard
        task={baseTask}
        onOpenTask={vi.fn()}
        canMove={true}
        isArchived={false}
        onMoveToStatus={vi.fn()}
      />
    );

    expect(screen.getByText('Implement OAuth refresh token')).toBeInTheDocument();
    expect(screen.getByText('High')).toBeInTheDocument();
    expect(screen.getByTitle('Alex Rivers')).toBeInTheDocument();
  });

  it('renders "Unassigned" placeholder when no assignee is present', () => {
    const unassignedTask = { ...baseTask, assigneeName: null };
    render(
      <TaskCard
        task={unassignedTask}
        onOpenTask={vi.fn()}
        canMove={true}
        isArchived={false}
        onMoveToStatus={vi.fn()}
      />
    );

    expect(screen.getByText('Unassigned')).toBeInTheDocument();
  });
});