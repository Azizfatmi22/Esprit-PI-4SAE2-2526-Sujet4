/// <reference types="jasmine" />

// ← Mock SockJS AVANT tout import
(window as any).global = window;
(window as any).SockJS = function() {
  return {
    close: () => {},
    send: () => {},
    onopen: null,
    onclose: null,
    onmessage: null,
    readyState: 3  // ← CLOSED, évite toute tentative de connexion
  };
};

import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NgZone, ChangeDetectorRef } from '@angular/core';
import { of } from 'rxjs';
import { FormsModule } from '@angular/forms';

import { ReclamationComponent } from './reclamation.component.component';
import { ReclamationService, Reclamation, ReclamationResponse } from '../../services/reclamation.service';
import { UserService } from '../../services/user.service';

// ── MOCKS ─────────────────────────────────────────────────────────────────────

const mockUser = {
  id: 'user-123',
  username: 'ines',
  email: 'ines@test.com',
  fullName: 'Ines Test',
  roles: ['LEARNER']
};

const mockReclamation: Reclamation = {
  id: 1,
  learnerId: 'user-123',
  type: 'TECHNICAL',
  subject: 'Problème de connexion',
  description: 'Je ne peux pas me connecter à la plateforme',
  status: 'PENDING',
  priority: 2,
  createdDate: new Date().toISOString()
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

const mockReclamationService = {
  getReclamationsByLearner: jasmine.createSpy('getReclamationsByLearner').and.returnValue(of([mockReclamation])),
  getPublicResponses: jasmine.createSpy('getPublicResponses').and.returnValue(of([mockResponse])),
  createReclamation: jasmine.createSpy('createReclamation').and.returnValue(of(mockReclamation)),
  deleteReclamation: jasmine.createSpy('deleteReclamation').and.returnValue(of({})),
  createResponse: jasmine.createSpy('createResponse').and.returnValue(of(mockResponse)),
  createResponseWithQuote: jasmine.createSpy('createResponseWithQuote').and.returnValue(of(mockResponse)),
  submitSatisfaction: jasmine.createSpy('submitSatisfaction').and.returnValue(of({ ...mockReclamation, satisfactionScore: 5 })),
  addReaction: jasmine.createSpy('addReaction').and.returnValue(of({ ...mockResponse, reaction: '👍' })),
  detectReclamationType: jasmine.createSpy('detectReclamationType').and.returnValue(of({ hasSuggestion: false })),
  extractInformation: jasmine.createSpy('extractInformation').and.returnValue(of({ hasData: false })),
  buildTimeline: jasmine.createSpy('buildTimeline').and.returnValue([]),
};

const mockUserService = {
  getUser: jasmine.createSpy('getUser').and.returnValue(mockUser)
};

// ── TESTS ─────────────────────────────────────────────────────────────────────

describe('ReclamationComponent', () => {
  let component: ReclamationComponent;
  let fixture: ComponentFixture<ReclamationComponent>;

  beforeEach(async () => {
    // Reset spies avant chaque test
    Object.values(mockReclamationService).forEach(spy => {
      if (spy && typeof spy.calls !== 'undefined') {
        (spy as jasmine.Spy).calls.reset();
      }
    });
    mockReclamationService.getReclamationsByLearner.and.returnValue(of([mockReclamation]));
    mockReclamationService.getPublicResponses.and.returnValue(of([mockResponse]));
    mockReclamationService.detectReclamationType.and.returnValue(of({ hasSuggestion: false }));

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [ReclamationComponent],
      providers: [
        { provide: ReclamationService, useValue: mockReclamationService },
        { provide: UserService, useValue: mockUserService },
        ChangeDetectorRef
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReclamationComponent);
    component = fixture.componentInstance;

    // ← Empêcher initWebSocket de créer de vraies connexions
    spyOn<any>(component, 'initWebSocket').and.callFake(() => {});
  });

  afterEach(() => {
    // Nettoyer le composant proprement
    if (component) {
      component.ngOnDestroy();
    }
  });

  // ══════════════════════════════════════════════════════════════════
  // 1. INITIALISATION
  // ══════════════════════════════════════════════════════════════════

  describe('Initialisation', () => {

    it('devrait créer le composant', () => {
      expect(component).toBeTruthy();
    });

    it('devrait récupérer l\'utilisateur courant au ngOnInit', () => {
      component.ngOnInit();
      expect(component.Currentuser).toEqual(mockUser);
    });

    it('devrait avoir learnerId égal à l\'id de l\'utilisateur', () => {
      component.Currentuser = mockUser;
      expect(component.learnerId).toBe('user-123');
    });

    it('devrait retourner null si aucun utilisateur connecté', () => {
      component.Currentuser = null;
      expect(component.learnerId).toBeNull();
    });

    it('devrait charger les réclamations au ngOnInit', () => {
      component.ngOnInit();
      expect(mockReclamationService.getReclamationsByLearner).toHaveBeenCalledWith('user-123');
      expect(component.reclamations.length).toBe(1);
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 2. NAVIGATION
  // ══════════════════════════════════════════════════════════════════

  describe('Navigation', () => {

    it('devrait aller au formulaire', () => {
      component.goToForm();
      expect(component.currentView).toBe('form');
    });

    it('devrait retourner à la liste', () => {
      component.currentView = 'form';
      component.goToList();
      expect(component.currentView).toBe('list');
    });

    it('devrait aller à la FAQ', () => {
      component.goToFaq();
      expect(component.currentView).toBe('faq');
    });

    it('devrait ouvrir le détail d\'une réclamation', () => {
      component.openDetail(mockReclamation);
      expect(component.currentView).toBe('detail');
      expect(component.selectedReclamation?.id).toBe(1);
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 3. FILTRES ET RECHERCHE
  // ══════════════════════════════════════════════════════════════════

  describe('Filtres et Recherche', () => {

    beforeEach(() => {
      component.reclamations = [mockReclamation];
    });

    it('devrait filtrer par statut PENDING', () => {
      component.setFilter('PENDING');
      expect(component.activeFilter).toBe('PENDING');
      expect(component.filteredReclamations.length).toBe(1);
    });

    it('devrait afficher toutes les réclamations avec le filtre ALL', () => {
      component.setFilter('ALL');
      expect(component.filteredReclamations.length).toBe(1);
    });

    it('devrait filtrer par recherche textuelle', () => {
      component.searchQuery = 'connexion';
      component.applyFilters();
      expect(component.filteredReclamations.length).toBe(1);
    });

    it('devrait retourner 0 résultats pour une recherche sans correspondance', () => {
      component.searchQuery = 'xyz-inexistant';
      component.applyFilters();
      expect(component.filteredReclamations.length).toBe(0);
    });

    it('devrait compter correctement les réclamations par filtre', () => {
      expect(component.getFilterCount('ALL')).toBe(1);
      expect(component.getFilterCount('PENDING')).toBe(1);
      expect(component.getFilterCount('RESOLVED')).toBe(0);
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 4. STATISTIQUES
  // ══════════════════════════════════════════════════════════════════

  describe('Statistiques', () => {

    it('devrait calculer les statistiques correctement', () => {
      component.reclamations = [
        { ...mockReclamation, status: 'PENDING' },
        { ...mockReclamation, id: 2, status: 'RESOLVED' },
        { ...mockReclamation, id: 3, status: 'IN_PROGRESS' }
      ];
      component.computeStats();
      expect(component.stats.total).toBe(3);
      expect(component.stats.pending).toBe(1);
      expect(component.stats.resolved).toBe(1);
      expect(component.stats.inProgress).toBe(1);
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 5. FORMULAIRE DE RÉCLAMATION
  // ══════════════════════════════════════════════════════════════════

  describe('Formulaire de Réclamation', () => {

    it('devrait valider le formulaire si sujet et description sont remplis', () => {
      component.reclamationForm.subject = 'Mon problème';
      component.reclamationForm.description = 'Description détaillée';
      expect(component.isFormValid()).toBeTrue();
    });

    it('devrait invalider le formulaire si le sujet est vide', () => {
      component.reclamationForm.subject = '';
      component.reclamationForm.description = 'Description';
      expect(component.isFormValid()).toBeFalse();
    });

    it('devrait invalider le formulaire si la description est vide', () => {
      component.reclamationForm.subject = 'Sujet';
      component.reclamationForm.description = '';
      expect(component.isFormValid()).toBeFalse();
    });

    it('devrait réinitialiser le formulaire', () => {
      component.reclamationForm.subject = 'test';
      component.resetForm();
      expect(component.reclamationForm.subject).toBe('');
    });

    it('devrait soumettre une réclamation valide', () => {
      component.Currentuser = mockUser;
      component.reclamationForm.subject = 'Mon problème';
      component.reclamationForm.description = 'Description détaillée';
      component.reclamationForm.learnerId = 'user-123';
      component.submitReclamation();
      expect(mockReclamationService.createReclamation).toHaveBeenCalled();
    });

    it('ne devrait pas soumettre si le formulaire est invalide', () => {
      component.reclamationForm.subject = '';
      component.submitReclamation();
      expect(mockReclamationService.createReclamation).not.toHaveBeenCalled();
    });

    it('devrait calculer la progression du formulaire', () => {
      component.reclamationForm.subject = 'Sujet';
      component.reclamationForm.description = 'Description';
      const progress = component.formProgress;
      expect(progress).toBeGreaterThan(0);
      expect(progress).toBeLessThanOrEqual(100);
    });

    it('devrait afficher les champs spécifiques selon le type TECHNICAL', () => {
      component.reclamationForm.type = 'TECHNICAL';
      expect(component.showField('browserName')).toBeTrue();
      expect(component.showField('transactionId')).toBeFalse();
    });

    it('devrait afficher les champs PAYMENT pour le type PAYMENT', () => {
      component.reclamationForm.type = 'PAYMENT';
      expect(component.showField('transactionId')).toBeTrue();
      expect(component.showField('browserName')).toBeFalse();
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 6. SUPPRESSION DE RÉCLAMATION
  // ══════════════════════════════════════════════════════════════════

  describe('Suppression de Réclamation', () => {

    it('devrait supprimer une réclamation', () => {
      component.reclamations = [mockReclamation];
      spyOn(window, 'confirm').and.returnValue(true);
      const event = new MouseEvent('click');
      component.deleteReclamation(1, event);
      expect(mockReclamationService.deleteReclamation).toHaveBeenCalledWith(1);
    });

    it('ne devrait pas supprimer si l\'utilisateur annule', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      const event = new MouseEvent('click');
      component.deleteReclamation(1, event);
      expect(mockReclamationService.deleteReclamation).not.toHaveBeenCalled();
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 7. RÉPONSES ET MESSAGES
  // ══════════════════════════════════════════════════════════════════

  describe('Réponses et Messages', () => {

    it('devrait identifier un message de l\'apprenant par senderType LEARNER', () => {
      const resp: ReclamationResponse = { ...mockResponse, senderType: 'LEARNER' };
      component.Currentuser = mockUser;
      expect(component.isLearnerMessage(resp)).toBeTrue();
    });

    it('devrait identifier un message admin par senderType ADMIN', () => {
      const resp: ReclamationResponse = { ...mockResponse, senderType: 'ADMIN' };
      expect(component.isLearnerMessage(resp)).toBeFalse();
    });

    it('devrait permettre de répondre si statut PENDING', () => {
      expect(component.canLearnerReply({ ...mockReclamation, status: 'PENDING' })).toBeTrue();
    });

    it('devrait permettre de répondre si statut IN_PROGRESS', () => {
      expect(component.canLearnerReply({ ...mockReclamation, status: 'IN_PROGRESS' })).toBeTrue();
    });

    it('ne devrait pas permettre de répondre si statut RESOLVED', () => {
      expect(component.canLearnerReply({ ...mockReclamation, status: 'RESOLVED' })).toBeFalse();
    });

    it('devrait gérer le texte de réponse de l\'apprenant', () => {
      component.setLearnerReply(1, 'Ma réponse');
      expect(component.getLearnerReply(1)).toBe('Ma réponse');
    });

    it('devrait soumettre une réponse avec citation', () => {
      component.Currentuser = mockUser;
      component.setLearnerReply(1, 'Ma réponse');
      component.submitLearnerReplyWithQuote(mockReclamation);
      expect(mockReclamationService.createResponseWithQuote).toHaveBeenCalled();
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 8. SATISFACTION
  // ══════════════════════════════════════════════════════════════════

  describe('Satisfaction', () => {

    it('devrait nécessiter une évaluation pour une réclamation résolue sans score', () => {
      const rec = { ...mockReclamation, status: 'RESOLVED', satisfactionScore: undefined };
      expect(component.needsSatisfaction(rec)).toBeTrue();
    });

    it('ne devrait pas nécessiter d\'évaluation si déjà noté', () => {
      const rec = { ...mockReclamation, status: 'RESOLVED', satisfactionScore: 4 };
      expect(component.needsSatisfaction(rec)).toBeFalse();
    });

    it('ne devrait pas nécessiter d\'évaluation si statut PENDING', () => {
      expect(component.needsSatisfaction(mockReclamation)).toBeFalse();
    });

    it('devrait initialiser un brouillon de satisfaction', () => {
      const draft = component.getSatisfactionDraft(1);
      expect(draft.score).toBe(0);
      expect(draft.comment).toBe('');
    });

    it('devrait mettre à jour le score de satisfaction', () => {
      component.setSatisfactionScore(1, 5);
      expect(component.getSatisfactionDraft(1).score).toBe(5);
    });

    it('devrait soumettre la satisfaction', () => {
      component.Currentuser = mockUser;
      component.setSatisfactionScore(1, 5);
      component.submitSatisfaction(mockReclamation);
      expect(mockReclamationService.submitSatisfaction).toHaveBeenCalledWith(1, {
        score: 5,
        comment: '',
        learnerId: 'user-123'
      });
    });

    it('ne devrait pas soumettre si score = 0', () => {
      component.setSatisfactionScore(1, 0);
      component.submitSatisfaction(mockReclamation);
      expect(mockReclamationService.submitSatisfaction).not.toHaveBeenCalled();
    });

    it('devrait retourner le bon label pour chaque score', () => {
      expect(component.starLabel(1)).toBe('Très insatisfait');
      expect(component.starLabel(3)).toBe('Neutre');
      expect(component.starLabel(5)).toBe('Très satisfait');
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 9. CITATIONS
  // ══════════════════════════════════════════════════════════════════

  describe('Citations', () => {

    it('devrait citer un message admin', () => {
      component.Currentuser = mockUser;
      component.quoteMessage(1, mockResponse);
      const quote = component.getQuote(1);
      expect(quote).not.toBeNull();
      expect(quote?.text).toBe(mockResponse.responseText);
      expect(quote?.author).toBe('Administrateur');
    });

    it('devrait annuler une citation', () => {
      component.quoteMessage(1, mockResponse);
      component.cancelQuote(1);
      expect(component.getQuote(1)).toBeNull();
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 10. HELPERS ET FORMATAGE
  // ══════════════════════════════════════════════════════════════════

  describe('Helpers et Formatage', () => {

    it('devrait formater une date correctement', () => {
      const date = '2024-01-15T10:00:00';
      const formatted = component.formatDate(date);
      expect(formatted).not.toBe('-');
    });

    it('devrait retourner - pour une date undefined', () => {
      expect(component.formatDate(undefined)).toBe('-');
    });

    it('devrait retourner la config de statut PENDING', () => {
      expect(component.getStatusConfig('PENDING').css).toBe('pending');
    });

    it('devrait retourner la config de statut RESOLVED', () => {
      expect(component.getStatusConfig('RESOLVED').css).toBe('resolved');
    });

    it('devrait retourner la config de priorité haute', () => {
      expect(component.getPriorityConfig(1).css).toBe('high');
    });

    it('devrait retourner la config de priorité basse', () => {
      expect(component.getPriorityConfig(3).css).toBe('low');
    });

    it('devrait retourner la config de type TECHNICAL', () => {
      expect(component.getTypeConfig('TECHNICAL').icon).toBe('⚙️');
    });

    it('devrait retourner la config de type PAYMENT', () => {
      expect(component.getTypeConfig('PAYMENT').icon).toBe('💳');
    });

    it('devrait afficher une card après toggle', () => {
      component.toggleCard(1);
      expect(component.isExpanded(1)).toBeTrue();
    });

    it('devrait cacher une card après double toggle', () => {
      component.toggleCard(1);
      component.toggleCard(1);
      expect(component.isExpanded(1)).toBeFalse();
    });

    it('devrait afficher une notification', fakeAsync(() => {
      component.showNotification('Test message', 'success');
      expect(component.notification?.message).toBe('Test message');
      tick(3500);
      expect(component.notification).toBeNull();
    }));

    it('devrait retourner la date du jour au format ISO', () => {
      const today = component.todayDate;
      expect(today).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 11. ONBOARDING
  // ══════════════════════════════════════════════════════════════════

  describe('Onboarding', () => {

    beforeEach(() => {
      localStorage.removeItem('onboarding_seen');
    });

    it('devrait avancer à l\'étape suivante', () => {
      component.onboardingStep = 0;
      component.nextOnboardingStep();
      expect(component.onboardingStep).toBe(1);
    });

    it('devrait reculer à l\'étape précédente', () => {
      component.onboardingStep = 2;
      component.prevOnboardingStep();
      expect(component.onboardingStep).toBe(1);
    });

    it('devrait finir l\'onboarding à la dernière étape', () => {
      component.onboardingStep = component.onboardingSteps.length - 1;
      component.nextOnboardingStep();
      expect(component.showOnboarding).toBeFalse();
      expect(localStorage.getItem('onboarding_seen')).toBe('1');
    });

    it('devrait sauter l\'onboarding', () => {
      component.showOnboarding = true;
      component.skipOnboarding();
      expect(component.showOnboarding).toBeFalse();
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 12. DÉTECTION DE TYPE (IA)
  // ══════════════════════════════════════════════════════════════════

  describe('Détection automatique de type', () => {

    it('devrait afficher une suggestion si l\'IA en retourne une', () => {
      // ← Pas de fakeAsync : appel direct à autoDetectType()
      mockReclamationService.detectReclamationType.and.returnValue(of({
        hasSuggestion: true,
        suggestedType: 'PAYMENT',
        suggestedLabel: 'Problème de paiement',
        confidence: 80,
        matchedKeywords: ['paiement', 'remboursement']
      }));

      component.reclamationForm.type = 'OTHER';
      component.reclamationForm.subject = 'Mon paiement a échoué hier';
      component.reclamationForm.description = 'La transaction a été refusée';

      component.autoDetectType();  // ← Direct, pas via onTextChange

      expect(component.showTypeSuggestion).toBeTrue();
      expect(component.suggestedType).toBe('PAYMENT');
    });

    it('ne devrait pas afficher de suggestion si le type est déjà sélectionné', () => {
      mockReclamationService.detectReclamationType.and.returnValue(of({
        hasSuggestion: true,
        suggestedType: 'TECHNICAL',
        suggestedLabel: 'Problème technique',
        confidence: 75,
        matchedKeywords: ['erreur']
      }));

      component.reclamationForm.type = 'TECHNICAL'; // ← même type
      component.autoDetectType();

      expect(component.showTypeSuggestion).toBeFalse();
    });

    it('devrait appliquer la suggestion', () => {
      component.suggestedType = 'PAYMENT';
      component.suggestedTypeLabel = 'Problème de paiement';
      component.showTypeSuggestion = true;
      component.applySuggestion();
      expect(component.reclamationForm.type).toBe('PAYMENT');
      expect(component.showTypeSuggestion).toBeFalse();
    });

    it('devrait ignorer la suggestion', () => {
      component.showTypeSuggestion = true;
      component.dismissSuggestion();
      expect(component.showTypeSuggestion).toBeFalse();
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 13. RÉACTIONS
  // ══════════════════════════════════════════════════════════════════

  describe('Réactions aux messages', () => {

    it('devrait ajouter une réaction 👍', () => {
      component.reclamationResponsesMap.set(1, [mockResponse]);
      component.react(mockResponse, '👍');
      expect(mockReclamationService.addReaction).toHaveBeenCalledWith(1, '👍');
    });

    it('devrait supprimer une réaction', () => {
      component.removeReaction(mockResponse);
      expect(mockReclamationService.addReaction).toHaveBeenCalledWith(1, '');
    });

  });

  // ══════════════════════════════════════════════════════════════════
  // 14. FAQ
  // ══════════════════════════════════════════════════════════════════

  describe('FAQ', () => {

    beforeEach(() => {
      component.ngOnInit();
    });

    it('devrait construire les items FAQ', () => {
      expect(component.faqItems.length).toBeGreaterThan(0);
    });

    it('devrait filtrer la FAQ par recherche', () => {
      component.faqSearch = 'paiement';
      expect(component.filteredFaq.length).toBeGreaterThan(0);
    });

    it('devrait retourner tous les items si recherche vide', () => {
      component.faqSearch = '';
      expect(component.filteredFaq.length).toBe(component.faqItems.length);
    });

    it('devrait toggle un item FAQ', () => {
      component.toggleFaq(0);
      expect(component.expandedFaq.has(0)).toBeTrue();
      component.toggleFaq(0);
      expect(component.expandedFaq.has(0)).toBeFalse();
    });

  });

});