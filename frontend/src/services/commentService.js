import api from './api';

export const commentService = {
  async getByTask(taskId) {
    const response = await api.get(`/api/tasks/${taskId}/comments`);
    return response.data;
  },

  async create(taskId, content) {
    const response = await api.post(`/api/tasks/${taskId}/comments`, { content });
    return response.data;
  },

  async update(commentId, content) {
    const response = await api.patch(`/api/comments/${commentId}`, { content });
    return response.data;
  },

  async delete(commentId) {
    await api.delete(`/api/comments/${commentId}`);
  }
};