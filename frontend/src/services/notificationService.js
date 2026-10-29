import api from './api';

export const notificationService = {
  async getNotifications({ page = 0, size = 20, unreadOnly = false } = {}) {
    const response = await api.get('/api/notifications', {
      params: { page, size, unreadOnly }
    });
    return response.data;
  },

  async getUnreadCount() {
    const response = await api.get('/api/notifications/unread-count');
    return response.data;
  },

  async markRead(notificationId) {
    await api.patch(`/api/notifications/${notificationId}/read`);
  },

  async markAllRead() {
    await api.patch('/api/notifications/read-all');
  }
};