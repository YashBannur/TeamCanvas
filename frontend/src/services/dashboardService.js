import api from './api';
import { getTodayLocalISO } from '../utils/date';

function addDaysToIsoDate(isoDate, days) {
  const date = new Date(`${isoDate}T00:00:00`);
  date.setDate(date.getDate() + days);

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

function createMockDashboard() {
  const today = getTodayLocalISO();
  const yesterday = addDaysToIsoDate(today, -1);
  const tomorrow = addDaysToIsoDate(today, 1);
  const updatedAt = new Date().toISOString();

  const recentProjects = [
    {
      id: 101,
      name: 'Website Refresh',
      progressPercent: 65,
      completedTasks: 13,
      totalTasks: 20,
      dueDate: addDaysToIsoDate(today, 14),
      updatedAt
    },
    {
      id: 102,
      name: 'Mobile App Launch',
      progressPercent: 35,
      completedTasks: 7,
      totalTasks: 20,
      dueDate: addDaysToIsoDate(today, 30),
      updatedAt
    }
  ];

  const myTasks = [
    {
      id: 201,
      projectId: 101,
      projectName: 'Website Refresh',
      workspaceId: 301,
      workspaceName: 'Product Team',
      title: 'Review the new landing page',
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      dueDate: tomorrow,
      assigneeName: 'You',
      updatedAt
    },
    {
      id: 202,
      projectId: 101,
      projectName: 'Website Refresh',
      workspaceId: 301,
      workspaceName: 'Product Team',
      title: 'Approve updated navigation',
      status: 'TODO',
      priority: 'URGENT',
      dueDate: yesterday,
      assigneeName: 'You',
      updatedAt
    },
    {
      id: 203,
      projectId: 102,
      projectName: 'Mobile App Launch',
      workspaceId: 301,
      workspaceName: 'Product Team',
      title: 'Check release notes',
      status: 'TODO',
      priority: 'MEDIUM',
      dueDate: null,
      assigneeName: 'You',
      updatedAt
    }
  ];

  const recentTasks = [
    ...myTasks,
    {
      id: 204,
      projectId: 102,
      projectName: 'Mobile App Launch',
      workspaceId: 301,
      workspaceName: 'Product Team',
      title: 'Complete sign-in screen',
      status: 'DONE',
      priority: 'HIGH',
      dueDate: addDaysToIsoDate(today, 3),
      assigneeName: 'Alex Morgan',
      updatedAt
    }
  ];

  return {
    counts: {
      workspaces: 1,
      activeProjects: 2,
      openTasks: 3,
      dueSoon: 1,
      overdue: 1
    },
    myTasks,
    recentProjects,
    recentTasks
  };
}

export const dashboardService = {
  async getDashboard() {
    if (import.meta.env.VITE_USE_MOCK_DASHBOARD === 'true') {
      return createMockDashboard();
    }

    const today = getTodayLocalISO();
    const response = await api.get('/api/dashboard', { params: { today } });
    return response.data;
  }
};