import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './components/AppLayout';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard/Dashboard';
import Workspaces from './pages/Workspaces/Workspaces';
import WorkspaceDetail from './pages/WorkspaceDetail/WorkspaceDetail';
import ProjectDetail from './pages/ProjectDetail/ProjectDetail';
import Invitations from './pages/Invitations/Invitations';
import Profile from './pages/Profile/Profile';
import NotFound from './pages/NotFound';
import Notifications from './pages/Notifications/Notifications';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/workspaces" element={<Workspaces />} />
                <Route path="/workspaces/:workspaceId" element={<WorkspaceDetail />} />
                <Route path="/projects/:projectId" element={<ProjectDetail />} />
                <Route path="/invitations" element={<Invitations />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/notifications" element={<Notifications />} />
              </Route>
            </Route>

            <Route path="*" element={<NotFound />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}