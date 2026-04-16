import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import {
  ReclamationService, Reclamation, ReclamationResponse, TimelineEvent, SatisfactionPayload
} from '../../services/reclamation.service';
// npm install @stomp/stompjs sockjs-client && npm install --save-dev @types/sockjs-client
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { UserService } from '../../services/user.service';
import { User } from '../../../user';



@Component({
  selector: 'app-reclamation',
  templateUrl: './reclamation.component.component.html',
  styleUrls: ['./reclamation.component.component.scss']
})
export class ReclamationComponent implements OnInit, OnDestroy {
  //learnerId = 1;
  currentView: 'list' | 'form' | 'detail' | 'faq' = 'list';

  Currentuser: User | null = null;
  reclamations: Reclamation[] = [];
  filteredReclamations: Reclamation[] = [];
  loading = false;
  submitting = false;
  activeFilter = 'ALL';
  searchQuery = '';
  senderId?: number;
  expandedCards: Set<number> = new Set();
  reclamationResponsesMap: Map<number, ReclamationResponse[]> = new Map();
  timelineMap: Map<number, TimelineEvent[]> = new Map();
  notification: { message: string; type: 'success' | 'error' } | null = null;
  selectedFileName = '';

  learnerReplyMap: Map<number, string> = new Map();
  submittingReply: Set<number> = new Set();

  selectedReclamation: Reclamation | null = null;
  detailResponses: ReclamationResponse[] = [];
  detailTimeline: TimelineEvent[] = [];
  loadingDetail = false;
  detailTab: 'messages' | 'timeline' = 'timeline';

  showTypeSuggestion = false;
suggestedType = '';
suggestedTypeLabel = '';
suggestionConfidence = 0;
suggestionKeywords: string[] = [];
isApplyingSuggestion = false;

showExtractionNotification = false;
extractedInfo: any = {};
isExtracting = false;
private extractDebounceTimer: any;

  satisfactionMap: Map<number, { score: number; comment: string; submitted: boolean }> = new Map();
  submittingSatisfaction: Set<number> = new Set();

  faqItems: { question: string; answer: string; type: string; icon: string }[] = [];
  faqSearch = '';
  expandedFaq: Set<number> = new Set();

  // ── ESTIMATION DE DÉLAI ───────────────────────────────────────────────────────
  private slaDefaults: { [p: number]: number } = { 1: 4, 2: 24, 3: 72 };
  estimationCache: Map<string, number> = new Map();

  // ── RECONNAISSANCE VOCALE ─────────────────────────────────────────────────────
  isRecording = false;
  isRecordingSubject = false;
  isRecordingDesc = false;
  voiceSupported = false;
  private recognition: any = null;
  private activeVoiceTarget: 'subject' | 'description' | null = null;

  // ── WEBSOCKET — TYPING INDICATOR ──────────────────────────────────────────────
  private stompClient: Client | null = null;
  private wsConnected = false;
  // reclamationId → true si l'admin est en train d'écrire
  adminTypingMap: Map<number, boolean> = new Map();
  // Timers de sécurité : effacer l'indicateur si pas de stop reçu
  private typingTimeouts: Map<number, any> = new Map();
  // IDs des topics déjà souscrits (éviter doublons)
  private subscribedIds: Set<number> = new Set();

  // ── CITATIONS ─────────────────────────────────────────────────────────────────
  quotedMessageMap: Map<number, { text: string; author: string } | null> = new Map();
  get learnerId(): string | null {
  return this.Currentuser?.id ?? null;
}

  stats = { total: 0, pending: 0, inProgress: 0, resolved: 0, closed: 0, rejected: 0 };
  getConfidenceColor(confidence: number): string {
    if (confidence >= 70) return '#10b981';
    if (confidence >= 40) return '#f59e0b';
    return '#ef4444';
  }


  reclamationForm: any = {
    learnerId: this.learnerId, type: 'OTHER', subject: '', description: '', priority: 3,
    contactPhone: '', desiredResolutionDate: '', attachmentUrl: '', additionalInfo: '',
    browserName: '', osVersion: '', errorCode: '', errorMessage: '',
    transactionId: '', paymentDate: '', amount: null, paymentMethod: '', invoiceNumber: '',
    courseId: null, lessonId: null, contentType: '', pageUrl: '',
    accessDate: '', deviceType: '', completionDate: '', certificateType: '',
  };

  reclamationTypes = [
    { value: 'TECHNICAL',   label: 'Problème technique',     icon: '⚙️', desc: 'Bug, erreur, panne sur la plateforme' },
    { value: 'PAYMENT',     label: 'Problème de paiement',   icon: '💳', desc: 'Facturation, remboursement, transaction' },
    { value: 'CONTENT',     label: 'Problème de contenu',    icon: '📚', desc: 'Vidéo, PDF, quiz incorrect ou inaccessible' },
    { value: 'ACCESS',      label: "Problème d'accès",       icon: '🔒', desc: 'Connexion, droits, cours verrouillé' },
    { value: 'CERTIFICATE', label: 'Problème de certificat', icon: '🏆', desc: 'Certificat manquant ou erroné' },
    { value: 'OTHER',       label: 'Autre',                  icon: '📌', desc: 'Tout autre type de problème' }
  ];
  priorities = [
    { value: 1, label: 'Haute',   color: 'high',   desc: 'Bloque ma formation' },
    { value: 2, label: 'Moyenne', color: 'medium', desc: 'Impacte ma progression' },
    { value: 3, label: 'Basse',   color: 'low',    desc: 'Gêne mineure' }
  ];
  filters = ['ALL', 'PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED'];
  availableCourses = [
    { id: 1, title: 'Introduction au JavaScript' }, { id: 2, title: 'Angular Avancé' },
    { id: 3, title: 'Spring Boot Microservices' },  { id: 4, title: 'React & TypeScript' },
    { id: 5, title: 'Python pour Data Science' },   { id: 6, title: 'Docker & Kubernetes' },
  ];
  private specificFields: { [key: string]: string[] } = {
    'TECHNICAL':   ['browserName', 'osVersion', 'errorCode', 'errorMessage'],
    'PAYMENT':     ['transactionId', 'paymentDate', 'amount', 'paymentMethod', 'invoiceNumber'],
    'CONTENT':     ['courseId', 'lessonId', 'contentType', 'pageUrl'],
    'ACCESS':      ['courseId', 'accessDate', 'deviceType', 'errorMessage'],
    'CERTIFICATE': ['courseId', 'completionDate', 'certificateType'],
    'OTHER':       []
  };
  showOnboarding = false;
onboardingStep = 0;

  constructor(private reclamationService: ReclamationService,
    private userService: UserService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.Currentuser = this.userService.getUser() || null;
    this.loadReclamations();
    this.buildFaq();
    this.initVoiceRecognition();
    this.initWebSocket();
    this.checkOnboarding();
  }

  ngOnDestroy(): void {
    this.stopRecording();
    this.disconnectWebSocket();
  }

  // ── WEBSOCKET ─────────────────────────────────────────────────────────────────
  // ── WEBSOCKET ─────────────────────────────────────────────────────────────────
private initWebSocket(): void {
  // ← runOutsideAngular seulement pour la connexion, pas pour les subscriptions
  this.ngZone.runOutsideAngular(() => {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8085/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        this.ngZone.run(() => {
          this.wsConnected = true;
          console.log('[WS Front] Connecté, réclamations:', this.reclamations.length);
          this.reclamations.forEach(r => { if (r.id) this.subscribeToTyping(r.id); });
          this.reclamationResponsesMap.forEach((_, id) => { this.subscribeToTyping(id); });
        });
      },
      onDisconnect: () => {
        this.ngZone.run(() => {
          this.wsConnected = false;
          this.subscribedIds.clear();
        });
      },
      onStompError: (frame) => {
        console.warn('[WS Front] Erreur:', frame.headers?.['message']);
      }
    });
    this.stompClient.activate();
  });
}

  private subscribeToTyping(reclamationId: number): void {
  if (!this.stompClient || !this.wsConnected) {
    console.log(`[WS] Pas encore connecté pour rec#${reclamationId}`);
    return;
  }
  if (this.subscribedIds.has(reclamationId)) {
    console.log(`[WS] Déjà souscrit à rec#${reclamationId}`);
    return;
  }
  console.log(`[WS] Souscription à /topic/typing/${reclamationId}`);

  // ← NE PAS utiliser runOutsideAngular ici
  // Le subscribe doit rester dans la zone pour que ngZone.run() fonctionne
  this.stompClient!.subscribe(
  `/topic/typing/${reclamationId}`,
  (message: IMessage) => {
    console.log('[WS FRONT] Message brut:', message.body);
    try {
      const event = JSON.parse(message.body);
      console.log('[WS FRONT] Event parsé:', event, 'sender match:', event.sender?.toUpperCase() === 'ADMIN');

      if (event.sender?.toUpperCase() === 'ADMIN') {

        this.ngZone.run(() => {
          // ← Créer un nouveau Map pour forcer la détection Angular
          this.adminTypingMap = new Map(this.adminTypingMap);
          this.adminTypingMap.set(reclamationId, event.isTyping);

          const existing = this.typingTimeouts.get(reclamationId);
          if (existing) clearTimeout(existing);

          if (event.isTyping) {
            const timer = setTimeout(() => {
              this.ngZone.run(() => {
                this.adminTypingMap = new Map(this.adminTypingMap);
                this.adminTypingMap.set(reclamationId, false);
                this.typingTimeouts.delete(reclamationId);
                this.cdr.detectChanges();
              });
            }, 6000);
            this.typingTimeouts.set(reclamationId, timer);
          } else {
            this.typingTimeouts.delete(reclamationId);
          }

          this.cdr.detectChanges(); // ← forcer la détection
        });

      }
    } catch (e) {
      console.warn('[WS] Erreur parsing:', e);
    }
  }
);

  this.subscribedIds.add(reclamationId);
}

  private disconnectWebSocket(): void {
    this.typingTimeouts.forEach(t => clearTimeout(t));
    this.typingTimeouts.clear();
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }

  isAdminTyping(reclamationId: number | undefined): boolean {
    if (!reclamationId) return false;
    return this.adminTypingMap.get(reclamationId) === true;
  }

  // ── ESTIMATION ────────────────────────────────────────────────────────────────
  private buildEstimationCache(): void {
    const resolved = this.reclamations.filter(r =>
      (r.status === 'RESOLVED' || r.status === 'CLOSED') && r.createdDate && r.resolvedDate
    );
    const groups: { [key: string]: number[] } = {};
    for (const r of resolved) {
      const hours = (new Date(r.resolvedDate!).getTime() - new Date(r.createdDate!).getTime()) / 3600000;
      if (hours > 0 && hours < 720) {
        const key = `${r.type}_${r.priority || 3}`;
        if (!groups[key]) groups[key] = [];
        groups[key].push(hours);
      }
    }
    this.estimationCache.clear();
    for (const [key, values] of Object.entries(groups)) {
      this.estimationCache.set(key, Math.round(values.reduce((a, b) => a + b, 0) / values.length));
    }
  }

  getEstimatedDelay(rec: Reclamation): string | null {
    if (['RESOLVED', 'CLOSED', 'REJECTED'].includes(rec.status || '')) return null;
    const key = `${rec.type}_${rec.priority || 3}`;
    let hours: number;
    if (this.estimationCache.has(key)) {
      hours = this.estimationCache.get(key)!;
    } else {
      const fallback = Array.from(this.estimationCache.entries()).find(([k]) => k.startsWith(rec.type + '_'));
      hours = fallback ? fallback[1] : (this.slaDefaults[rec.priority || 3] || 48);
    }
    return this.formatHours(hours);
  }

  get formTypeEstimation(): string | null {
    const key = `${this.reclamationForm.type}_${this.reclamationForm.priority}`;
    const hours = this.estimationCache.has(key)
      ? this.estimationCache.get(key)!
      : (this.slaDefaults[this.reclamationForm.priority || 3] || 48);
    return this.formatHours(hours);
  }

  private formatHours(h: number): string {
    if (h < 1)   return 'moins d\'1h';
    if (h < 24)  return `environ ${Math.round(h)}h`;
    if (h < 48)  return 'environ 1 jour';
    if (h < 168) return `environ ${Math.round(h / 24)} jours`;
    return `environ ${Math.round(h / 168)} semaine(s)`;
  }

  getEstimationClass(rec: Reclamation): string {
    if (rec.priority === 1) return 'est--high';
    if (rec.priority === 2) return 'est--medium';
    return 'est--low';
  }

  // ── VOIX ──────────────────────────────────────────────────────────────────────
  private initVoiceRecognition(): void {
    const SpeechAPI = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechAPI) { this.voiceSupported = false; return; }
    this.voiceSupported = true;
    this.recognition = new SpeechAPI();
    this.recognition.lang = 'fr-FR';
    this.recognition.continuous = false;
    this.recognition.interimResults = true;
    this.recognition.onresult = (event: any) => {
      let transcript = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        transcript += event.results[i][0].transcript;
      }
      if (this.activeVoiceTarget === 'subject')          this.reclamationForm.subject = transcript;
      else if (this.activeVoiceTarget === 'description') this.reclamationForm.description = transcript;
    };
    this.recognition.onend = () => {
      this.isRecording = false; this.isRecordingSubject = false;
      this.isRecordingDesc = false; this.activeVoiceTarget = null;
    };
    this.recognition.onerror = (event: any) => {
      this.isRecording = false; this.isRecordingSubject = false;
      this.isRecordingDesc = false; this.activeVoiceTarget = null;
      if (event.error !== 'no-speech') this.showNotification('Erreur microphone : ' + event.error, 'error');
    };
  }

  startRecording(target: 'subject' | 'description'): void {
    if (!this.voiceSupported) { this.showNotification('Reconnaissance vocale non supportée (utilisez Chrome)', 'error'); return; }
    if (this.isRecording) { this.stopRecording(); return; }
    this.activeVoiceTarget = target;
    this.isRecording = true;
    this.isRecordingSubject = target === 'subject';
    this.isRecordingDesc = target === 'description';
    try { this.recognition.start(); } catch (e) { this.isRecording = false; this.isRecordingSubject = false; this.isRecordingDesc = false; }
  }

  stopRecording(): void {
    if (this.recognition && this.isRecording) { try { this.recognition.stop(); } catch (e) {} }
    this.isRecording = false; this.isRecordingSubject = false;
    this.isRecordingDesc = false; this.activeVoiceTarget = null;
  }

  // ── NAVIGATION ────────────────────────────────────────────────────────────────
  goToForm(): void { this.resetForm(); this.currentView = 'form'; window.scrollTo({ top: 0, behavior: 'smooth' }); }
  goToList(): void { this.currentView = 'list'; window.scrollTo({ top: 0, behavior: 'smooth' }); }
  goToFaq():  void { this.currentView = 'faq';  window.scrollTo({ top: 0, behavior: 'smooth' }); }

  openDetail(rec: Reclamation): void {
    this.selectedReclamation = { ...rec };
    this.currentView = 'detail'; this.detailTab = 'timeline'; this.loadingDetail = true;
    this.reclamationService.getPublicResponses(rec.id!).subscribe({
      next: (res) => {
        this.detailResponses = res.sort((a, b) => new Date(a.createdDate || '').getTime() - new Date(b.createdDate || '').getTime());
        this.detailTimeline = this.reclamationService.buildTimeline(rec, this.detailResponses);
        this.loadingDetail = false;
        // S'assurer qu'on écoute le typing pour cette réclamation
        this.subscribeToTyping(rec.id!);
      },
      error: () => { this.detailResponses = []; this.loadingDetail = false; }
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // ── FORM ──────────────────────────────────────────────────────────────────────
  showField(name: string): boolean { return (this.specificFields[this.reclamationForm.type] || []).includes(name); }
  getSpecificSectionTitle(): string {
    const map: { [k: string]: string } = { 'TECHNICAL': 'Détails techniques', 'PAYMENT': 'Informations de paiement', 'CONTENT': 'Contenu concerné', 'ACCESS': "Détails d'accès", 'CERTIFICATE': 'Détails du certificat' };
    return map[this.reclamationForm.type] || '';
  }
  hasSpecificFields(): boolean { return (this.specificFields[this.reclamationForm.type] || []).length > 0; }
  get formProgress(): number {
    const required = [this.reclamationForm.type, this.reclamationForm.subject, this.reclamationForm.description];
    const optional = [this.reclamationForm.contactPhone, this.reclamationForm.desiredResolutionDate, this.reclamationForm.additionalInfo, this.reclamationForm.attachmentUrl];
    const specific = (this.specificFields[this.reclamationForm.type] || []).map((f: string) => this.reclamationForm[f]).filter(Boolean);
    const total = required.length + optional.length;
    const filled = required.filter(Boolean).length + optional.filter(Boolean).length;
    return Math.min(100, Math.round(((filled + specific.length * 0.5) / total) * 100));
  }
  isFormValid(): boolean { return !!(this.reclamationForm.subject?.trim() && this.reclamationForm.description?.trim()); }

  // ── DATA ──────────────────────────────────────────────────────────────────────
  loadReclamations(): void {
  this.loading = true;
  const learnerId = this.learnerId;
  if (!learnerId) {
    this.loading = false;
    this.showNotification('Utilisateur non connecté', 'error');
    return;
  }
  this.reclamationService.getReclamationsByLearner(this.learnerId).subscribe({
    next: (data) => {
      this.reclamations = data.sort((a, b) =>
        new Date(b.createdDate || '').getTime() - new Date(a.createdDate || '').getTime()
      );
      this.applyFilters();
      this.computeStats();
      this.buildEstimationCache();
      this.loading = false;

      // ← Charger les réponses ET s'abonner au typing
      data.forEach(r => {
        if (r.id) this.loadAllResponses(r.id);
      });

      // ← Si le WS est déjà connecté au moment où les données arrivent
      // (cas où HTTP est plus lent que WS)
      if (this.wsConnected) {
        data.forEach(r => {
          if (r.id) this.subscribeToTyping(r.id);
        });
      }
      // Sinon, onConnect() les prend en charge via this.reclamations
    },
    error: () => {
      this.loading = false;
      this.showNotification('Erreur de chargement', 'error');
    }
  });
}

  loadAllResponses(id: number): void {
  this.reclamationService.getPublicResponses(id).subscribe({
    next: (res) => {
      const sorted = res.sort((a, b) =>
        new Date(a.createdDate || '').getTime() - new Date(b.createdDate || '').getTime()
      );
      // ← Créer un nouveau Map pour forcer la détection
      this.reclamationResponsesMap = new Map(this.reclamationResponsesMap);
      this.reclamationResponsesMap.set(id, sorted);
      const rec = this.reclamations.find(r => r.id === id);
      if (rec) this.timelineMap.set(id, this.reclamationService.buildTimeline(rec, sorted));
      if (this.wsConnected) this.subscribeToTyping(id);
      this.cdr.detectChanges(); // ← forcer la détection
    },
    error: () => this.reclamationResponsesMap.set(id, [])
  });
}
  computeStats(): void {
    this.stats = {
      total:      this.reclamations.length,
      pending:    this.reclamations.filter(r => r.status === 'PENDING').length,
      inProgress: this.reclamations.filter(r => r.status === 'IN_PROGRESS').length,
      resolved:   this.reclamations.filter(r => r.status === 'RESOLVED').length,
      closed:     this.reclamations.filter(r => r.status === 'CLOSED').length,
      rejected:   this.reclamations.filter(r => r.status === 'REJECTED').length,
    };
  }

  applyFilters(): void {
    let result = [...this.reclamations];
    if (this.activeFilter !== 'ALL') result = result.filter(r => r.status === this.activeFilter);
    if (this.searchQuery.trim()) { const q = this.searchQuery.toLowerCase(); result = result.filter(r => r.subject.toLowerCase().includes(q) || r.description.toLowerCase().includes(q)); }
    this.filteredReclamations = result;
  }

  setFilter(f: string): void { this.activeFilter = f; this.applyFilters(); }
  onSearch(): void { this.applyFilters(); }
  getFilterCount(f: string): number { if (f === 'ALL') return this.reclamations.length; return this.reclamations.filter(r => r.status === f).length; }
  getPublicResponses(id: number | undefined): ReclamationResponse[] { if (!id) return []; return this.reclamationResponsesMap.get(id) || []; }
  getTimeline(id: number | undefined): TimelineEvent[] { if (!id) return []; return this.timelineMap.get(id) || []; }
  toggleCard(id: number | undefined): void { if (!id) return; this.expandedCards.has(id) ? this.expandedCards.delete(id) : this.expandedCards.add(id); }
  isExpanded(id: number | undefined): boolean { return !!id && this.expandedCards.has(id); }

  // ── RÉPONSE APPRENANT ─────────────────────────────────────────────────────────
  getLearnerReply(id: number): string { return this.learnerReplyMap.get(id) || ''; }
  setLearnerReply(id: number, text: string): void { this.learnerReplyMap.set(id, text); }
  canLearnerReply(rec: Reclamation): boolean { return rec.status === 'PENDING' || rec.status === 'IN_PROGRESS'; }

  isLearnerMessage(resp: ReclamationResponse): boolean {
    if (resp.senderType) return resp.senderType === 'LEARNER';
    return resp.learnerId?.toString() === this.learnerId;
  }

  submitLearnerReply(rec: Reclamation): void {
    const text = this.getLearnerReply(rec.id!);
    if (!text.trim() || !rec.id || !this.learnerId) return;
    this.submittingReply.add(rec.id);
    this.reclamationService.createResponse(rec.id, this.learnerId, text.trim(), false, 'LEARNER').subscribe({
      next: () => { this.learnerReplyMap.set(rec.id!, ''); this.loadAllResponses(rec.id!); this.submittingReply.delete(rec.id!); this.showNotification('Message envoyé !', 'success'); },
      error: () => { this.submittingReply.delete(rec.id!); this.showNotification('Erreur lors de l\'envoi', 'error'); }
    });
  }

  // ── CITATIONS ─────────────────────────────────────────────────────────────────
  quoteMessage(recId: number, resp: ReclamationResponse): void {
    const author = this.isLearnerMessage(resp) ? 'Vous' : 'Administrateur';
    this.quotedMessageMap.set(recId, { text: resp.responseText, author });
    setTimeout(() => {
      const ta = document.querySelector(`#reply-ta-${recId}`) as HTMLTextAreaElement;
      if (ta) ta.focus();
    }, 50);
  }

  cancelQuote(recId: number): void { this.quotedMessageMap.set(recId, null); }
  getQuote(recId: number): { text: string; author: string } | null { return this.quotedMessageMap.get(recId) || null; }

  submitLearnerReplyWithQuote(rec: Reclamation): void {
    const text = this.getLearnerReply(rec.id!);
  if (!text.trim() || !rec.id || !this.learnerId) return;  // ← guard null
  const quote = this.getQuote(rec.id);
  this.submittingReply.add(rec.id);
  this.reclamationService.createResponseWithQuote(
    rec.id, this.learnerId, text.trim(), false, 'LEARNER', quote?.text, quote?.author  // ← learnerId garanti non-null
  ).subscribe({
      next: () => {
        this.learnerReplyMap.set(rec.id!, '');
        this.quotedMessageMap.set(rec.id!, null);
        this.loadAllResponses(rec.id!);
        this.submittingReply.delete(rec.id!);
        this.showNotification('Message envoyé !', 'success');
      },
      error: () => { this.submittingReply.delete(rec.id!); this.showNotification('Erreur lors de l\'envoi', 'error'); }
    });
  }

  // ── RÉACTIONS ─────────────────────────────────────────────────────────────────
  react(resp: ReclamationResponse, emoji: string): void {
    if (!resp.id) return;
    this.reclamationService.addReaction(resp.id, emoji).subscribe({
      next: () => {
        this.reclamationResponsesMap.forEach((responses, recId) => {
          const idx = responses.findIndex((r: ReclamationResponse) => r.id === resp.id);
          if (idx !== -1) { responses[idx] = { ...responses[idx], reaction: emoji }; this.reclamationResponsesMap.set(recId, [...responses]); }
        });
        const idx = this.detailResponses.findIndex((r: ReclamationResponse) => r.id === resp.id);
        if (idx !== -1) this.detailResponses[idx] = { ...this.detailResponses[idx], reaction: emoji };
      },
      error: () => this.showNotification('Erreur lors de la réaction', 'error')
    });
  }

  removeReaction(resp: ReclamationResponse): void {
    if (!resp.id) return;
    this.reclamationService.addReaction(resp.id, '').subscribe({
      next: () => {
        this.reclamationResponsesMap.forEach((responses, recId) => {
          const idx = responses.findIndex((r: ReclamationResponse) => r.id === resp.id);
          if (idx !== -1) { responses[idx] = { ...responses[idx], reaction: undefined }; this.reclamationResponsesMap.set(recId, [...responses]); }
        });
        const idx = this.detailResponses.findIndex((r: ReclamationResponse) => r.id === resp.id);
        if (idx !== -1) this.detailResponses[idx] = { ...this.detailResponses[idx], reaction: undefined };
      },
      error: () => {}
    });
  }

  // ── SATISFACTION ──────────────────────────────────────────────────────────────
  needsSatisfaction(rec: Reclamation): boolean { return (rec.status === 'RESOLVED' || rec.status === 'CLOSED') && !rec.satisfactionScore; }
  getSatisfactionDraft(id: number): { score: number; comment: string; submitted: boolean } {
    if (!this.satisfactionMap.has(id)) this.satisfactionMap.set(id, { score: 0, comment: '', submitted: false });
    return this.satisfactionMap.get(id)!;
  }
  setSatisfactionScore(id: number, score: number): void { this.getSatisfactionDraft(id).score = score; }
  setSatisfactionComment(id: number, comment: string): void { this.getSatisfactionDraft(id).comment = comment; }
  submitSatisfaction(rec: Reclamation): void {
    const draft = this.getSatisfactionDraft(rec.id!);
  if (!draft.score) { this.showNotification('Veuillez choisir une note', 'error'); return; }
  if (!this.learnerId) return;  // ← guard null
  this.submittingSatisfaction.add(rec.id!);
  this.reclamationService.submitSatisfaction(rec.id!, { 
    score: draft.score, 
    comment: draft.comment, 
    learnerId: this.learnerId  // ← garanti string ici
  }).subscribe({
      next: (updated) => {
        const idx = this.reclamations.findIndex(r => r.id === rec.id);
        if (idx !== -1) this.reclamations[idx] = updated;
        draft.submitted = true; this.submittingSatisfaction.delete(rec.id!);
        this.showNotification('Merci pour votre évaluation !', 'success');
      },
      error: () => { this.submittingSatisfaction.delete(rec.id!); this.showNotification('Erreur lors de l\'envoi', 'error'); }
    });
  }
  starLabel(score: number): string { return ['', 'Très insatisfait', 'Insatisfait', 'Neutre', 'Satisfait', 'Très satisfait'][score] || ''; }

  // ── FAQ ───────────────────────────────────────────────────────────────────────
  private buildFaq(): void {
    this.faqItems = [
      { question: 'Mon paiement a été débité mais je n\'ai pas accès au cours', answer: 'Vérifiez votre email de confirmation. Si le problème persiste, soumettez une réclamation "Problème de paiement" avec votre référence de transaction.', type: 'PAYMENT', icon: '💳' },
      { question: 'Une vidéo de cours ne se charge pas', answer: 'Videz le cache du navigateur et rechargez. Si le problème persiste, soumettez une réclamation "Problème technique" avec le message d\'erreur.', type: 'TECHNICAL', icon: '⚙️' },
      { question: 'Je ne reçois pas mon certificat', answer: 'Disponible sous 48h après la complétion. Vérifiez que vous avez validé tous les modules. Sinon, soumettez une réclamation "Certificat" avec la date de complétion.', type: 'CERTIFICATE', icon: '🏆' },
      { question: 'Combien de temps prend le traitement ?', answer: 'Haute : 4h. Moyenne : 24h. Basse : 72h. Vous serez notifié dès la prise en charge.', type: 'OTHER', icon: '📌' },
      { question: 'Je n\'arrive plus à me connecter', answer: 'Utilisez "Mot de passe oublié". Si le compte est bloqué, soumettez une réclamation "Accès" avec le message d\'erreur.', type: 'ACCESS', icon: '🔒' },
      { question: 'Un quiz affiche des réponses incorrectes', answer: 'Signalez via une réclamation "Contenu" en précisant la question et la correction.', type: 'CONTENT', icon: '📚' },
      { question: 'Puis-je modifier une réclamation soumise ?', answer: 'Non, mais vous pouvez ajouter des informations dans le fil de discussion tant que le statut est "En attente" ou "En cours".', type: 'OTHER', icon: '📌' },
    ];
  }
  get filteredFaq() {
    if (!this.faqSearch.trim()) return this.faqItems;
    const q = this.faqSearch.toLowerCase();
    return this.faqItems.filter(f => f.question.toLowerCase().includes(q) || f.answer.toLowerCase().includes(q));
  }
  toggleFaq(i: number): void { this.expandedFaq.has(i) ? this.expandedFaq.delete(i) : this.expandedFaq.add(i); }

  // ── FORM SUBMIT ───────────────────────────────────────────────────────────────
  resetForm(): void {
    this.reclamationForm = {
      learnerId: this.learnerId, type: 'OTHER', subject: '', description: '', priority: 3,
      contactPhone: '', desiredResolutionDate: '', attachmentUrl: '', additionalInfo: '',
      browserName: '', osVersion: '', errorCode: '', errorMessage: '',
      transactionId: '', paymentDate: '', amount: null, paymentMethod: '', invoiceNumber: '',
      courseId: null, lessonId: null, contentType: '', pageUrl: '',
      accessDate: '', deviceType: '', completionDate: '', certificateType: '',
    };
    this.selectedFileName = '';
    this.stopRecording();
  }

  onFileSelected(event: any): void {
    const file = event.target.files?.[0];
    if (file) { this.selectedFileName = file.name; this.reclamationForm.attachmentUrl = file.name; }
  }

  submitReclamation(): void {
    if (!this.isFormValid()) { this.showNotification('Veuillez remplir les champs obligatoires (*)', 'error'); return; }
    this.stopRecording(); this.submitting = true;
    const f = this.reclamationForm;
    const payload: any = {
      learnerId: f.learnerId, type: f.type, subject: f.subject, description: f.description, priority: f.priority,
      contactPhone: f.contactPhone || undefined, desiredResolutionDate: f.desiredResolutionDate || undefined,
      attachmentUrl: f.attachmentUrl || undefined, additionalInfo: f.additionalInfo || undefined,
      browserName: f.browserName || undefined, osVersion: f.osVersion || undefined,
      errorCode: f.errorCode || undefined, errorMessage: f.errorMessage || undefined,
      transactionId: f.transactionId || undefined, paymentDate: f.paymentDate || undefined,
      amount: f.amount || undefined, paymentMethod: f.paymentMethod || undefined,
      invoiceNumber: f.invoiceNumber || undefined, courseId: f.courseId || undefined,
      lessonId: f.lessonId || undefined, contentType: f.contentType || undefined,
      pageUrl: f.pageUrl || undefined, accessDate: f.accessDate || undefined,
      deviceType: f.deviceType || undefined, completionDate: f.completionDate || undefined,
      certificateType: f.certificateType || undefined,
    };
    this.reclamationService.createReclamation(payload).subscribe({
      next: (r) => {
        this.reclamations.unshift(r); this.computeStats(); this.applyFilters(); this.buildEstimationCache();
        this.submitting = false; this.showNotification('Réclamation soumise !', 'success'); this.goToList();
      },
      error: () => { this.submitting = false; this.showNotification('Erreur lors de la soumission.', 'error'); }
    });
  }

  deleteReclamation(id: number | undefined, e: Event): void {
    e.stopPropagation();
    if (!id || !confirm('Supprimer cette réclamation ?')) return;
    this.reclamationService.deleteReclamation(id).subscribe({
      next: () => { this.reclamations = this.reclamations.filter(r => r.id !== id); this.computeStats(); this.applyFilters(); this.buildEstimationCache(); this.showNotification('Réclamation supprimée', 'success'); },
      error: () => this.showNotification('Erreur lors de la suppression', 'error')
    });
  }

  // ── HELPERS ───────────────────────────────────────────────────────────────────
  showNotification(message: string, type: 'success' | 'error'): void { this.notification = { message, type }; setTimeout(() => this.notification = null, 3500); }
  getStatusConfig(status: string): { label: string; css: string; icon: string } {
    const map: any = { 'PENDING': { label: 'En attente', css: 'pending', icon: '⏳' }, 'IN_PROGRESS': { label: 'En cours', css: 'in-progress', icon: '🔄' }, 'RESOLVED': { label: 'Résolue', css: 'resolved', icon: '✅' }, 'CLOSED': { label: 'Fermée', css: 'closed', icon: '🔒' }, 'REJECTED': { label: 'Rejetée', css: 'rejected', icon: '❌' } };
    return map[status] || { label: status, css: 'pending', icon: '❓' };
  }
  getTypeConfig(type: string): { label: string; icon: string } { const t = this.reclamationTypes.find(x => x.value === type); return { label: t?.label || type, icon: t?.icon || '📌' }; }
  getPriorityConfig(p: number | undefined): { label: string; css: string } { const map: any = { 1: { label: 'Haute', css: 'high' }, 2: { label: 'Moyenne', css: 'medium' }, 3: { label: 'Basse', css: 'low' } }; return map[p || 3] || { label: 'Basse', css: 'low' }; }
  formatDate(d: string | undefined): string { if (!d) return '-'; return new Date(d).toLocaleDateString('fr-FR', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }); }
  formatRelativeDate(d: string | undefined): string {
    if (!d) return '-';
    const days = Math.floor((Date.now() - new Date(d).getTime()) / 86400000);
    if (days === 0) return "Aujourd'hui"; if (days === 1) return 'Hier'; if (days < 7) return `Il y a ${days} jours`;
    return this.formatDate(d);
  }
  get todayDate(): string { return new Date().toISOString().split('T')[0]; }

  readonly onboardingSteps = [
  {
    icon: '📝',
    title: 'Décrivez votre problème',
    desc: 'Choisissez le type de réclamation et décrivez votre problème clairement. Le formulaire s\'adapte automatiquement selon votre choix.',
    tip: '💡 Plus vous êtes précis, plus vite nous pouvons vous aider !',
    color: '#5b4fcf',
    bg: '#ede9ff'
  },
  {
    icon: '⚡',
    title: 'Suivi en temps réel',
    desc: 'Suivez l\'avancement de votre réclamation à chaque étape. Vous recevez une notification dès qu\'un admin répond.',
    tip: '🔔 L\'indicateur animé vous montre quand le support est en train de vous répondre.',
    color: '#2563eb',
    bg: '#eff6ff'
  },
  {
    icon: '✅',
    title: 'Échangez avec le support',
    desc: 'Répondez directement dans le fil de discussion, citez les messages et évaluez la qualité de l\'aide reçue.',
    tip: '⭐ Votre avis nous aide à améliorer notre service !',
    color: '#059669',
    bg: '#ecfdf5'
  }
];

private checkOnboarding(): void {
  const seen = localStorage.getItem('onboarding_seen');
  if (!seen) {
    setTimeout(() => { this.showOnboarding = true; }, 600);
  }
}

nextOnboardingStep(): void {
  if (this.onboardingStep < this.onboardingSteps.length - 1) {
    this.onboardingStep++;
  } else {
    this.finishOnboarding();
  }
}

prevOnboardingStep(): void {
  if (this.onboardingStep > 0) this.onboardingStep--;
}

finishOnboarding(): void {
  this.showOnboarding = false;
  localStorage.setItem('onboarding_seen', '1');
}

skipOnboarding(): void {
  this.finishOnboarding();
}

onTextChange(): void {
  console.log('🟢 onTextChange appelé');  // ← LOG 1
  if (this.debounceTimer) clearTimeout(this.debounceTimer);
  this.debounceTimer = setTimeout(() => {
    console.log('🟢 Debounce terminé, autoDetectType appelé'); // ← LOG 2
    if (this.reclamationForm.subject?.length > 10 || 
        this.reclamationForm.description?.length > 20) {
      console.log('🟢 Conditions remplies, appel autoDetectType'); // ← LOG 3
      this.autoDetectType();
    } else {
      console.log('🔴 Texte trop court:', 
        'sujet:', this.reclamationForm.subject?.length, 
        'description:', this.reclamationForm.description?.length);
    }
  }, 800);
}


private debounceTimer: any;

autoDetectType(): void {
  console.log('🟢 autoDetectType appelé'); // ← LOG 4
  const subject = this.reclamationForm.subject;
  const description = this.reclamationForm.description;
  
  console.log('Sujet:', subject);
  console.log('Description:', description);
  
  if (!subject && !description) {
    console.log('🔴 Pas de texte à analyser');
    return;
  }
  
  this.reclamationService.detectReclamationType(subject, description)
    .subscribe({
      next: (result) => {
        console.log('🟢 Réponse reçue:', result); // ← LOG 5
        if (result.hasSuggestion && result.suggestedType !== this.reclamationForm.type) {
          console.log('🟢 Suggestion à afficher:', result.suggestedLabel);
          this.showTypeSuggestion = true;
          this.suggestedType = result.suggestedType!;
          this.suggestedTypeLabel = result.suggestedLabel!;
          this.suggestionConfidence = result.confidence || 0;
          this.suggestionKeywords = result.matchedKeywords || [];
        } else {
          console.log('🔴 Pas de suggestion ou type déjà sélectionné');
          this.showTypeSuggestion = false;
        }
      },
      error: (err) => {
        console.error('🔴 Erreur API:', err); // ← LOG 6
      }
    });
}

applySuggestion(): void {
  this.isApplyingSuggestion = true;
  this.reclamationForm.type = this.suggestedType;
  this.showTypeSuggestion = false;
  this.isApplyingSuggestion = false;
  
  // Notification visuelle
  this.showNotification(`Type changé en "${this.suggestedTypeLabel}"`, 'success');
}

dismissSuggestion(): void {
  this.showTypeSuggestion = false;
}

// Ajouter ces méthodes dans la classe ReclamationComponent

getConfidenceDashArray(confidence: number): string {
  const percent = confidence / 100;
  const dashArray = 2 * Math.PI * 15.9155; // Circonférence du cercle
  const offset = dashArray * (1 - percent);
  return `${dashArray - offset} ${dashArray}`;
}

getCategoryColor(type: string): string {
  const colors: Record<string, string> = {
    'PAYMENT': '#10b981',
    'TECHNICAL': '#3b82f6',
    'ACCESS': '#8b5cf6',
    'CONTENT': '#f59e0b',
    'CERTIFICATE': '#06b6d4',
    'OTHER': '#6b7280'
  };
  return colors[type] || '#6366f1';
}

getCategoryIcon(type: string): string {
  const icons: Record<string, string> = {
    'PAYMENT': '💳',
    'TECHNICAL': '⚙️',
    'ACCESS': '🔒',
    'CONTENT': '📚',
    'CERTIFICATE': '🏆',
    'OTHER': '📌'
  };
  return icons[type] || '🤖';
}

onDescriptionChange(): void {
  if (this.extractDebounceTimer) clearTimeout(this.extractDebounceTimer);
  this.extractDebounceTimer = setTimeout(() => {
    const text = this.reclamationForm.description;
    if (text && text.length > 15) {
      this.extractInfo(text);
    }
  }, 1000);
}

// Extraction des informations
extractInfo(text: string): void {
  this.isExtracting = true;
  this.reclamationService.extractInformation(text).subscribe({
    next: (result) => {
      this.isExtracting = false;
      if (result.hasData) {
        this.extractedInfo = result;
        
        // Pré-remplir les champs
        if (result.amount && this.showField('amount')) {
          this.reclamationForm.amount = result.amount;
        }
        if (result.transactionId && this.showField('transactionId')) {
          this.reclamationForm.transactionId = result.transactionId;
        }
        if (result.errorCode && this.showField('errorCode')) {
          this.reclamationForm.errorCode = result.errorCode;
        }
        if (result.email) {
          this.reclamationForm.contactPhone = result.email;
        }
        if (result.extractedDate) {
          if (this.showField('paymentDate')) {
            this.reclamationForm.paymentDate = result.extractedDate;
          }
          if (this.showField('accessDate')) {
            this.reclamationForm.accessDate = result.extractedDate;
          }
        }
        if (result.invoiceNumber && this.showField('invoiceNumber')) {
          this.reclamationForm.invoiceNumber = result.invoiceNumber;
        }
        
        // Afficher la notification
        this.showExtractionNotification = true;
        setTimeout(() => {
          this.showExtractionNotification = false;
        }, 5000);
      }
    },
    error: () => {
      this.isExtracting = false;
    }
  });
}

// Fermer la notification
dismissExtractionNotification(): void {
  this.showExtractionNotification = false;
}


}