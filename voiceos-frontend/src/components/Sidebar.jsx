import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { 
  Phone, Layers, Shield, CheckSquare, Database, Activity, AlertTriangle, Link2 
} from 'lucide-react';

export default function Sidebar({ pendingApprovalsCount }) {
  return (
    <nav style={{
      width: '240px', borderRight: '1px solid var(--border-color)',
      background: 'rgba(15, 20, 31, 0.5)', padding: '20px 12px',
      display: 'flex', flexDirection: 'column', gap: '6px'
    }}>
      <NavLink to="/" className={({ isActive }) => `btn ${isActive ? 'btn-primary' : 'btn-secondary'}`} style={{ width: '100%', justifyContent: 'flex-start' }}>
        <Phone size={18} /> Vapi Voice Console
      </NavLink>

      <NavLink to="/agents" className={({ isActive }) => `btn ${isActive ? 'btn-primary' : 'btn-secondary'}`} style={{ width: '100%', justifyContent: 'flex-start' }}>
        <Layers size={18} /> Multi-Agent Fleet
      </NavLink>

      <NavLink to="/approvals" className={({ isActive }) => `btn ${isActive ? 'btn-primary' : 'btn-secondary'}`} style={{ width: '100%', justifyContent: 'flex-start', position: 'relative' }}>
        <Shield size={18} /> Human Approvals
        {pendingApprovalsCount > 0 && (
          <span style={{ marginLeft: 'auto', background: '#f43f5e', color: '#fff', fontSize: '0.7rem', padding: '2px 6px', borderRadius: '10px' }}>
            {pendingApprovalsCount}
          </span>
        )}
      </NavLink>

      <NavLink to="/tasks" className={({ isActive }) => `btn ${isActive ? 'btn-primary' : 'btn-secondary'}`} style={{ width: '100%', justifyContent: 'flex-start' }}>
        <CheckSquare size={18} /> Tasks & Memory
      </NavLink>
      
      <NavLink to="/accounts" className={({ isActive }) => `btn ${isActive ? 'btn-primary' : 'btn-secondary'}`} style={{ width: '100%', justifyContent: 'flex-start' }}>
        <Link2 size={18} /> Connected Accounts
      </NavLink>

      <NavLink to="/rag" className={({ isActive }) => `btn ${isActive ? 'btn-primary' : 'btn-secondary'}`} style={{ width: '100%', justifyContent: 'flex-start' }}>
        <Database size={18} /> RAG Knowledge
      </NavLink>

      <NavLink to="/metrics" className={({ isActive }) => `btn ${isActive ? 'btn-primary' : 'btn-secondary'}`} style={{ width: '100%', justifyContent: 'flex-start' }}>
        <Activity size={18} /> Quality & Metrics
      </NavLink>

      <div style={{ marginTop: 'auto', padding: '12px', background: 'rgba(255,255,255,0.02)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
          <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10b981' }} />
          <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Vapi Webhook: ACTIVE</span>
        </div>
        <p style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>POST /api/v1/webhooks/vapi</p>
      </div>
    </nav>
  );
}
