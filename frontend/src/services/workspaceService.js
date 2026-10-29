import api from './api';

export const workspaceService = {
  async getAll() {
    const response = await api.get('/api/workspaces');
    return response.data;
  },

  async getById(id) {
    const response = await api.get(`/api/workspaces/${id}`);
    return response.data;
  },

  async create(data) {
    const response = await api.post('/api/workspaces', data);
    return response.data;
  },

  async update(id, data) {
    const response = await api.patch(`/api/workspaces/${id}`, data);
    return response.data;
  },

  async delete(id) {
    await api.delete(`/api/workspaces/${id}`);
  },

  async getMembers(id) {
    const response = await api.get(`/api/workspaces/${id}/members`);
    return response.data;
  },

  async updateMemberRole(workspaceId, userId, role) {
    const response = await api.patch(`/api/workspaces/${workspaceId}/members/${userId}`, { role });
    return response.data;
  },

  async removeMember(workspaceId, userId) {
    await api.delete(`/api/workspaces/${workspaceId}/members/${userId}`);
  },

  async leave(id) {
    await api.post(`/api/workspaces/${id}/leave`);
  }
};