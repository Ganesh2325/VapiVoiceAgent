import React, { useState, useEffect, useRef } from 'react';
import VapiImport from '@vapi-ai/web';
import { Phone, PhoneOff, Mic, Send, Terminal, Zap, ArrowRight, Cpu } from 'lucide-react';
import { apiFetch, fetchActionTimeline, fetchLatestAction, fetchVoiceConfig, getAccessToken } from '../services/api';
import { wsService } from '../services/WebSocketService';

function vapiConstructor() {
  if (typeof VapiImport === 'function') {
    return VapiImport;
  }
  if (VapiImport && typeof VapiImport.default === 'function') {
    return VapiImport.default;
  }
  return null;
}

export default function ConsolePage({ isCallActive, setIsCallActive }) {
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [volumeLevel, setVolumeLevel] = useState(0);
  const [isProcessing, setIsProcessing] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [conversationId, setConversationId] = useState(null);

  const [vapiPublicKey, setVapiPublicKey] = useState('');
  const [vapiAssistantId, setVapiAssistantId] = useState('');
  const [vapiConfigured, setVapiConfigured] = useState(false);
  const [sessionBound, setSessionBound] = useState(false);
  const [boundCallId, setBoundCallId] = useState('');
  const [voiceStatus, setVoiceStatus] = useState('Live Vapi is not configured');

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

  const [executionTimeline, setExecutionTimeline] = useState([]);
  const [lastAction, setLastAction] = useState(null);

  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, executionTimeline]);

  useEffect(() => {
    const loadVoiceConfig = async () => {
      if (!getAccessToken()) {
        setVapiConfigured(false);
        setVapiPublicKey('');
        setVapiAssistantId('');
        setVoiceStatus('Log in to start a live Vapi call');
        return;
      }
      const config = await fetchVoiceConfig();
      const publicKey = config.publicKey || '';
      const assistantId = config.assistantId || '';
      const configured = Boolean(config.configured && publicKey && assistantId);
      setVapiPublicKey(publicKey);
      setVapiAssistantId(assistantId);
      setVapiConfigured(configured);
      setVoiceStatus(configured
        ? 'Vapi Voice Agent Ready'
        : 'Live Vapi is not configured (public key / assistant id missing)');
    };
    loadVoiceConfig();
    window.addEventListener('voiceos:auth-changed', loadVoiceConfig);
    window.addEventListener('voiceos:unauthorized', loadVoiceConfig);
    return () => {
      window.removeEventListener('voiceos:auth-changed', loadVoiceConfig);
      window.removeEventListener('voiceos:unauthorized', loadVoiceConfig);
    };
  }, []);

  const applyBackendAction = async (action) => {
    if (!action?.actionId) {
      return;
    }
    const timeline = await fetchActionTimeline(action.actionId);
    const merged = {
      ...action,
      ...(timeline || {}),
      duration: timeline?.durationMs != null ? timeline.durationMs : action.durationMs
    };
    setLastAction(merged);
    if (Array.isArray(merged.timeline) && merged.timeline.length > 0) {
      setExecutionTimeline(merged.timeline.map((item, idx) => ({
        step: idx + 1,
        agent: item.agent || merged.agent || 'ActionEngine',
        action: item.message || item.type,
        status: item.status || merged.status,
        latency: item.timestamp || ''
      })));
    }
  };

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

  useEffect(() => {
    if (!isCallActive || !getAccessToken()) {
      return undefined;
    }
    let cancelled = false;
    const poll = async () => {
      const action = await fetchLatestAction();
      if (!cancelled && action) {
        await applyBackendAction(action);
      }
    };
    poll();
    const timer = setInterval(poll, 2000);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, [isCallActive]);

  // Initialize Vapi SDK instance
  useEffect(() => {
    if (vapiPublicKey) {
      try {
        const Vapi = vapiConstructor();
        if (!Vapi) {
          console.warn('Vapi SDK constructor is unavailable');
          return undefined;
        }
        const vapi = new Vapi(vapiPublicKey);
        vapiRef.current = vapi;

        vapi.on('call-start', () => {
          setIsCallActive(true);
        });

        vapi.on('call-end', () => {
          setIsCallActive(false);
          setIsSpeaking(false);
          setSessionBound(false);
        });

        vapi.on('speech-start', () => setIsSpeaking(true));
        vapi.on('speech-end', () => setIsSpeaking(false));
        vapi.on('volume-level', (vol) => setVolumeLevel(vol));

        vapi.on('message', (msg) => {
          const incomingCallId = msg?.call?.id || msg?.callId;
          if (incomingCallId) {
            apiFetch('/voice/sessions', {
              method: 'POST',
              body: JSON.stringify({ callId: incomingCallId, conversationId })
            }).catch(() => {});
          }
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
      setSessionBound(false);
      return;
    }
    if (!getAccessToken()) {
      setVoiceStatus('Log in to start a live Vapi call');
      return;
    }
    if (!vapiConfigured || !vapiPublicKey || !vapiAssistantId) {
      setVoiceStatus('Live Vapi is not configured (public key / assistant id missing)');
      return;
    }
    try {
      let currentConvId = conversationId;
      if (!currentConvId) {
        const initRes = await apiFetch('/conversations', {
          method: 'POST',
          body: JSON.stringify({ title: 'Live Vapi call' })
        });
        if (initRes.ok) {
          const created = await initRes.json();
          currentConvId = created.id;
          setConversationId(currentConvId);
        }
      }
      const call = await vapiRef.current.start(vapiAssistantId);
      const callId = call?.id;
      if (!callId) {
        setSessionBound(false);
        setVoiceStatus('Vapi SDK did not return a call id; VoiceSession is not bound');
        if (vapiRef.current) vapiRef.current.stop();
        return;
      }
      const bindRes = await apiFetch('/voice/sessions', {
        method: 'POST',
        body: JSON.stringify({ callId, conversationId: currentConvId })
      });
      if (!bindRes.ok) {
        setSessionBound(false);
        setBoundCallId(callId);
        setVoiceStatus('Call started but VoiceSession bind failed');
        return;
      }
      setBoundCallId(callId);
      setSessionBound(true);
      setVoiceStatus('Live Vapi Call Connected');
    } catch (err) {
      console.error('Failed to start Vapi call:', err);
      setVoiceStatus('Failed to start Vapi call');
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
          const initRes = await apiFetch(`/conversations`, { method: 'POST', body: JSON.stringify({ title: text.slice(0, 80) }) });
          if (initRes.status === 401) {
              setMessages(prev => [...prev, {
                  id: Date.now().toString(), role: 'assistant', agent: 'System',
                  content: 'Authentication required. Log in from the header, then retry.',
                  timestamp: new Date().toLocaleTimeString()
              }]);
              return;
          }
          if(initRes.ok) {
              const c = await initRes.json();
              currentConvId = c.id;
              setConversationId(currentConvId);
          }
      }

      const res = await apiFetch(`/agent/execute`, {
        method: 'POST',
        body: JSON.stringify({
          conversationId: currentConvId,
          input: text
        })
      });

      if (res.status === 401) {
        setMessages(prev => [...prev, {
            id: Date.now().toString(), role: 'assistant', agent: 'System',
            content: 'Authentication required. Log in from the header, then retry.',
            timestamp: new Date().toLocaleTimeString()
        }]);
        return;
      }
      if (res.status === 403) {
        setMessages(prev => [...prev, {
            id: Date.now().toString(), role: 'assistant', agent: 'System',
            content: 'You are not authorized to use this conversation.',
            timestamp: new Date().toLocaleTimeString()
        }]);
        return;
      }

      if (res.ok) {
        const data = await res.json();
        const botMsg = {
          id: (Date.now() + 1).toString(),
          role: 'assistant',
          agent: data.activeAgent || data.agent || 'VoiceOS',
          content: data.responseText,
          timestamp: new Date().toLocaleTimeString(),
          tools: data.toolsExecuted || [],
          awaitingApproval: data.awaitingApproval
        };
        setMessages(prev => [...prev, botMsg]);
        setLastAction({
          actionId: data.actionId,
          status: data.status,
          agent: data.agent || data.activeAgent,
          tool: data.tool,
          provider: data.provider,
          providerMode: data.providerMode,
          result: data.result,
          error: data.error,
          duration: data.totalLatencyMs
        });
        if (data.actionId) {
          const timeline = await fetchActionTimeline(data.actionId);
          if (timeline) {
            setLastAction(prev => ({
              ...(prev || {}),
              ...timeline,
              duration: timeline.durationMs != null ? timeline.durationMs : data.totalLatencyMs
            }));
            if (Array.isArray(timeline.timeline) && timeline.timeline.length > 0) {
              setExecutionTimeline(timeline.timeline.map((item, idx) => ({
                step: idx + 1,
                agent: item.agent || timeline.agent || 'ActionEngine',
                action: item.message || item.type,
                status: item.status || timeline.status,
                latency: item.timestamp || ''
              })));
            }
          }
        } else {
          setExecutionTimeline(prev => [
            {
              step: prev.length + 1,
              agent: data.agent || data.activeAgent || 'ActionEngine',
              action: `${data.tool || 'action'} → ${data.status}`,
              status: data.status || 'COMPLETED',
              latency: `${data.totalLatencyMs || 0}ms`
            },
            ...prev
          ]);
        }
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
                {isCallActive
                  ? (sessionBound
                    ? (isSpeaking ? 'Vapi Assistant Speaking...' : 'Live Vapi Call Connected (Listening...)')
                    : 'Call started — VoiceSession not bound')
                  : (vapiConfigured ? 'Vapi Voice Agent Ready' : voiceStatus)}
              </h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                {isCallActive
                  ? (sessionBound
                    ? `Speak to VoiceOS. Bound call ${boundCallId}.`
                    : 'The webhook will reject calculator until this call is bound to your login.')
                  : (vapiConfigured
                    ? 'Click the orb or call button to start a live WebRTC voice session.'
                    : 'Live Vapi requires login plus VAPI_PUBLIC_KEY and VAPI_ASSISTANT_ID on the backend.')}
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
            <span className="badge badge-primary">Action Timeline</span>
          </div>

          {executionTimeline.length === 0 && !lastAction && (
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '12px' }}>
              No actions yet. Execute a request to load a real backend timeline.
            </div>
          )}

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

          {lastAction && (
            <div style={{ marginTop: '20px', padding: '12px', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-sm)' }}>
              <h5 style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '8px' }}>
                Last Action
              </h5>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                <div>actionId: {lastAction.actionId || 'n/a'}</div>
                <div>status: {lastAction.status || 'n/a'}</div>
                <div>agent: {lastAction.agent || 'n/a'}</div>
                <div>intent: {lastAction.intent || 'n/a'}</div>
                <div>tool: {lastAction.tool || 'none'}</div>
                <div>provider: {lastAction.provider || 'n/a'}</div>
                <div>mode: {lastAction.providerMode || 'unavailable'}</div>
                <div>policy: {lastAction.policyDecision || 'unavailable'}</div>
                <div>verification: {lastAction.verificationPassed == null ? 'unavailable' : String(lastAction.verificationPassed)}</div>
                <div>result: {lastAction.result || 'n/a'}</div>
                <div>error: {lastAction.error || 'none'}</div>
                {Array.isArray(lastAction.missingFields) && lastAction.missingFields.length > 0 && (
                  <div>missing: {lastAction.missingFields.join(', ')}</div>
                )}
                <div>duration: {lastAction.duration != null ? `${lastAction.duration}ms` : (lastAction.durationMs != null ? `${lastAction.durationMs}ms` : 'n/a')}</div>
              </div>
              {Array.isArray(lastAction.timeline) && lastAction.timeline.length > 0 && (
                <div style={{ marginTop: '10px' }}>
                  <h5 style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '6px' }}>
                    Backend Timeline
                  </h5>
                  {lastAction.timeline.map((item, idx) => (
                    <div key={idx} style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', marginBottom: '4px' }}>
                      {item.type} {item.stepType ? `(${item.stepType})` : ''} — {item.message || item.status || ''}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          <div style={{ marginTop: '24px' }}>
            <h5 style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '10px' }}>
              Suggested Voice Instructions
            </h5>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              {[
                'Calculate 125 multiplied by 24',
                'What is the product of 125 and 24?',
                'What is dependency injection in Spring Boot?',
                'Plan my trip to Bangalore next Friday',
                'Write a professional email asking my professor for an extension.'
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
