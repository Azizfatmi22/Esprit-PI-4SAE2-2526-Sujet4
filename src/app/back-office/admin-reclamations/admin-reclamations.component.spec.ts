import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NgZone } from '@angular/core';
import { of } from 'rxjs';
import { FormsModule } from '@angular/forms';

import { AdminReclamationsComponent } from './admin-reclamations.component';
import { ReclamationService, Reclamation, ReclamationResponse, ReclamationStats, SlaAlert }
  from '../../front-office/services/reclamation.service';
import { AdminTypingService } from '../../front-office/services/admin-typing.service';
import { UserService } from '../../front-office/services/user.service';

// ── MOCKS ─────────────────────────────────────────────────────────────────────

const mockUser = {
  id: 'admin-99',
  username: 'admin',
  email: 'admin@test.com',
  fullName: 'Admin Test',
  roles: ['ADMIN']
};

const mockReclamation: Reclamation = {
  id: 1,
  learnerId: 'user-123',
  type: 'TECHNICAL',
  subject: 'Problème de connexion',
  description: 'Je ne peux pas me connecter',
  status: 'PENDING',
  priority: 2,
  createdDate: new Date().toISOString(),
  gravityLevel: 'HIGH'
};

const mockReclamationResolved: Reclamation = {
  ...mockReclamation,
  id: 2,
  status: 'RESOLVED',
  resolvedDate: new Date(Date.now() - 13 * 60 * 60 * 1000).toISOString()
};

const mockResponse: ReclamationResponse = {
  id: 1,
  reclamationId: 1,
  learnerId: 'user-123',
  responseText: 'Nous avons pris en charge votre demande',
  isInternal: false,
  senderType: 'ADMIN',
  createdDate: new Date().toISOString()
};

const mockStats: ReclamationStats = {
  total: 5,
  pending: 2,
  inProgress: 1,
  resolved: 1,
  closed: 1,
  rejected: 0,
  unresolved: 3
};

const mockSlaAlert: SlaAlert = {
  id: 1,
  reclamationId: 1,
  priority: 1,
  slaMinutes: 240,
  elapsedMinutes: 300,
  alertDate: new Date().toISOString(),
  emailSent: true,
  resolved: false
};

const mockReclamationService = {
  getAllReclamations: jasmine.createSpy('getAllReclamations').and.returnValue(of([mockReclamation])),
  getStatistics: jasmine.createSpy('getStatistics').and.returnValue(of(mockStats)),
  getSlaAlerts: jasmine.createSpy('getSlaAlerts').and.returnValue(of([mockSlaAlert])),
  resolveSlaAlert: jasmine.createSpy('resolveSlaAlert').and.returnValue(of(void 0)),
  getReclamationResponses: jasmine.createSpy('getReclamationResponses').and.returnValue(of([mockResponse])),
  createResponse: jasmine.createSpy('createResponse').and.returnValue(of(mockResponse)),
  updateReclamationStatus: jasmine.createSpy('updateReclamationStatus').and.returnValue(of({ ...mockReclamation, status: 'IN_PROGRESS' })),
  deleteReclamation: jasmine.createSpy('deleteReclamation').and.returnValue(of({})),
  approveReclamation: jasmine.createSpy('approveReclamation').and.returnValue(of({ ...mockReclamation, isSuspect: false })),
  rejectReclamation: jasmine.createSpy('rejectReclamation').and.returnValue(of({ ...mockReclamation, status: 'REJECTED' })),
  getAiSuggestions: jasmine.createSpy('getAiSuggestions').and.returnValue(of({ suggestions: [] })),
  detectLanguage: jasmine.createSpy('detectLanguage').and.returnValue(of({ detectedLanguage: 'FR', isFrench: true })),
  translateText: jasmine.createSpy('translateText').and.returnValue(of({
    translatedText: 'Translated text',
    detectedLanguage: 'FR',
    languageName: 'Français',
    languageFlag: '🇫🇷',
    originalText: 'Original'
  })),
  createExternalTicket: jasmine.createSpy('createExternalTicket').and.returnValue(of({
    ticketId: 'JIRA-1',
    ticketUrl: 'https://jira.example.com/JIRA-1',
    toolName: 'Jira',
    alreadyExists: false
  })),
  getTicketInfo: jasmine.createSpy('getTicketInfo').and.returnValue(of({ exists: false })),
  updateReclamation: jasmine.createSpy('updateReclamation').and.returnValue(of(mockReclamation)),
};

const mockAdminTypingService = {
  sendTyping: jasmine.createSpy('sendTyping')
};

const mockUserService = {
  getUser: jasmine.createSpy('getUser').and.returnValue(mockUser)
};

// ── TESTS ─────────────────────────────────────────────────────────────────────

describe('AdminReclamationsComponent', () => {
  let component: AdminReclamationsComponent;
  let fixture: ComponentFixture<AdminReclamationsComponent>;

  beforeEach(async () => {
    // Reset toutes les spies
    mockReclamationService.getAllReclamations.and.returnValue(of([mockReclamation]));
    mockReclamationService.getStatistics.and.returnValue(of(mockStats));
    mockReclamationService.getSlaAlerts.and.returnValue(of([mockSlaAlert]));
    mockReclamationService.getReclamationResponses.and.returnValue(of([mockResponse]));
    mockReclamationService.getAiSuggestions.and.returnValue(of({ suggestions: [] }));
    mockReclamationService.detectLanguage.and.returnValue(of({ detectedLanguage: 'FR', isFrench: true }));
    mockReclamationService.updateReclamationStatus.and.returnValue(of({ ...mockReclamation, status: 'IN_PROGRESS' }));
    mockReclamationService.deleteReclamation.and.returnValue(of({}));
    mockReclamationService.approveReclamation.and.returnValue(of({ ...mockReclamation, isSuspect: false }));
    mockReclamationService.rejectReclamation.and.returnValue(of({ ...mockReclamation, status: 'REJECTED' }));
    mockAdminTypingService.sendTyping.calls.reset();

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [AdminReclamationsComponent],
      providers: [
        { provide: ReclamationService, useValue: mockReclamationService },
        { provide: AdminTypingService, useValue: mockAdminTypingService },
        { provide: UserService, useValue: mockUserService },
        // ← NE PAS mettre NgZone ici, il est fourni automatiquement par Angular Test
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminReclamationsComponent);
    component = fixture.componentInstance;

    // ← Bloquer le setInterval du ngOnInit pour éviter les fuites
    spyOn(window, 'setInterval').and.returnValue(1 as any);
  });

  afterEach(() => {
    // ← Guard pour éviter l'erreur si component est undefined
    if (component) {
      try {
        component.ngOnDestroy();
      } catch (e) {}
    }
  });

  // ══════════════════════════════════════════════════════════════════
  // 1. INITIALISATION
  // ══════════════════════════════════════════════════════════════════

  describe('Initialisation', () => {

    it('devrait créer le composant', () => {
      expect(component).toBeTruthy();
    });

    it('devrait charger les réclamations au ngOnInit', () => {
      component.ngOnInit();
      expect(mockReclamationService.getAllReclamations).toHaveBeenCalled();
      expect(component.reclamations.length).toBe(1);
    });

    it('devrait charger les statistiques au ngOnInit', () => {
      component.ngOnInit();
      expect(mockReclamationService.getStatistics).toHaveBeenCalled();
      expect(component.stats).toEqual(mockStats);
    });

    it('devrait charger les alertes SLA au ngOnInit', () => {
      component.ngOnInit();
      expect(mockReclamationService.getSlaAlerts).toHaveBeenCalled();
      expect(component.slaAlerts.length).toBe(1);
    });

    it('devrait récupérer l\'utilisateur courant au ngOnInit', () => {
      component.ngOnInit();
      expect(component.currentUser).toEqual(mockUser);
    });

    it('devrait avoir viewMode = table par défaut', () => {
      expect(component.viewMode).toBe('table');
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 2. FILTRES ET RECHERCHE
  // ══════════════════════════════════════════════════════════════════

  describe('Filtres et Recherche', () => {

    beforeEach(() => {
      component.reclamations = [
        mockReclamation,
        { ...mockReclamation, id: 2, status: 'RESOLVED', type: 'PAYMENT', priority: 1 },
        { ...mockReclamation, id: 3, status: 'IN_PROGRESS', type: 'CONTENT', priority: 3, isSuspect: true }
      ];
    });

    it('devrait filtrer par statut RESOLVED', () => {
      component.filterStatus = 'RESOLVED';
      expect(component.filteredReclamations.length).toBe(1);
    });

    it('devrait filtrer par type PAYMENT', () => {
      component.filterType = 'PAYMENT';
      expect(component.filteredReclamations.length).toBe(1);
    });

    it('devrait filtrer par priorité 1', () => {
      component.filterPriority = '1';
      expect(component.filteredReclamations.length).toBe(1);
    });

    it('devrait filtrer les réclamations suspectes', () => {
      component.filterModeration = 'suspect';
      expect(component.filteredReclamations.length).toBe(1);
      expect(component.filteredReclamations[0].isSuspect).toBeTrue();
    });

    it('devrait filtrer les réclamations non suspectes', () => {
      component.filterModeration = 'clean';
      expect(component.filteredReclamations.length).toBe(2);
    });

    it('devrait réinitialiser tous les filtres', () => {
      component.searchTerm = 'test';
      component.filterStatus = 'PENDING';
      component.filterType = 'TECHNICAL';
      component.filterPriority = '1';
      component.filterModeration = 'suspect';
      component.resetFilters();
      expect(component.searchTerm).toBe('');
      expect(component.filterStatus).toBe('all');
      expect(component.filterType).toBe('all');
      expect(component.filterPriority).toBe('all');
      expect(component.filterModeration).toBe('all');
      expect(component.currentPage).toBe(1);
    });

    it('devrait compter les filtres actifs', () => {
      component.searchTerm = 'test';
      component.filterStatus = 'PENDING';
      expect(component.activeFiltersCount).toBe(2);
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 3. TRI PAR GRAVITÉ
  // ══════════════════════════════════════════════════════════════════

  describe('Tri par Gravité', () => {

    beforeEach(() => {
      component.reclamations = [
        { ...mockReclamation, id: 1, gravityLevel: 'LOW' },
        { ...mockReclamation, id: 2, gravityLevel: 'CRITICAL' },
        { ...mockReclamation, id: 3, gravityLevel: 'MEDIUM' },
        { ...mockReclamation, id: 4, gravityLevel: 'HIGH' }
      ];
    });

    it('devrait trier par gravité (CRITICAL en premier)', () => {
      component.sortByGravity = true;
      const filtered = component.filteredReclamations;
      expect(filtered[0].gravityLevel).toBe('CRITICAL');
      expect(filtered[1].gravityLevel).toBe('HIGH');
      expect(filtered[2].gravityLevel).toBe('MEDIUM');
      expect(filtered[3].gravityLevel).toBe('LOW');
    });

    it('devrait toggle le tri par gravité', () => {
      component.sortByGravity = true;
      component.toggleGravitySort();
      expect(component.sortByGravity).toBeFalse();
      expect(component.currentPage).toBe(1);
    });

    it('devrait retourner la config CRITICAL correcte', () => {
      const config = component.getGravityConfig('CRITICAL');
      expect(config.label).toBe('URGENT');
      expect(config.icon).toBe('🔴');
    });

    it('devrait retourner la config LOW correcte', () => {
      const config = component.getGravityConfig('LOW');
      expect(config.label).toBe('Low priority');
    });

    it('devrait retourner MEDIUM par défaut', () => {
      const config = component.getGravityConfig(undefined);
      expect(config.label).toBe('Normal priority');
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 4. PAGINATION
  // ══════════════════════════════════════════════════════════════════

  describe('Pagination', () => {

    beforeEach(() => {
      component.reclamations = Array.from({ length: 15 }, (_, i) => ({
        ...mockReclamation, id: i + 1
      }));
    });

    it('devrait calculer le nombre total de pages', () => {
      expect(component.totalPages).toBe(2);
    });

    it('devrait retourner 10 réclamations sur la page 1', () => {
      component.currentPage = 1;
      expect(component.paginatedReclamations.length).toBe(10);
    });

    it('devrait retourner 5 réclamations sur la page 2', () => {
      component.currentPage = 2;
      expect(component.paginatedReclamations.length).toBe(5);
    });

    it('devrait naviguer vers une page valide', () => {
      component.goToPage(2);
      expect(component.currentPage).toBe(2);
    });

    it('ne devrait pas naviguer vers une page invalide', () => {
      component.currentPage = 1;
      component.goToPage(0);
      expect(component.currentPage).toBe(1);
    });

    it('devrait calculer paginationEnd correctement', () => {
      component.currentPage = 1;
      expect(component.paginationEnd).toBe(10);
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 5. DÉTAIL D'UNE RÉCLAMATION
  // ══════════════════════════════════════════════════════════════════

  describe('Détail de Réclamation', () => {

    it('devrait ouvrir le détail d\'une réclamation', () => {
      component.openDetail(mockReclamation);
      expect(component.viewMode).toBe('detail');
      expect(component.selectedReclamation?.id).toBe(1);
    });

    it('devrait charger les réponses lors de l\'ouverture du détail', () => {
      component.openDetail(mockReclamation);
      expect(mockReclamationService.getReclamationResponses).toHaveBeenCalledWith(1);
    });

    it('devrait charger les suggestions IA lors de l\'ouverture du détail', () => {
      component.openDetail(mockReclamation);
      expect(mockReclamationService.getAiSuggestions).toHaveBeenCalledWith(1);
    });

    it('devrait retourner à la table', () => {
      component.viewMode = 'detail';
      component.selectedReclamation = { ...mockReclamation };
      component.backToTable();
      expect(component.viewMode).toBe('table');
      expect(component.selectedReclamation).toBeNull();
    });

    it('devrait ouvrir le détail par ID', () => {
      component.reclamations = [mockReclamation];
      component.openDetailById(1);
      expect(component.selectedReclamation?.id).toBe(1);
      expect(component.showSlaPanel).toBeFalse();
    });

    it('ne devrait rien faire si l\'ID n\'existe pas', () => {
      component.reclamations = [mockReclamation];
      component.selectedReclamation = null;
      component.openDetailById(999);
      expect(component.selectedReclamation).toBeNull();
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 6. GESTION DU STATUT
  // ══════════════════════════════════════════════════════════════════

  describe('Gestion du Statut', () => {

    it('devrait retourner le prochain statut pour PENDING', () => {
      expect(component.getNextStatus('PENDING')).toBe('IN_PROGRESS');
    });

    it('devrait retourner le prochain statut pour IN_PROGRESS', () => {
      expect(component.getNextStatus('IN_PROGRESS')).toBe('RESOLVED');
    });

    it('devrait retourner null pour CLOSED', () => {
      expect(component.getNextStatus('CLOSED')).toBeNull();
    });

    it('devrait mettre à jour le statut d\'une réclamation', () => {
      component.reclamations = [mockReclamation];
      component.updateStatus(mockReclamation, 'IN_PROGRESS');
      expect(mockReclamationService.updateReclamationStatus).toHaveBeenCalledWith(
        1, 'IN_PROGRESS', undefined, 99
      );
    });

    it('devrait avancer le statut automatiquement', () => {
      component.reclamations = [mockReclamation];
      component.advanceStatus(mockReclamation);
      expect(mockReclamationService.updateReclamationStatus).toHaveBeenCalledWith(
        1, 'IN_PROGRESS', undefined, 99
      );
    });

    it('devrait marquer comme résolue', () => {
      component.selectedReclamation = { ...mockReclamation };
      component.reclamations = [mockReclamation];
      component.markResolved();
      expect(mockReclamationService.updateReclamationStatus).toHaveBeenCalledWith(
        1, 'RESOLVED', undefined, 99
      );
    });

    it('devrait marquer comme rejetée', () => {
      component.selectedReclamation = { ...mockReclamation };
      component.reclamations = [mockReclamation];
      component.markRejected();
      expect(mockReclamationService.updateReclamationStatus).toHaveBeenCalledWith(
        1, 'REJECTED', undefined, 99
      );
    });

    it('devrait calculer la progression PENDING', () => {
      expect(component.getStatusProgress('PENDING')).toBe(10);
    });

    it('devrait calculer la progression CLOSED', () => {
      expect(component.getStatusProgress('CLOSED')).toBe(100);
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 7. RÉPONSES ADMIN
  // ══════════════════════════════════════════════════════════════════

  describe('Réponses Admin', () => {

    beforeEach(() => {
      component.selectedReclamation = { ...mockReclamation };
      component.reclamations = [{ ...mockReclamation }];
    });

    it('devrait soumettre une réponse admin', () => {
      component.adminResponse = 'Ma réponse admin';
      component.submitResponse();
      expect(mockReclamationService.createResponse).toHaveBeenCalledWith(
        1, 'user-123', 'Ma réponse admin', false
      );
    });

    it('ne devrait pas soumettre si la réponse est vide', () => {
      mockReclamationService.createResponse.calls.reset();
      component.adminResponse = '';
      component.submitResponse();
      expect(mockReclamationService.createResponse).not.toHaveBeenCalled();
    });

    it('ne devrait pas soumettre si aucune réclamation sélectionnée', () => {
      mockReclamationService.createResponse.calls.reset();
      component.selectedReclamation = null;
      component.adminResponse = 'Réponse';
      component.submitResponse();
      expect(mockReclamationService.createResponse).not.toHaveBeenCalled();
    });

    it('devrait envoyer l\'indicateur de frappe', () => {
      component.onAdminResponseInput();
      expect(mockAdminTypingService.sendTyping).toHaveBeenCalledWith(1, true);
    });

    it('devrait appliquer une suggestion IA', () => {
      component.applySuggestion('Texte suggéré');
      expect(component.adminResponse).toBe('Texte suggéré');
      expect(component.isInternalNote).toBeFalse();
    });

    it('devrait identifier une réponse LEARNER', () => {
      const resp: ReclamationResponse = { ...mockResponse, senderType: 'LEARNER' };
      expect(component.isLearnerResponse(resp)).toBeTrue();
    });

    it('devrait identifier une réponse ADMIN', () => {
      expect(component.isLearnerResponse(mockResponse)).toBeFalse();
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 8. SUPPRESSION
  // ══════════════════════════════════════════════════════════════════

  describe('Suppression', () => {

    it('devrait supprimer une réclamation après confirmation', () => {
      component.reclamations = [{ ...mockReclamation }];
      // ← Spy sur window.confirm AVANT l'appel
      spyOn(window, 'confirm').and.returnValue(true);
      component.deleteReclamation(1);
      expect(mockReclamationService.deleteReclamation).toHaveBeenCalledWith(1);
    });

    it('ne devrait pas supprimer si l\'utilisateur annule', () => {
      mockReclamationService.deleteReclamation.calls.reset();
      spyOn(window, 'confirm').and.returnValue(false);
      component.deleteReclamation(1);
      expect(mockReclamationService.deleteReclamation).not.toHaveBeenCalled();
    });

    it('devrait retourner à la table si réclamation sélectionnée supprimée', () => {
      component.reclamations = [{ ...mockReclamation }];
      component.selectedReclamation = { ...mockReclamation };
      component.viewMode = 'detail';
      spyOn(window, 'confirm').and.returnValue(true);
      component.deleteReclamation(1);
      expect(component.viewMode).toBe('table');
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 9. MODÉRATION
  // ══════════════════════════════════════════════════════════════════

  describe('Modération', () => {

  beforeEach(() => {
    component.selectedReclamation = { ...mockReclamation, isSuspect: true };
    component.reclamations = [{ ...mockReclamation, isSuspect: true }];
    // ← Reset les spies au début de chaque test de modération
    mockReclamationService.approveReclamation.calls.reset();
    mockReclamationService.rejectReclamation.calls.reset();
  });

  it('devrait approuver une réclamation suspecte', () => {
    component.approveReclamation();
    expect(mockReclamationService.approveReclamation).toHaveBeenCalledWith(1);
  });

  it('devrait rejeter une réclamation suspecte', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.rejectReclamation();
    expect(mockReclamationService.rejectReclamation).toHaveBeenCalledWith(1);
  });

  it('ne devrait pas rejeter si l\'utilisateur annule', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    component.rejectReclamation();
    expect(mockReclamationService.rejectReclamation).not.toHaveBeenCalled();
  });

  it('devrait compter les réclamations suspectes', () => {
    component.reclamations = [
      { ...mockReclamation, isSuspect: true },
      { ...mockReclamation, id: 2, isSuspect: true },
      { ...mockReclamation, id: 3, isSuspect: false }
    ];
    expect(component.suspectCount).toBe(2);
  });

  it('ne devrait rien faire si aucune réclamation sélectionnée', () => {
    // ← Reset AVANT de mettre null
    mockReclamationService.approveReclamation.calls.reset();
    component.selectedReclamation = null;
    component.approveReclamation();
    expect(mockReclamationService.approveReclamation).not.toHaveBeenCalled();
  });

});
  // ══════════════════════════════════════════════════════════════════
  // 10. TRADUCTION
  // ══════════════════════════════════════════════════════════════════

  describe('Traduction', () => {

    beforeEach(() => {
      component.selectedReclamation = { ...mockReclamation };
    });

    it('devrait détecter la langue', () => {
      component.translateReclamation(mockReclamation);
      expect(mockReclamationService.detectLanguage).toHaveBeenCalled();
      expect(component.detectedLanguage).toBe('FR');
    });

    it('devrait traduire sujet et description', () => {
      component.doTranslate();
      expect(mockReclamationService.translateText).toHaveBeenCalled();
      expect(component.isTranslated).toBeTrue();
      expect(component.showTranslation).toBeTrue();
    });

    it('devrait toggle la traduction si déjà traduit', () => {
      component.isTranslated = true;
      component.showTranslation = false;
      component.doTranslate();
      expect(component.showTranslation).toBeTrue();
      expect(mockReclamationService.translateText).not.toHaveBeenCalled();
    });

    it('devrait reset la traduction au changement de langue', () => {
      component.isTranslated = true;
      component.translatedSubject = 'Translated';
      component.onTargetLangChange();
      expect(component.isTranslated).toBeFalse();
      expect(component.translatedSubject).toBe('');
    });

    it('devrait retourner le drapeau FR', () => {
      expect(component.getLanguageFlag('FR')).toBe('🇫🇷');
    });

    it('devrait retourner 🌐 pour langue inconnue', () => {
      expect(component.getLanguageFlag('UNKNOWN')).toBe('🌐');
    });

    it('devrait retourner le nom de langue Français', () => {
      expect(component.getLanguageName('FR')).toBe('Français');
    });

    it('devrait toggle l\'affichage de la traduction', () => {
      component.showTranslation = false;
      component.toggleTranslation();
      expect(component.showTranslation).toBeTrue();
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 11. TICKETS EXTERNES
  // ══════════════════════════════════════════════════════════════════

  describe('Tickets Externes', () => {

    beforeEach(() => {
      component.selectedReclamation = { ...mockReclamation };
    });

    it('devrait créer un ticket externe Jira', () => {
      component.ticketInfo = null;
      component.selectedTool = 'jira';
      component.createExternalTicket();
      expect(mockReclamationService.createExternalTicket).toHaveBeenCalledWith(1, 'jira');
    });

    it('devrait ouvrir le ticket existant si déjà créé', () => {
      spyOn(window, 'open');
      component.ticketInfo = { exists: true, ticketUrl: 'https://jira.example.com/JIRA-1' };
      component.createExternalTicket();
      expect(window.open).toHaveBeenCalledWith('https://jira.example.com/JIRA-1', '_blank');
      expect(mockReclamationService.createExternalTicket).not.toHaveBeenCalled();
    });

    it('ne devrait pas créer un ticket si aucune réclamation sélectionnée', () => {
  // ← forcer null AVANT l'appel (le beforeEach met mockReclamation)
  component.selectedReclamation = null;
  mockReclamationService.createExternalTicket.calls.reset();
  
  component.createExternalTicket();
  
  expect(mockReclamationService.createExternalTicket).not.toHaveBeenCalled();
});

    it('devrait charger les infos du ticket', () => {
      component.loadTicketInfo();
      expect(mockReclamationService.getTicketInfo).toHaveBeenCalledWith(1);
    });

    it('devrait retourner la couleur Jira', () => {
      expect(component.getToolColor('Jira')).toBe('#0052CC');
    });

    it('devrait retourner couleur par défaut pour outil inconnu', () => {
      expect(component.getToolColor('Unknown')).toBe('#6366f1');
    });

    it('devrait retourner l\'icône Jira', () => {
      expect(component.getToolIcon('Jira')).toBe('📊');
    });

    it('devrait retourner le label de l\'outil sélectionné', () => {
      component.selectedTool = 'jira';
      expect(component.getSelectedToolLabel()).toBe('Jira');
    });

    it('devrait retourner l\'aide pour le mode démo', () => {
      component.selectedTool = 'demo';
      expect(component.getToolHint()).toContain('démo');
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 12. ALERTES SLA
  // ══════════════════════════════════════════════════════════════════

  describe('Alertes SLA', () => {

    it('devrait charger les alertes SLA', () => {
      component.ngOnInit();
      expect(component.slaAlerts.length).toBe(1);
    });

    it('devrait résoudre une alerte SLA', () => {
      component.slaAlerts = [{ ...mockSlaAlert }];
      component.resolveSlaAlert(1);
      expect(mockReclamationService.resolveSlaAlert).toHaveBeenCalledWith(1);
      expect(component.slaAlerts.length).toBe(0);
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 13. TRI
  // ══════════════════════════════════════════════════════════════════

  describe('Tri', () => {

    it('devrait trier par un nouveau champ', () => {
      component.sort('status');
      expect(component.sortField).toBe('status');
      expect(component.sortDirection).toBe('asc');
    });

    it('devrait inverser la direction si même champ', () => {
      component.sortField = 'date';
      component.sortDirection = 'asc';
      component.sort('date');
      expect(component.sortDirection).toBe('desc');
    });

    it('devrait retourner l\'icône asc pour champ actif', () => {
      component.sortField = 'date';
      component.sortDirection = 'asc';
      expect(component.sortIcon('date')).toBe('asc');
    });

    it('devrait retourner both pour champ inactif', () => {
      component.sortField = 'date';
      expect(component.sortIcon('status')).toBe('both');
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 14. HELPERS
  // ══════════════════════════════════════════════════════════════════

  describe('Helpers', () => {

    it('devrait formater une date correctement', () => {
      expect(component.formatDate('2024-01-15T10:00:00')).not.toBe('-');
    });

    it('devrait retourner - pour date undefined', () => {
      expect(component.formatDate(undefined)).toBe('-');
    });

    it('devrait formater date relative récente', () => {
      const recent = new Date(Date.now() - 30000).toISOString();
      expect(component.formatRelative(recent)).toBe('À l\'instant');
    });

    it('devrait formater date relative d\'hier', () => {
      const yesterday = new Date(Date.now() - 25 * 3600000).toISOString();
      expect(component.formatRelative(yesterday)).toBe('Hier');
    });

    it('devrait retourner config statut PENDING', () => {
      expect(component.getStatusConfig('PENDING')?.css).toBe('pending');
    });

    it('devrait retourner config type TECHNICAL', () => {
      expect(component.getTypeConfig('TECHNICAL')?.icon).toBe('⚙️');
    });

    it('devrait retourner config priorité haute', () => {
      expect(component.getPriorityConfig(1)?.css).toBe('high');
    });

    it('devrait détecter les infos spécifiques', () => {
      const rec = { ...mockReclamation, browserName: 'Chrome' };
      expect(component.hasSpecificInfo(rec)).toBeTrue();
    });

    it('ne devrait pas détecter d\'infos si vides', () => {
      expect(component.hasSpecificInfo(mockReclamation)).toBeFalse();
    });

    it('devrait afficher une notification et l\'effacer', fakeAsync(() => {
      component.notify('Test message', 'success');
      expect(component.notification?.msg).toBe('Test message');
      tick(4000);
      expect(component.notification).toBeNull();
    }));

  });

  // ══════════════════════════════════════════════════════════════════
  // 15. HIGHLIGHT
  // ══════════════════════════════════════════════════════════════════

  describe('Highlight des mots suspects', () => {

    it('devrait surligner un mot suspect', () => {
      const result = component.highlightWords('Ce texte contient spam', ['spam']);
      expect(result).toContain('<span class="mod-highlight">spam</span>');
    });

    it('devrait retourner texte original si aucun mot', () => {
      expect(component.highlightWords('Texte normal', [])).toBe('Texte normal');
    });

    it('devrait retourner texte vide si vide', () => {
      expect(component.highlightWords('', ['spam'])).toBe('');
    });

    it('devrait surligner plusieurs mots', () => {
      const result = component.highlightWords('spam et fraude', ['spam', 'fraude']);
      const count = (result.match(/mod-highlight/g) || []).length;
      expect(count).toBe(2);
    });

  });

});