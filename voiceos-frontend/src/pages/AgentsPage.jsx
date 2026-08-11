import React from 'react';
import { Layers, Zap } from 'lucide-react';

export default function AgentsPage() {
  const agents = [
    { name: 'TravelAgent', desc: 'Flights, hotels, and trip itinerary planning', priority: 20, tools: 2, icon: '✈️' },
    { name: 'TaskAgent', desc: 'Creates, tracks, and manages action items', priority: 30, tools: 1, icon: '📋' },
    { name: 'EmailAgent', desc: 'Drafts emails with human-in-the-loop approval', priority: 25, tools: 1, icon: '✉️' },
    { name: 'FinanceAgent', desc: 'Budget estimates, cost calculations, and currency math', priority: 40, tools: 1, icon: '💰' },
    { name: 'ResearchAgent', desc: 'Deep web and document intelligence synthesis', priority: 15, tools: 2, icon: '🔍' },
    { name: 'CalendarAgent', desc: 'Schedule management and conflict detection', priority: 35, tools: 1, icon: '📅' },
    { name: 'MemoryAgent', desc: 'Long-term preference and fact persistence', priority: 10, tools: 2, icon: '🧠' },
    { name: 'DeveloperAgent', desc: 'Repo architecture and PR/issue inspection', priority: 20, tools: 1, icon: '💻' }
  ];

  return (
    <div>
      <div style={{ marginBottom: '20px' }}>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700 }}>Multi-Agent Fleet Registry</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
          Specialized AI agents orchestrated by the VoiceOS Central Engine.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '18px' }}>
        {agents.map((agent, idx) => (
          <div key={idx} className="glass-panel" style={{ padding: '20px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <span style={{ fontSize: '1.5rem' }}>{agent.icon}</span>
                  <div>
                    <h4 style={{ fontWeight: 700, fontSize: '1.05rem' }}>{agent.name}</h4>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Priority: {agent.priority}</span>
                  </div>
                </div>
                <span className="badge badge-primary">{agent.tools} Tool(s)</span>
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', lineHeight: '1.5', marginBottom: '16px' }}>
                {agent.desc}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
