import React, { useState } from 'react';
import { Link2, Mail, MessageCircle, Calendar, Plane } from 'lucide-react';

export default function ConnectedAccountsPage() {
  const [accounts, setAccounts] = useState([
    { id: 1, provider: 'Google', icon: <Mail size={20} />, status: 'Connected', email: 'user@example.com' },
    { id: 2, provider: 'WhatsApp Business', icon: <MessageCircle size={20} />, status: 'Connected', phone: '+1234567890' },
    { id: 3, provider: 'Microsoft Calendar', icon: <Calendar size={20} />, status: 'Not Connected' },
    { id: 4, provider: 'Official Travel API', icon: <Plane size={20} />, status: 'Not Connected' },
  ]);

  const toggleConnection = (id, currentStatus) => {
    // Mocking OAuth connection flow
    setAccounts(prev => prev.map(acc => 
      acc.id === id ? { ...acc, status: currentStatus === 'Connected' ? 'Not Connected' : 'Connected' } : acc
    ));
  };

  return (
    <div>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Link2 size={24} className="text-primary" /> Connected Accounts
        </h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
          Manage your third-party integrations and OAuth tokens securely. Passwords are never stored.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
        {accounts.map(acc => (
          <div key={acc.id} className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ padding: '10px', background: 'rgba(255,255,255,0.05)', borderRadius: '8px' }}>
                {acc.icon}
              </div>
              <div style={{ flex: 1 }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600 }}>{acc.provider}</h3>
                {acc.email && <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{acc.email}</p>}
                {acc.phone && <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{acc.phone}</p>}
              </div>
            </div>
            
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '10px' }}>
              <span className={`badge ${acc.status === 'Connected' ? 'badge-success' : 'badge-secondary'}`}>
                {acc.status}
              </span>
              
              <button 
                onClick={() => toggleConnection(acc.id, acc.status)}
                className={`btn ${acc.status === 'Connected' ? 'btn-danger' : 'btn-primary'}`} 
                style={{ padding: '6px 12px', fontSize: '0.85rem' }}
              >
                {acc.status === 'Connected' ? 'Disconnect' : 'Connect'}
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
