import api from './api';

export const projectService = {
  async getByWorkspace(workspaceId, status) {
    const params = status ? { status } : {};
    const response = await api.get(`/api/workspaces/${workspaceId}/projects`, { params });
    return response.data;
  },

  async getAnalytics(projectId, { from, to } = {}) {
    const response = await api.get(`/api/projects/${projectId}/analytics`, {
      params: { from, to }
    });
    return response.data;
  },

  async getById(projectId) {
    const response = await api.get(`/api/projects/${projectId}`);
    return response.data;
  },

  async create(workspaceId, data) {
    const response = await api.post(`/api/workspaces/${workspaceId}/projects`, data);
    return response.data;
  },

  async update(projectId, data) {
    const response = await api.patch(`/api/projects/${projectId}`, data);
    return response.data;
  },

  async delete(projectId) {
    await api.delete(`/api/projects/${projectId}`);
  }
};