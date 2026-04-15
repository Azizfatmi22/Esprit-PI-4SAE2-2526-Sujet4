import { Injectable, OnDestroy, NgZone } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

@Injectable({ providedIn: 'root' })
export class AdminTypingService implements OnDestroy {

  private stompClient: Client | null = null;
  private wsConnected = false;
  private currentTypingId: number | null = null;
  private typingDebounce: any = null;

  constructor(private ngZone: NgZone) {  // ← ajouter NgZone
    this.ngZone.runOutsideAngular(() => {  // ← connexion hors zone (perf)
      this.connect();
    });
  }

  private connect(): void {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8085/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        this.ngZone.run(() => {  // ← rentrer dans la zone
          this.wsConnected = true;
          console.log('[AdminWS] Connecté');
        });
      },
      onDisconnect: () => {
        this.ngZone.run(() => {
          this.wsConnected = false;
        });
      },
      onStompError: (frame) => {
        console.warn('[AdminWS] Erreur:', frame.headers?.['message']);
      }
    });
    this.stompClient.activate();
  }

  sendTyping(reclamationId: number, isTyping: boolean): void {
    if (!this.stompClient || !this.wsConnected) return;

    if (this.currentTypingId && this.currentTypingId !== reclamationId) {
      this.publishTyping(this.currentTypingId, false);
    }
    this.currentTypingId = isTyping ? reclamationId : null;

    if (this.typingDebounce) clearTimeout(this.typingDebounce);

    if (isTyping) {
      this.publishTyping(reclamationId, true);
      this.typingDebounce = setTimeout(() => {
        this.publishTyping(reclamationId, false);
      }, 3000);
    } else {
      this.typingDebounce = setTimeout(() => {
        this.publishTyping(reclamationId, false);
      }, 500);
    }
  }

  private publishTyping(reclamationId: number, isTyping: boolean): void {
    if (!this.stompClient || !this.wsConnected) return;
    this.stompClient.publish({
      destination: `/app/typing/${reclamationId}`,
      body: JSON.stringify({ sender: 'ADMIN', isTyping, reclamationId })
    });
  }

  ngOnDestroy(): void {
    if (this.typingDebounce) clearTimeout(this.typingDebounce);
    if (this.stompClient) this.stompClient.deactivate();
  }
}