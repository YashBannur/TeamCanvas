import api from './api';

export const activityService = {
  async getProjectActivity(projectId, { page = 0, size = 20 } = {}) {
    const response = await api.get(`/api/projects/${projectId}/activity`, {
      params: { page, size }
    });
    return response.data;
  },

  async getTaskActivity(taskId, { page = 0, size = 50 } = {}) {
    const response = await api.get(`/api/tasks/${taskId}/activity`, {
      params: { page, size }
    });
    return response.data;
  }
};