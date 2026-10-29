import api from './api';

export const invitationService = {
  async getWorkspaceInvitations(workspaceId) {
    const response = await api.get(`/api/workspaces/${workspaceId}/invitations`);
    return response.data;
  },

  async createInvitation(workspaceId, { email, role = 'MEMBER' }) {
    const response = await api.post(`/api/workspaces/${workspaceId}/invitations`, { email, role });
    return response.data;
  },

  async cancelInvitation(workspaceId, invitationId) {
    await api.delete(`/api/workspaces/${workspaceId}/invitations/${invitationId}`);
  },

  async getMyInvitations() {
    const response = await api.get('/api/invitations');
    return response.data;
  },

  async acceptInvitation(invitationId) {
    const response = await api.post(`/api/invitations/${invitationId}/accept`);
    return response.data;
  },

  async rejectInvitation(invitationId) {
    await api.post(`/api/invitations/${invitationId}/reject`);
  }
};