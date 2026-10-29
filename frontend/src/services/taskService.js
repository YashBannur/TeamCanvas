import api from './api';

export const taskService = {
  async getByProjectPaged(projectId, params = {}) {
    const response = await api.get(`/api/projects/${projectId}/tasks`, { params });
    return response.data;
  },

  async getBoard(projectId, params = {}) {
    const response = await api.get(`/api/projects/${projectId}/board`, { params });
    return response.data;
  },

  async getById(taskId) {
    const response = await api.get(`/api/tasks/${taskId}`);
    return response.data;
  },

  async create(projectId, data) {
    const response = await api.post(`/api/projects/${projectId}/tasks`, data);
    return response.data;
  },

  async update(taskId, data) {
    const response = await api.patch(`/api/tasks/${taskId}`, data);
    return response.data;
  },

  async move(taskId, { status, afterTaskId, version }) {
    const payload = { status };
    if (afterTaskId !== undefined) {
      payload.afterTaskId = afterTaskId;
    }
    if (version !== undefined) {
      payload.version = version;
    }
    const response = await api.patch(`/api/tasks/${taskId}/move`, payload);
    return response.data;
  },

  async delete(taskId) {
    await api.delete(`/api/tasks/${taskId}`);
  }
};