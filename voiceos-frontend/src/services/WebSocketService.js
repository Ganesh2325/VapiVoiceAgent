import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
    constructor() {
        this.client = null;
        this.subscriptions = new Map();
    }

    connect(conversationId, onMessageReceived) {
        if (this.client && this.client.connected) {
            this.subscribe(conversationId, onMessageReceived);
            return;
        }

        const socketUrl = 'http://localhost:8080/ws';
        
        this.client = new Client({
            webSocketFactory: () => new SockJS(socketUrl),
            debug: (str) => console.log('STOMP: ' + str),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        this.client.onConnect = (frame) => {
            console.log('Connected to WebSocket', frame);
            this.subscribe(conversationId, onMessageReceived);
        };

        this.client.onStompError = (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
        };

        this.client.activate();
    }

    subscribe(conversationId, onMessageReceived) {
        if (!this.client || !this.client.connected || !conversationId) return;
        
        const topic = `/topic/executions/${conversationId}`;
        
        if (this.subscriptions.has(topic)) {
            this.subscriptions.get(topic).unsubscribe();
        }

        const sub = this.client.subscribe(topic, (message) => {
            if (message.body) {
                const data = JSON.parse(message.body);
                onMessageReceived(data);
            }
        });

        this.subscriptions.set(topic, sub);
    }

    disconnect() {
        if (this.client) {
            this.client.deactivate();
        }
    }
}

export const wsService = new WebSocketService();
