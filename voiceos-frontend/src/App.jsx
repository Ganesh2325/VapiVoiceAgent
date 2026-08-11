import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import ConsolePage from './pages/ConsolePage';
import AgentsPage from './pages/AgentsPage';
import ApprovalsPage from './pages/ApprovalsPage';
import TasksPage from './pages/TasksPage';
import RagPage from './pages/RagPage';
import MetricsPage from './pages/MetricsPage';
import ConnectedAccountsPage from './pages/ConnectedAccountsPage';

export default function App() {
  const [isCallActive, setIsCallActive] = useState(false);
  const [pendingApprovalsCount, setPendingApprovalsCount] = useState(0);

  // You would typically fetch the approvals count from the backend here
  // useEffect(() => { ... }, []);

  return (
    <BrowserRouter>
      <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: 'var(--bg-primary)' }}>
        <Header isCallActive={isCallActive} pendingApprovalsCount={pendingApprovalsCount} />
        
        <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
          <Sidebar pendingApprovalsCount={pendingApprovalsCount} />
          
          <main style={{ flex: 1, overflowY: 'auto', padding: '24px 32px', display: 'flex', flexDirection: 'column' }}>
            <Routes>
              <Route path="/" element={<ConsolePage isCallActive={isCallActive} setIsCallActive={setIsCallActive} />} />
              <Route path="/agents" element={<AgentsPage />} />
              <Route path="/approvals" element={<ApprovalsPage setPendingApprovalsCount={setPendingApprovalsCount} />} />
              <Route path="/accounts" element={<ConnectedAccountsPage />} />
              <Route path="/tasks" element={<TasksPage />} />
              <Route path="/rag" element={<RagPage />} />
              <Route path="/metrics" element={<MetricsPage />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </div>
    </BrowserRouter>
  );
}
