import React, { useState, useEffect, useRef } from 'react';
import Vapi from '@vapi-ai/web';
import { Phone, PhoneOff, Mic, Send, Terminal, Zap, ArrowRight, Cpu } from 'lucide-react';
import { wsService } from '../services/WebSocketService';

const API_BASE = 'http://localhost:8080/api/v1';

export default function ConsolePage({ isCallActive, setIsCallActive }) {
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [volumeLevel, setVolumeLevel] = useState(0);
  const [isProcessing, setIsProcessing] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [conversationId, setConversationId] = useState(null);

  const [vapiPublicKey, setVapiPublicKey] = useState(import.meta.env.VITE_VAPI_PUBLIC_KEY || '');
  const [vapiAssistantId, setVapiAssistantId] = useState(import.meta.env.VITE_VAPI_ASSISTANT_ID || '');

  const vapiRef = useRef(null);
  const chatBottomRef = useRef(null);

  const [messages, setMessages] = useState([
    {
      id: '1',
      role: 'assistant',
      agent: 'VoiceOS Orchestrator',
      content: 'Welcome to **VoiceOS** — your multi-agent AI voice operations platform powered by **Vapi**.\n\nClick the **Start Vapi Voice Call** button or speak/type to instruct your AI team.',
      timestamp: new Date().toLocaleTimeString(),
      tools: []
    }
  ]);

  const [executionTimeline, setExecutionTimeline] = useState([
    { step: 1, agent: 'System', action: 'Console Initialized', status: 'COMPLETED', latency: '0ms' }
  ]);

  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, executionTimeline]);

  // Connect to Spring Boot WebSocket
  useEffect(() => {
    if (conversationId) {
      wsService.connect(conversationId, (data) => {
        setExecutionTimeline(prev => [
          { step: prev.length + 1, agent: data.agent || 'System', action: data.eventType || 'Event', status: 'RUNNING', latency: '...' },
          ...prev
        ]);
      });
    }
    return () => wsService.disconnect();
  }, [conversationId]);

  // Initialize Vapi SDK instance
  useEffect(() => {
    if (vapiPublicKey) {
      try {
        const vapi = new Vapi(vapiPublicKey);
        vapiRef.current = vapi;

        vapi.on('call-start', () => {
          setIsCallActive(true);
          setExecutionTimeline(prev => [
            { step: prev.length + 1, agent: 'Vapi WebRTC', action: 'Voice Session Connected', status: 'COMPLETED', latency: '40ms' },
            ...prev
          ]);
        });

        vapi.on('call-end', () => {
          setIsCallActive(false);
          setIsSpeaking(false);
        });

        vapi.on('speech-start', () => setIsSpeaking(true));
        vapi.on('speech-end', () => setIsSpeaking(false));
        vapi.on('volume-level', (vol) => setVolumeLevel(vol));

        vapi.on('message', (msg) => {
          if (msg.type === 'transcript' && msg.transcriptType === 'final') {
            const newMsg = {
              id: Date.now().toString(),
              role: msg.role === 'assistant' ? 'assistant' : 'user',
              agent: msg.role === 'assistant' ? 'Vapi Voice Agent' : 'User',
              content: msg.transcript,
              timestamp: new Date().toLocaleTimeString()
            };
            setMessages(prev => [...prev, newMsg]);
          }
        });

      } catch (err) {
        console.warn('Vapi instance initialization:', err);
      }
    }
  }, [vapiPublicKey, setIsCallActive]);

  const toggleVapiCall = async () => {
    if (isCallActive) {
      if (vapiRef.current) vapiRef.current.stop();
      setIsCallActive(false);
      return;
    }
    if (!vapiPublicKey || !vapiAssistantId) {
      alert('Vapi keys missing! Please configure them in your environment variables.');
      return;
    }
    try {
      if (vapiRef.current) await vapiRef.current.start(vapiAssistantId);
    } catch (err) {
      console.error('Failed to start Vapi call:', err);
    }
  };

  const handleSendMessage = async (textToSend) => {
    const text = textToSend || transcript;
    if (!text.trim() || isProcessing) return;

    const userMsg = {
      id: Date.now().toString(),
      role: 'user',
      content: text,
      timestamp: new Date().toLocaleTimeString()
    };

    setMessages(prev => [...prev, userMsg]);
    setTranscript('');
    setIsProcessing(true);

    try {
      // Create conversation if not exists
      let currentConvId = conversationId;
      if (!currentConvId) {
          const initRes = await fetch(`http://localhost:8080/api/v1/conversations`, { method: 'POST' });
          if(initRes.ok) {
              const c = await initRes.json();
              currentConvId = c.id;
              setConversationId(currentConvId);
          }
      }

      const res = await fetch(`http://localhost:8080/api/v1/agent/execute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          conversationId: currentConvId,
          input: text
        })
      });

      if (res.ok) {
        const data = await res.json();
        const botMsg = {
          id: (Date.now() + 1).toString(),
          role: 'assistant',
          agent: data.activeAgent || 'VoiceOS Orchestrator',
          content: data.responseText,
          timestamp: new Date().toLocaleTimeString(),
          tools: data.toolsExecuted || [],
          awaitingApproval: data.awaitingApproval
        };
        setMessages(prev => [...prev, botMsg]);
      }
    } catch (err) {
      console.error(err);
      setMessages(prev => [...prev, {
          id: Date.now().toString(), role: 'assistant', agent: 'System', content: 'Network Error: Backend unreachable.', timestamp: new Date().toLocaleTimeString()
      }]);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div style={{ display: 'flex', gap: '24px', flex: 1, minHeight: 0 }}>
      {/* Left: Chat & Voice Interface */}
      <div style={{ flex: 2, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
        
        {/* Vapi Voice Orb Header Banner */}
        <div className="glass-panel" style={{ padding: '20px 24px', marginBottom: '18px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
            <div
              onClick={toggleVapiCall}
              className={isCallActive ? 'voice-orb-active' : ''}
              style={{
                width: '60px', height: '60px', borderRadius: '50%',
                background: isCallActive ? 'var(--accent-gradient)' : 'rgba(255,255,255,0.08)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                cursor: 'pointer', transition: 'var(--transition-normal)',
                border: isCallActive ? '2px solid #ec4899' : '1px solid var(--border-color)'
              }}
            >
              {isCallActive ? <PhoneOff size={26} color="#ffffff" /> : <Phone size={26} color="#818cf8" />}
            </div>
            <div>
              <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '2px' }}>
                {isCallActive ? (isSpeaking ? 'Vapi Assistant Speaking...' : 'Live Vapi Call Connected (Listening...)') : 'Vapi Voice Agent Ready'}
              </h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                {isCallActive ? 'Speak directly to instruct your multi-agent team in real-time.' : 'Click the orb or call button to start live WebRTC voice session.'}
              </p>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <button
              onClick={toggleVapiCall}
              className={`btn ${isCallActive ? 'btn-danger' : 'btn-primary'}`}
              style={{ padding: '10px 20px' }}
            >
              {isCallActive ? <PhoneOff size={18} /> : <Phone size={18} />}
              {isCallActive ? 'End Vapi Call' : 'Start Vapi Voice Call'}
            </button>
          </div>
        </div>

        {/* Messages Feed */}
        <div className="glass-panel" style={{ flex: 1, padding: '20px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '18px' }}>
          {messages.map((msg) => (
            <div
              key={msg.id}
              style={{
                display: 'flex', flexDirection: 'column',
                alignSelf: msg.role === 'user' ? 'flex-end' : 'flex-start',
                maxWidth: '85%'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px', justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start' }}>
                {msg.role === 'assistant' && (
                  <span className="badge badge-primary" style={{ fontSize: '0.7rem' }}>
                    <Cpu size={12} /> {msg.agent || 'Agent'}
                  </span>
                )}
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{msg.timestamp}</span>
              </div>

              <div style={{
                padding: '14px 18px', borderRadius: 'var(--radius-md)',
                background: msg.role === 'user' ? 'var(--accent-primary)' : 'rgba(255,255,255,0.05)',
                color: '#ffffff', border: '1px solid var(--border-color)',
                fontSize: '0.92rem', lineHeight: '1.6', whiteSpace: 'pre-wrap'
              }}>
                {msg.content}
              </div>

              {msg.tools && msg.tools.length > 0 && (
                <div style={{ display: 'flex', gap: '6px', marginTop: '6px', flexWrap: 'wrap' }}>
                  {msg.tools.map((t, idx) => (
                    <span key={idx} className="badge badge-success" style={{ fontSize: '0.68rem' }}>
                      <Zap size={10} /> Tool: {t.toolName || 'tool'} ({t.durationMs || 85}ms)
                    </span>
                  ))}
                </div>
              )}
            </div>
          ))}
          <div ref={chatBottomRef} />
        </div>

        {/* Input Prompt Box */}
        <div style={{ display: 'flex', gap: '10px' }}>
          <input
            type="text"
            value={transcript}
            onChange={(e) => setTranscript(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
            placeholder="Instruct VoiceOS... (e.g. 'Plan trip to Bangalore next Friday')"
            style={{
              flex: 1, padding: '14px 20px', borderRadius: 'var(--radius-md)',
              background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border-color)',
              color: '#ffffff', fontSize: '0.95rem', outline: 'none'
            }}
          />
          <button
            onClick={() => handleSendMessage()}
            disabled={isProcessing || !transcript.trim()}
            className="btn btn-primary"
            style={{ padding: '0 24px' }}
          >
            <Send size={18} />
          </button>
        </div>
      </div>

      {/* Right: Live Agent Execution Timeline */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '16px' }}>
        <div className="glass-panel" style={{ padding: '20px', flex: 1, overflowY: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <h4 style={{ fontSize: '0.95rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Terminal size={16} color="#818cf8" /> Live Agent Execution Timeline
            </h4>
            <span className="badge badge-primary">WebSocket Stream</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {executionTimeline.map((item, idx) => (
              <div
                key={idx}
                style={{
                  display: 'flex', alignItems: 'center', gap: '12px',
                  padding: '10px 14px', background: 'rgba(255,255,255,0.02)',
                  borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)'
                }}
              >
                <div style={{
                  width: '24px', height: '24px', borderRadius: '50%',
                  background: item.status === 'COMPLETED' ? 'rgba(16,185,129,0.2)' : 'rgba(99,102,241,0.2)',
                  color: item.status === 'COMPLETED' ? '#10b981' : '#818cf8',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: '0.75rem', fontWeight: 700
                }}>
                  {item.step}
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>{item.agent}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{item.action}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <span className={`badge ${item.status === 'COMPLETED' ? 'badge-success' : 'badge-primary'}`} style={{ fontSize: '0.65rem' }}>
                    {item.status}
                  </span>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '2px' }}>{item.latency}</div>
                </div>
              </div>
            ))}
          </div>

          <div style={{ marginTop: '24px' }}>
            <h5 style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '10px' }}>
              Suggested Voice Instructions
            </h5>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {[
                'Plan my trip to Bangalore next Friday',
                'What are my saved travel preferences?',
                'Remember that I prefer morning flights'
              ].map((prompt, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSendMessage(prompt)}
                  className="btn btn-secondary"
                  style={{ fontSize: '0.78rem', justifyContent: 'flex-start', textAlign: 'left', padding: '8px 12px' }}
                >
                  <ArrowRight size={12} color="#818cf8" /> {prompt}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
