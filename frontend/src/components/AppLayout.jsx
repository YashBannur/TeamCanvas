import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Topbar from './Topbar';
import './AppLayout.css';

export default function AppLayout() {
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);

  return (
    <div className="app-shell">
      {mobileDrawerOpen && (
        <div
          className="mobile-drawer-backdrop"
          onClick={() => setMobileDrawerOpen(false)}
          role="presentation"
        />
      )}

      <Sidebar
        isOpen={mobileDrawerOpen}
        onClose={() => setMobileDrawerOpen(false)}
      />

      <div className="main-wrapper">
        <Topbar onOpenMobileMenu={() => setMobileDrawerOpen(true)} />
        <main className="content-area">
          <Outlet />
        </main>
      </div>
    </div>
  );
}