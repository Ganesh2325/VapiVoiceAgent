import React, { useState, useEffect } from 'react';
import { Shield, AlertTriangle, CheckCircle2, XCircle } from 'lucide-react';

export default function ApprovalsPage({ setPendingApprovalsCount }) {
  const [approvals, setApprovals] = useState([]);

  useEffect(() => {
    fetch('http://localhost:8080/api/v1/approvals', {
      headers: {
        'Authorization': 'Bearer placeholder-if-needed'
      }
    })
      .then(res => res.json())
      .then(data => {
        setApprovals(data);
        setPendingApprovalsCount(data.filter(a => a.status === 'PENDING').length);
      })
      .catch(console.error);
  }, [setPendingApprovalsCount]);

  const handleAction = async (id, action) => {
    try {
      const res = await fetch(`http://localhost:8080/api/v1/approvals/${id}/${action}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(action === 'authenticate' ? { token: 'mock-auth-token' } : action === 'pay' ? { transactionId: 'mock-txn-id' } : {})
      });
      if (res.ok) {
        setApprovals(prev => prev.map(a => a.id === id ? { ...a, status: action === 'reject' ? 'REJECTED' : 'APPROVED' } : a));
        setPendingApprovalsCount(prev => prev - 1);
      }
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div>
      <div style={{ marginBottom: '20px' }}>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700 }}>Human-in-the-Loop Approval Queue</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
          Actions classified as <strong>HIGH</strong> or <strong>CRITICAL</strong> risk require explicit human confirmation before execution.
        </p>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        {approvals.length === 0 && (
          <div style={{ color: 'var(--text-muted)' }}>No pending approvals.</div>
        )}
        {approvals.map((appr) => (
          <div key={appr.id} className="glass-panel" style={{ padding: '22px' }}>
            <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '14px' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
                  <span className={`badge ${appr.riskLevel === 'HIGH' ? 'badge-danger' : 'badge-warning'}`}>
                    <AlertTriangle size={12} /> {appr.riskLevel} RISK
                  </span>
                  <h4 style={{ fontSize: '1.05rem', fontWeight: 700 }}>Action: {appr.actionType}</h4>
                </div>
                <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)' }}>{appr.actionDescription}</p>
              </div>
              <span className={`badge ${appr.status === 'APPROVED' ? 'badge-success' : appr.status === 'REJECTED' ? 'badge-danger' : 'badge-warning'}`}>
                {appr.status}
              </span>
            </div>
            
            <div style={{
              padding: '12px 16px', background: 'rgba(0,0,0,0.25)',
              borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)',
              marginBottom: '16px', fontSize: '0.85rem'
            }}>
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap', color: 'var(--text-muted)' }}>
                {JSON.stringify(appr.payload, null, 2)}
              </pre>
            </div>

            {appr.status === 'PENDING' && (
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                <button onClick={() => handleAction(appr.id, 'reject')} className="btn btn-danger">
                  <XCircle size={16} /> Reject Action
                </button>
                <button onClick={() => handleAction(appr.id, 'approve')} className="btn btn-success">
                  <CheckCircle2 size={16} /> Approve & Dispatch
                </button>
              </div>
            )}
            {appr.status === 'REQUIRES_AUTHENTICATION' && (
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                <button onClick={() => handleAction(appr.id, 'reject')} className="btn btn-danger">
                  <XCircle size={16} /> Cancel
                </button>
                <button onClick={() => handleAction(appr.id, 'authenticate')} className="btn btn-primary">
                  <Shield size={16} /> Complete Authentication
                </button>
              </div>
            )}
            {appr.status === 'REQUIRES_PAYMENT' && (
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                <button onClick={() => handleAction(appr.id, 'reject')} className="btn btn-danger">
                  <XCircle size={16} /> Cancel
                </button>
                <button onClick={() => handleAction(appr.id, 'pay')} className="btn btn-success">
                  <CheckCircle2 size={16} /> Complete Payment
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
