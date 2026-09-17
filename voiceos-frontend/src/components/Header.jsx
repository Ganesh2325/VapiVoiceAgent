import React, { useEffect, useState } from 'react';
import { Cpu, Sparkles, AlertTriangle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getStoredUser, login, logout, register } from '../services/api';

export default function Header({ isCallActive, pendingApprovalsCount }) {
  const navigate = useNavigate();
  const [user, setUser] = useState(getStoredUser());
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState('');

  useEffect(() => {
    const refresh = () => setUser(getStoredUser());
    window.addEventListener('voiceos:auth-changed', refresh);
    window.addEventListener('voiceos:unauthorized', refresh);
    return () => {
      window.removeEventListener('voiceos:auth-changed', refresh);
      window.removeEventListener('voiceos:unauthorized', refresh);
    };
  }, []);

  const handleLogin = async () => {
    setAuthError('');
    try {
      await login(email, password);
      setPassword('');
      setUser(getStoredUser());
    } catch (err) {
      setAuthError(err.message || 'Login failed');
    }
  };

  const handleRegister = async () => {
    setAuthError('');
    try {
      await register(email.split('@')[0] || 'VoiceOS User', email, password);
      setPassword('');
      setUser(getStoredUser());
    } catch (err) {
      setAuthError(err.message || 'Registration failed');
    }
  };

  return (
    <header style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '14px 28px', borderBottom: '1px solid var(--border-color)',
      background: 'rgba(8, 11, 17, 0.85)', backdropFilter: 'blur(16px)', zIndex: 50
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        <div style={{
          width: '36px', height: '36px', borderRadius: '10px',
          background: 'var(--accent-gradient)', display: 'flex',
          alignItems: 'center', justifyContent: 'center', boxShadow: 'var(--shadow-glow)'
        }}>
          <Cpu size={22} color="#ffffff" />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontWeight: 800, fontSize: '1.25rem', letterSpacing: '-0.02em', background: 'var(--accent-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              VoiceOS
            </span>
            <span className="badge badge-primary">v2.0 • Vapi Edition</span>
          </div>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            Multi-Agent Voice AI Operations Platform
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 12px', background: 'rgba(255,255,255,0.04)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: isCallActive ? '#10b981' : '#6366f1', boxShadow: isCallActive ? '0 0 10px #10b981' : 'none' }} />
          <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Voice Platform: <strong>Vapi (WebRTC/SIP)</strong></span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 12px', background: 'rgba(255,255,255,0.04)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
          <Sparkles size={14} color="#818cf8" />
          <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>LLM: <strong>GPT-4o</strong></span>
        </div>

        {pendingApprovalsCount > 0 && (
          <button
            onClick={() => navigate('/approvals')}
            className="badge badge-warning"
            style={{ cursor: 'pointer', padding: '6px 12px', fontSize: '0.8rem' }}
          >
            <AlertTriangle size={14} /> {pendingApprovalsCount} Approval Required
          </button>
        )}

        {user ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{user.email}</span>
            <button className="btn" style={{ padding: '6px 10px', fontSize: '0.75rem' }} onClick={logout}>Logout</button>
          </div>
        ) : (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <input
              type="email"
              placeholder="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              style={{ width: '150px', padding: '6px 8px', fontSize: '0.75rem' }}
            />
            <input
              type="password"
              placeholder="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={{ width: '120px', padding: '6px 8px', fontSize: '0.75rem' }}
            />
            <button className="btn btn-primary" style={{ padding: '6px 10px', fontSize: '0.75rem' }} onClick={handleLogin}>Login</button>
            <button className="btn" style={{ padding: '6px 10px', fontSize: '0.75rem' }} onClick={handleRegister}>Register</button>
            {authError && <span style={{ color: '#f87171', fontSize: '0.7rem' }}>{authError}</span>}
          </div>
        )}
      </div>
    </header>
  );
}
