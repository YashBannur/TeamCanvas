import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Notifications from '../pages/Notifications/Notifications';
import { notificationService } from '../services/notificationService';

vi.mock('../services/notificationService', () => ({
  notificationService: {
    getNotifications: vi.fn(),
    markRead: vi.fn(),
    markAllRead: vi.fn()
  }
}));

const unreadNotification = {
  id: 41,
  type: 'INVITATION_RECEIVED',
  message: 'You were invited to join workspace: Design Team',
  entityType: 'INVITATION',
  entityId: 12,
  isRead: false,
  createdAt: '2026-10-27T08:30:00Z'
};

describe('Notifications page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    notificationService.getNotifications.mockResolvedValue({
      content: [unreadNotification],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1
    });
    notificationService.markRead.mockResolvedValue(undefined);
    notificationService.markAllRead.mockResolvedValue(undefined);
  });

  it('loads and displays notifications', async () => {
    render(
      <MemoryRouter>
        <Notifications />
      </MemoryRouter>
    );

    expect(await screen.findByText(unreadNotification.message)).toBeInTheDocument();
    expect(notificationService.getNotifications).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      unreadOnly: false
    });
  });

  it('marks a notification read and refreshes the list', async () => {
    render(
      <MemoryRouter>
        <Notifications />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: 'Mark read' }));

    await waitFor(() => {
      expect(notificationService.markRead).toHaveBeenCalledWith(41);
      expect(notificationService.getNotifications).toHaveBeenCalledTimes(2);
    });
  });

  it('marks all notifications read', async () => {
    render(
      <MemoryRouter>
        <Notifications />
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: 'Mark all read' }));

    await waitFor(() => {
      expect(notificationService.markAllRead).toHaveBeenCalledTimes(1);
      expect(notificationService.getNotifications).toHaveBeenCalledTimes(2);
    });
  });
});