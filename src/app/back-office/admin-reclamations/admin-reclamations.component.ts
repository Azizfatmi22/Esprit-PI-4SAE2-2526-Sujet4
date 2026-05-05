import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { ReclamationService, Reclamation, ReclamationResponse, ReclamationStats, SlaAlert }
  from '../../front-office/services/reclamation.service';
import { AdminTypingService } from '../../front-office/services/admin-typing.service';
import { UserService } from '../../front-office/services/user.service';

type SortField = 'id' | 'date' | 'status' | 'type' | 'priority' | 'gravity';
type SortDirection = 'asc' | 'desc';
type ViewMode = 'table' | 'detail';

@Component({
  selector: 'app-admin-reclamations',
  templateUrl: './admin-reclamations.component.html',
  styleUrls: ['./admin-reclamations.component.scss']
})
export class AdminReclamationsComponent implements OnInit, OnDestroy {

  // ── Data ─────────────────────────────────────────────────────────────────────
  reclamations: Reclamation[] = [];
  stats: ReclamationStats | null = null;
  loading = false;
  error: string | null = null;
  notification: { msg: string; type: 'success' | 'error' } | null = null;
  private refreshTimer: any;
  currentUser: any;

  // ── View ─────────────────────────────────────────────────────────────────────
  viewMode: ViewMode = 'table';
  selectedReclamation: Reclamation | null = null;
  reclamationResponses: ReclamationResponse[] = [];
  loadingResponses = false;

  // ── Filters ──────────────────────────────────────────────────────────────────
  searchTerm = '';
  filterStatus = 'all';
  filterType = 'all';
  filterPriority = 'all';
  dateFrom = '';
  dateTo = '';
  showFilters = false;

  // ── Sort / Pagination ────────────────────────────────────────────────────────
  sortField: SortField = 'date';
  sortDirection: SortDirection = 'desc';
  currentPage = 1;
  itemsPerPage = 10;

  // ── Response form ────────────────────────────────────────────────────────────
  adminResponse = '';
  isInternalNote = false;
  submittingResponse = false;
  adminId = 99;

  aiSuggestions: string[] = [];
  loadingSuggestions = false;
  suggestionError = false;

  slaAlerts: SlaAlert[] = [];
  showSlaPanel = false;

  // ── Traduction ───────────────────────────────────────────────────────────────
  translatedSubject = '';
  translatedDescription = '';
  detectedLanguage = '';
  isTranslating = false;
  showTranslation = false;
  isTranslated = false;
  selectedTargetLang = 'français';

  // ── Modération ───────────────────────────────────────────────────────────────
  filterModeration = 'all';
  showModerationPanel = false;

  sortByGravity = true;

  // Drapeaux par code langue
  readonly languageFlags: { [key: string]: string } = {
    'AR': '🇸🇦', 'EN': '🇬🇧', 'DE': '🇩🇪',
    'ES': '🇪🇸', 'IT': '🇮🇹', 'PT': '🇵🇹',
    'FR': '🇫🇷', 'NL': '🇳🇱', 'PL': '🇵🇱'
  };

  readonly languageNames: { [key: string]: string } = {
    'AR': 'Arabe', 'EN': 'Anglais', 'DE': 'Allemand',
    'ES': 'Espagnol', 'IT': 'Italien', 'PT': 'Portugais',
    'FR': 'Français', 'NL': 'Néerlandais', 'PL': 'Polonais'
  };

  readonly targetLanguages = [
    { value: 'français', label: 'Français', flag: '🇫🇷' },
    { value: 'anglais', label: 'Anglais', flag: '🇬🇧' },
    { value: 'arabe', label: 'Arabe', flag: '🇸🇦' },
    { value: 'espagnol', label: 'Espagnol', flag: '🇪🇸' },
    { value: 'allemand', label: 'Allemand', flag: '🇩🇪' },
    { value: 'italien', label: 'Italien', flag: '🇮🇹' },
  ];

  // ── Auto-status config ───────────────────────────────────────────────────────
  autoStatusRules = [
    { from: 'PENDING', to: 'IN_PROGRESS', trigger: 'Réponse publique ajoutée' },
    { from: 'IN_PROGRESS', to: 'RESOLVED', trigger: 'Admin marque comme résolue' },
    { from: 'RESOLVED', to: 'CLOSED', trigger: 'Auto: 7 jours sans activité' },
  ];

  // ── Config ───────────────────────────────────────────────────────────────────
  reclamationTypes = [
    { value: 'TECHNICAL', label: 'Technique', icon: '⚙️', color: '#6366f1' },
    { value: 'PAYMENT', label: 'Paiement', icon: '💳', color: '#0ea5e9' },
    { value: 'CONTENT', label: 'Contenu', icon: '📚', color: '#f59e0b' },
    { value: 'ACCESS', label: 'Accès', icon: '🔒', color: '#8b5cf6' },
    { value: 'CERTIFICATE', label: 'Certificat', icon: '🏆', color: '#10b981' },
    { value: 'OTHER', label: 'Autre', icon: '📌', color: '#64748b' }
  ];

  statusOptions = [
    { value: 'PENDING', label: 'Pending', css: 'pending', icon: '⏳', next: 'IN_PROGRESS' },
    { value: 'IN_PROGRESS', label: 'In Progress', css: 'progress', icon: '🔄', next: 'RESOLVED' },
    { value: 'RESOLVED', label: 'Resolved', css: 'resolved', icon: '✅', next: 'CLOSED' },
    { value: 'CLOSED', label: 'Closed', css: 'closed', icon: '🔒', next: null },
    { value: 'REJECTED', label: 'Rejected', css: 'rejected', icon: '❌', next: null }
  ];

  priorityOptions = [
    { value: '1', label: 'High', css: 'high', icon: '🔴' },
    { value: '2', label: 'Medium', css: 'medium', icon: '🟡' },
    { value: '3', label: 'Low', css: 'low', icon: '🟢' }
  ];

  creatingTicket = false;
  ticketInfo: {
    exists: boolean;
    ticketId?: string;
    ticketUrl?: string;
    toolName?: string;
  } | null = null;
  selectedTool = 'jira';
  tools = [
    { value: 'demo', label: 'Démo', icon: '🎮', color: '#6366f1' },
    { value: 'jira', label: 'Jira', icon: '📊', color: '#0052CC' },
    { value: 'linear', label: 'Linear', icon: '⚡', color: '#5E6AD2' },
    { value: 'clickup', label: 'ClickUp', icon: '✅', color: '#7B68EE' }
  ];

  constructor(
    private reclamationService: ReclamationService,
    private adminTypingService: AdminTypingService,
    private ngZone: NgZone,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.loadReclamations();
    this.loadStatistics();
    this.loadSlaAlerts();
    this.currentUser = this.userService.getUser();

    this.refreshTimer = setInterval(() => {
      this.ngZone.run(() => {
        this.reclamationService.getAllReclamations().subscribe({
          next: (data) => {
            this.reclamations = data;
            this.autoCloseOldResolved();
            this.loadStatistics();
          },
          error: () => {}
        });
      });
    }, 60000);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
  }

  // ═══════════════════════════════════════════════════════════════════════════════
  // PROPRIÉTÉS GET
  // ═══════════════════════════════════════════════════════════════════════════════

  get activeFiltersCount(): number {
    return [
      this.searchTerm,
      this.filterStatus !== 'all' ? '1' : '',
      this.filterType !== 'all' ? '1' : '',
      this.filterPriority !== 'all' ? '1' : '',
      this.dateFrom, this.dateTo
    ].filter(Boolean).length;
  }

  get suspectCount(): number {
    return this.reclamations.filter(r => r.isSuspect === true).length;
  }

  get filteredReclamations(): Reclamation[] {
  let r = [...this.reclamations];
  
  // ─────────────────────────────────────────────────────────────────
  // FILTRES
  // ─────────────────────────────────────────────────────────────────
  if (this.searchTerm) {
    const q = this.searchTerm.toLowerCase();
    r = r.filter(x =>
      x.subject?.toLowerCase().includes(q) ||
      x.description?.toLowerCase().includes(q) ||
      x.id?.toString().includes(q) ||
      x.learnerId?.toString().includes(q)
    );
  }
  if (this.filterStatus !== 'all') {
    r = r.filter(x => x.status === this.filterStatus);
  }
  if (this.filterType !== 'all') {
    r = r.filter(x => x.type === this.filterType);
  }
  if (this.filterPriority !== 'all') {
    r = r.filter(x => String(x.priority) === this.filterPriority);
  }
  if (this.dateFrom) {
    r = r.filter(x => new Date(x.createdDate || '') >= new Date(this.dateFrom));
  }
  if (this.dateTo) {
    r = r.filter(x => new Date(x.createdDate || '') <= new Date(this.dateTo + 'T23:59:59'));
  }
  if (this.filterModeration === 'suspect') {
    r = r.filter(x => x.isSuspect === true);
  } else if (this.filterModeration === 'clean') {
    r = r.filter(x => !x.isSuspect);
  }
  
  // ─────────────────────────────────────────────────────────────────
  // TRI PAR GRAVITÉ (optionnel, activé par bouton)
  // ─────────────────────────────────────────────────────────────────
  const gravityOrder: { [key: string]: number } = {
    'CRITICAL': 1,
    'HIGH': 2,
    'MEDIUM': 3,
    'LOW': 4
  };
  
  if (this.sortByGravity) {
    // Tri par gravité d'abord, puis par date pour les mêmes gravités
    r.sort((a, b) => {
      const orderA = gravityOrder[a.gravityLevel || 'MEDIUM'];
      const orderB = gravityOrder[b.gravityLevel || 'MEDIUM'];
      
      if (orderA !== orderB) {
        return orderA - orderB;  // CRITICAL en premier
      }
      
      // Même gravité → tri par date décroissante (plus récent en premier)
      const dateA = new Date(a.createdDate || '').getTime();
      const dateB = new Date(b.createdDate || '').getTime();
      return dateB - dateA;
    });
  } else {
    // Tri normal par date (du plus récent au plus ancien)
    r.sort((a, b) => {
      const dateA = new Date(a.createdDate || '').getTime();
      const dateB = new Date(b.createdDate || '').getTime();
      return dateB - dateA;
    });
  }
  
  return r;
}
  getGravityConfig(level: string | undefined): { label: string; color: string; bg: string; icon: string } {
    const configs: { [key: string]: { label: string; color: string; bg: string; icon: string } } = {
        'CRITICAL': { label: 'URGENT', color: '#dc2626', bg: '#fef2f2', icon: '🔴' },
        'HIGH': { label: 'High priority', color: '#f59e0b', bg: '#fffbeb', icon: '🟠' },
        'MEDIUM': { label: 'Normal priority', color: '#3b82f6', bg: '#eff6ff', icon: '🟡' },
        'LOW': { label: 'Low priority', color: '#10b981', bg: '#ecfdf5', icon: '🟢' }
    };
    return configs[level || 'MEDIUM'] || configs['MEDIUM'];
}

  get paginatedReclamations(): Reclamation[] {
    const s = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredReclamations.slice(s, s + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredReclamations.length / this.itemsPerPage);
  }

  get pages(): number[] {
    const total = this.totalPages;
    const cur = this.currentPage;
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
    const pages: number[] = [1];
    if (cur > 3) pages.push(-1);
    for (let i = Math.max(2, cur - 1); i <= Math.min(total - 1, cur + 1); i++) pages.push(i);
    if (cur < total - 2) pages.push(-1);
    pages.push(total);
    return pages;
  }

  get paginationEnd(): number {
    return Math.min(this.currentPage * this.itemsPerPage, this.filteredReclamations.length);
  }

  // ═══════════════════════════════════════════════════════════════════════════════
  // MÉTHODES PUBLIQUES
  // ═══════════════════════════════════════════════════════════════════════════════

  loadReclamations(): void {
  console.log('🟢 loadReclamations appelée');
  this.loading = true;
  this.error = null;
  this.reclamationService.getAllReclamations().subscribe({
    next: (data) => {
      console.log('🟢 Réclamations reçues:', data.length);
      this.reclamations = data;
      this.loading = false;
      this.autoCloseOldResolved();
    },
    error: (e) => {
      console.error('🔴 Erreur:', e);
      this.error = e.status === 0
        ? 'Impossible de joindre le serveur. Vérifiez que le backend est démarré.'
        : `Erreur ${e.status} — ${e.error || e.message}`;
      this.loading = false;
    }
  });
}

  loadStatistics(): void {
    this.reclamationService.getStatistics().subscribe({
      next: (s) => this.stats = s,
      error: (e) => console.error('Stats:', e)
    });
  }

  loadSlaAlerts(): void {
    this.reclamationService.getSlaAlerts().subscribe({
      next: (alerts) => this.slaAlerts = alerts,
      error: () => {}
    });
  }

  resolveSlaAlert(reclamationId: number): void {
    this.reclamationService.resolveSlaAlert(reclamationId).subscribe({
      next: () => {
        this.slaAlerts = this.slaAlerts.filter(a => a.reclamationId !== reclamationId);
        this.notify('Alerte SLA résolue', 'success');
      }
    });
  }

  openDetailById(reclamationId: number): void {
    const rec = this.reclamations.find(r => r.id === reclamationId);
    if (rec) {
      this.openDetail(rec);
      this.showSlaPanel = false;
    }
  }

  loadTicketInfo(): void {
    if (!this.selectedReclamation?.id) return;
    this.reclamationService.getTicketInfo(this.selectedReclamation.id).subscribe({
      next: (info) => { this.ticketInfo = info; },
      error: () => {}
    });
  }

  createExternalTicket(): void {
    if (!this.selectedReclamation?.id) return;
    
    if (this.ticketInfo?.exists && this.ticketInfo.ticketUrl) {
      window.open(this.ticketInfo.ticketUrl, '_blank');
      return;
    }
    
    this.creatingTicket = true;
    this.reclamationService.createExternalTicket(this.selectedReclamation.id, this.selectedTool)
      .subscribe({
        next: (response) => {
          this.ticketInfo = {
            exists: true,
            ticketId: response.ticketId,
            ticketUrl: response.ticketUrl,
            toolName: response.toolName
          };
          this.creatingTicket = false;
          if (this.selectedReclamation) {
            this.selectedReclamation.externalTicketId = response.ticketId;
            this.selectedReclamation.externalTicketUrl = response.ticketUrl;
            this.selectedReclamation.externalTool = response.toolName;
          }
          this.notify(`Ticket ${response.ticketId} créé dans ${response.toolName}`, 'success');
          window.open(response.ticketUrl, '_blank');
        },
        error: (err) => {
          this.creatingTicket = false;
          this.notify(err.error?.error || 'Erreur lors de la création du ticket', 'error');
        }
      });
  }

  private autoCloseOldResolved(): void {
    const twelveHoursAgo = Date.now() - 12 * 60 * 60 * 1000;
    this.reclamations
      .filter(r => r.status === 'RESOLVED')
      .filter(r => {
        const dateToCheck = r.resolvedDate || r.updatedDate;
        if (!dateToCheck) return false;
        return new Date(dateToCheck).getTime() < twelveHoursAgo;
      })
      .forEach(r => {
        this.reclamationService.updateReclamationStatus(r.id!, 'CLOSED', undefined, this.adminId)
          .subscribe({
            next: (updated) => {
              const idx = this.reclamations.findIndex(x => x.id === r.id);
              if (idx !== -1) this.reclamations[idx] = updated;
              this.loadStatistics();
            },
            error: () => {}
          });
      });
  }

  getNextStatus(currentStatus: string | undefined): string | null {
    const opt = this.statusOptions.find(s => s.value === currentStatus);
    return opt?.next || null;
  }

  advanceStatus(rec: Reclamation, e?: Event): void {
    e?.stopPropagation();
    const next = this.getNextStatus(rec.status);
    if (next) this.updateStatus(rec, next);
  }

  sort(field: SortField): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 1;
  }

  sortIcon(field: SortField): string {
    if (this.sortField !== field) return 'both';
    return this.sortDirection;
  }

  goToPage(p: number): void {
    if (p >= 1 && p <= this.totalPages) {
      this.currentPage = p;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  openDetail(rec: Reclamation): void {
    this.selectedReclamation = { ...rec };
    this.viewMode = 'detail';
    this.adminResponse = '';
    this.isInternalNote = false;
    this.reclamationResponses = [];
    this.aiSuggestions = [];
    this.translatedSubject = '';
    this.translatedDescription = '';
    this.detectedLanguage = '';
    this.showTranslation = false;
    this.isTranslated = false;
    this.ticketInfo = null;
    this.loadResponses(rec.id!);
    this.loadAiSuggestions(rec.id!);
    this.translateReclamation(rec);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  backToTable(): void {
    this.viewMode = 'table';
    this.selectedReclamation = null;
    this.loadReclamations();
    this.loadStatistics();
  }

  loadResponses(id: number): void {
    this.loadingResponses = true;
    this.reclamationService.getReclamationResponses(id).subscribe({
      next: (res) => {
        this.ngZone.run(() => {
          this.reclamationResponses = res.sort((a, b) =>
            new Date(a.createdDate || '').getTime() - new Date(b.createdDate || '').getTime()
          );
          this.loadingResponses = false;
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.reclamationResponses = [];
          this.loadingResponses = false;
        });
      }
    });
  }

  updateStatus(rec: Reclamation, status: string, e?: Event): void {
    e?.stopPropagation();
    this.reclamationService.updateReclamationStatus(rec.id!, status, undefined, this.adminId).subscribe({
      next: (updated) => {
        const idx = this.reclamations.findIndex(r => r.id === rec.id);
        if (idx !== -1) this.reclamations[idx] = updated;
        if (this.selectedReclamation?.id === rec.id) this.selectedReclamation = { ...updated };
        this.loadStatistics();
        const statusLabel = this.getStatusConfig(status).label;
        this.notify(`Statut mis à jour → ${statusLabel}`, 'success');
      },
      error: () => this.notify('Erreur lors de la mise à jour du statut', 'error')
    });
  }

  onAdminResponseInput(): void {
    if (this.selectedReclamation?.id) {
      this.adminTypingService.sendTyping(this.selectedReclamation.id, true);
    }
  }

  submitResponse(): void {
    if (this.selectedReclamation?.id) {
      this.adminTypingService.sendTyping(this.selectedReclamation.id, false);
    }
    if (!this.selectedReclamation || !this.adminResponse.trim()) return;
    this.submittingResponse = true;

    this.reclamationService.createResponse(
      this.selectedReclamation.id!, this.selectedReclamation.learnerId,
      this.adminResponse.trim(), this.isInternalNote
    ).subscribe({
      next: () => {
        this.adminResponse = '';
        this.loadResponses(this.selectedReclamation!.id!);
        this.loadStatistics();
        this.submittingResponse = false;

        if (this.selectedReclamation!.status === 'PENDING') {
          this.reclamationService
            .updateReclamationStatus(this.selectedReclamation!.id!, 'IN_PROGRESS', undefined, this.adminId)
            .subscribe({
              next: (updated) => {
                const idx = this.reclamations.findIndex(r => r.id === updated.id);
                if (idx !== -1) this.reclamations[idx] = updated;
                this.selectedReclamation = { ...updated };
                this.loadStatistics();
              },
              error: () => {}
            });
          this.notify('Answer added — status → Automatically being processed', 'success');
        } else {
          this.notify(this.isInternalNote ? 'Note interne ajoutée' : 'Réponse envoyée à l\'apprenant', 'success');
        }
      },
      error: (e) => {
        this.submittingResponse = false;
        this.notify(`Erreur : ${e.error || e.message}`, 'error');
      }
    });
  }

  markResolved(): void {
    if (!this.selectedReclamation) return;
    this.updateStatus(this.selectedReclamation, 'RESOLVED');
  }

  markRejected(): void {
    if (!this.selectedReclamation) return;
    this.updateStatus(this.selectedReclamation, 'REJECTED');
  }

  deleteReclamation(id: number, e?: Event): void {
    e?.stopPropagation();
    if (!confirm('Supprimer définitivement cette réclamation ?')) return;
    this.reclamationService.deleteReclamation(id).subscribe({
      next: () => {
        this.reclamations = this.reclamations.filter(r => r.id !== id);
        if (this.selectedReclamation?.id === id) this.backToTable();
        this.loadStatistics();
        this.notify('Réclamation supprimée', 'success');
      },
      error: () => this.notify('Erreur lors de la suppression', 'error')
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.filterStatus = 'all';
    this.filterType = 'all';
    this.filterPriority = 'all';
    this.dateFrom = '';
    this.dateTo = '';
    this.filterModeration = 'all';
    this.currentPage = 1;
  }

  notify(msg: string, type: 'success' | 'error'): void {
    this.notification = { msg, type };
    setTimeout(() => this.notification = null, 4000);
  }

  // ═══════════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════════

  getStatusConfig(status: string | undefined) {
    return this.statusOptions.find(s => s.value === status) || { label: status || '-', css: 'pending', icon: '❓', next: null };
  }

  getTypeConfig(type: string | undefined) {
    return this.reclamationTypes.find(t => t.value === type) || { label: type || '-', icon: '📌', color: '#64748b' };
  }

  getPriorityConfig(p: number | undefined) {
    return this.priorityOptions.find(x => x.value === String(p)) || { label: 'Basse', css: 'low', icon: '🟢' };
  }

  getStatusProgress(status: string | undefined): number {
    const map: { [k: string]: number } = {
      'PENDING': 10, 'IN_PROGRESS': 50, 'RESOLVED': 90, 'CLOSED': 100, 'REJECTED': 100
    };
    return map[status || 'PENDING'] || 0;
  }

  hasSpecificInfo(rec: Reclamation): boolean {
    return !!(rec.browserName || rec.osVersion || rec.errorCode ||
              rec.transactionId || rec.invoiceNumber || rec.amount ||
              rec.courseId || rec.contentType || rec.pageUrl ||
              rec.accessDate || rec.deviceType ||
              rec.completionDate || rec.certificateType);
  }

  formatDate(d: string | undefined): string {
    if (!d) return '-';
    return new Date(d).toLocaleDateString('fr-FR', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatRelative(d: string | undefined): string {
    if (!d) return '-';
    const diff = Date.now() - new Date(d).getTime();
    const mins = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    if (mins < 1) return 'À l\'instant';
    if (mins < 60) return `Il y a ${mins} min`;
    if (hours < 24) return `Il y a ${hours}h`;
    if (days === 1) return 'Hier';
    if (days < 7) return `Il y a ${days} j`;
    return this.formatDate(d);
  }

  isLearnerResponse(resp: ReclamationResponse): boolean {
    return resp.senderType === 'LEARNER';

  }

  loadAiSuggestions(id: number): void {
    this.loadingSuggestions = true;
    this.suggestionError = false;
    this.reclamationService.getAiSuggestions(id).subscribe({
      next: (res) => {
        this.aiSuggestions = res.suggestions;
        this.loadingSuggestions = false;
      },
      error: () => {
        this.suggestionError = true;
        this.loadingSuggestions = false;
      }
    });
  }

  applySuggestion(text: string): void {
    this.adminResponse = text;
    this.isInternalNote = false;
  }

  // ═══════════════════════════════════════════════════════════════════════════════
  // MODÉRATION
  // ═══════════════════════════════════════════════════════════════════════════════

  approveReclamation(): void {
    if (!this.selectedReclamation?.id) return;
    this.reclamationService.approveReclamation(this.selectedReclamation.id).subscribe({
      next: (updated) => {
        const index = this.reclamations.findIndex(r => r.id === updated.id);
        if (index !== -1) this.reclamations[index] = updated;
        if (this.selectedReclamation) this.selectedReclamation = updated;
        this.notify('✅ Réclamation approuvée et publiée', 'success');
        this.loadStatistics();
      },
      error: () => this.notify('Erreur lors de l\'approbation', 'error')
    });
  }

  rejectReclamation(): void {
    if (!this.selectedReclamation?.id) return;
    if (confirm('Rejeter cette réclamation ? L\'apprenant sera notifié.')) {
      this.reclamationService.rejectReclamation(this.selectedReclamation.id).subscribe({
        next: (updated) => {
          const index = this.reclamations.findIndex(r => r.id === updated.id);
          if (index !== -1) this.reclamations[index] = updated;
          if (this.selectedReclamation) this.selectedReclamation = updated;
          this.notify('❌ Réclamation rejetée', 'success');
          this.loadStatistics();
        },
        error: () => this.notify('Erreur lors du rejet', 'error')
      });
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════════
  // TRADUCTION
  // ═══════════════════════════════════════════════════════════════════════════════

  translateReclamation(rec: Reclamation): void {
    const fullText = `${rec.subject} ${rec.description}`;
    this.reclamationService.detectLanguage(fullText).subscribe({
      next: (res) => {
        this.detectedLanguage = res.detectedLanguage;
        this.isTranslated = false;
        this.showTranslation = false;
        this.translatedSubject = '';
        this.translatedDescription = '';
      },
      error: () => {}
    });
  }

  doTranslate(): void {
    if (!this.selectedReclamation) return;
    if (this.isTranslated) {
      this.showTranslation = !this.showTranslation;
      return;
    }
    this.isTranslating = true;
    const rec = this.selectedReclamation;
    this.reclamationService.translateText(rec.subject || '', this.selectedTargetLang).subscribe({
      next: (r1) => {
        this.translatedSubject = r1.translatedText;
        this.reclamationService.translateText(rec.description || '', this.selectedTargetLang).subscribe({
          next: (r2) => {
            this.translatedDescription = r2.translatedText;
            this.isTranslating = false;
            this.isTranslated = true;
            this.showTranslation = true;
          },
          error: () => { this.isTranslating = false; }
        });
      },
      error: () => { this.isTranslating = false; }
    });
  }

  onTargetLangChange(): void {
    this.isTranslated = false;
    this.showTranslation = false;
    this.translatedSubject = '';
    this.translatedDescription = '';
  }

  toggleTranslation(): void {
    this.showTranslation = !this.showTranslation;
  }

  getLanguageFlag(code: string): string {
    return this.languageFlags[code] || '🌐';
  }

  getLanguageName(code: string): string {
    return this.languageNames[code] || code;
  }

  // ═══════════════════════════════════════════════════════════════════════════════
  // TICKETS
  // ═══════════════════════════════════════════════════════════════════════════════

  getToolColor(toolName: string): string {
    const colors: Record<string, string> = {
      'Jira': '#0052CC',
      'Linear': '#5E6AD2',
      'ClickUp': '#7B68EE'
    };
    return colors[toolName] || '#6366f1';
  }

  getToolIcon(toolName: string): string {
    const icons: Record<string, string> = {
      'Jira': '📊',
      'Linear': '⚡',
      'ClickUp': '✅'
    };
    return icons[toolName] || '📋';
  }

  getSelectedToolLabel(): string {
    const tool = this.tools.find(t => t.value === this.selectedTool);
    return tool ? tool.label : '';
  }

  getToolHint(): string {
    const hints: Record<string, string> = {
      'demo': 'Mode démo - simule la création d\'un ticket (aucune API requise)',
      'jira': 'Crée un ticket dans Jira pour le suivi technique',
      'linear': 'Crée une issue Linear pour l\'équipe produit',
      'clickup': 'Crée une tâche ClickUp pour l\'équipe projet'
    };
    return hints[this.selectedTool] || 'Crée un ticket de suivi';
  }

  editAndApprove(): void {
  // Ouvrir un modal ou un panneau d'édition
  const newSubject = prompt('Modifier le sujet:', this.selectedReclamation?.subject);
  const newDescription = prompt('Modifier la description:', this.selectedReclamation?.description);
  
  if (newSubject && newDescription && this.selectedReclamation) {
    // Créer une version modifiée
    const modifiedReclamation = {
      ...this.selectedReclamation,
      subject: newSubject,
      description: newDescription
    };
    
    // Sauvegarder puis approuver
    this.reclamationService.updateReclamation(this.selectedReclamation.id!, modifiedReclamation)
      .subscribe(() => {
        this.approveReclamation();
      });
  }
}



// Ajoutez cette méthode dans la classe AdminReclamationsComponent
toggleGravitySort(): void {
  this.sortByGravity = !this.sortByGravity;
  this.currentPage = 1;  // Revenir à la première page
  this.notify(this.sortByGravity ? 'Tri par gravité activé' : 'Tri par date uniquement', 'success');
}

highlightWords(text: string, words: string[] | undefined): string {
  if (!text || !words?.length) return text;
  let result = text;
  words.forEach(w => {
    const regex = new RegExp(`(${w})`, 'gi');
    result = result.replace(regex, '<span class="mod-highlight">$1</span>');
  });
  return result;
}
}