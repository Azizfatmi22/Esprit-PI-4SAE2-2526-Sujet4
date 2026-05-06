/// <reference types="jasmine" />

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';

import { CartComponent } from './cart.component';
import { CartService } from '../services/cart.service';
import { CourseService } from '../services/course.service';
import { UserService } from '../services/user.service';
import { FileUrlService } from '../services/file-url.service';

// ───────── HELPERS (IMPORTANT) ─────────

function createMockUser() {
  return {
    id: 'user-123',
    username: 'ines',
    email: 'ines@test.com',
    fullName: 'Ines Test',
    roles: ['LEARNER']
  };
}

function createMockCart() {
  return {
    learnerId: 'user-123',
    items: [
      {
        id: 1,
        courseId: 10,
        courseTitle: 'Angular',
        coursePrice: 50
      },
      {
        id: 2,
        courseId: 20,
        courseTitle: 'Spring Boot',
        coursePrice: 60
      }
    ]
  };
}

// ───────── MOCKS ─────────

const mockCourse1 = { id: 10, title: 'Angular' };
const mockCourse2 = { id: 20, title: 'Spring Boot' };

let mockCartService: any;
let mockCourseService: any;
let mockUserService: any;
let mockRouter: any;
let mockFileUrlService: any;

// ───────── TESTS ─────────

describe('CartComponent', () => {
  let component: CartComponent;
  let fixture: ComponentFixture<CartComponent>;

  beforeEach(async () => {

    // ⚠️ recréer les mocks à chaque test
    mockCartService = {
      getCart: jasmine.createSpy('getCart').and.callFake(() => of(createMockCart())),
      removeItemFromCart: jasmine.createSpy('removeItemFromCart').and.returnValue(of({})),
      getCartTotal: jasmine.createSpy('getCartTotal').and.returnValue(100)
    };

    mockCourseService = {
      getCourseById: jasmine.createSpy('getCourseById')
        .and.callFake((id: number) => {
          if (id === 10) return of(mockCourse1);
          if (id === 20) return of(mockCourse2);
          return of(null);
        })
    };

    mockUserService = {
      getUser: jasmine.createSpy('getUser').and.returnValue(createMockUser())
    };

    mockRouter = {
      navigate: jasmine.createSpy('navigate')
    };

    mockFileUrlService = {
      getThumbnailUrl: jasmine.createSpy('getThumbnailUrl').and.returnValue('url')
    };

    await TestBed.configureTestingModule({
      declarations: [CartComponent],
      providers: [
        { provide: CartService, useValue: mockCartService },
        { provide: CourseService, useValue: mockCourseService },
        { provide: UserService, useValue: mockUserService },
        { provide: Router, useValue: mockRouter },
        { provide: FileUrlService, useValue: mockFileUrlService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CartComponent);
    component = fixture.componentInstance;
  });

  // ═══════════════════════════════
  // 1. INITIALISATION
  // ═══════════════════════════════

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait récupérer l’utilisateur au ngOnInit', () => {
    component.ngOnInit();
    expect(component.Currentuser?.id).toBe('user-123');
  });

  it('devrait charger le panier', () => {
    component.ngOnInit();
    expect(mockCartService.getCart).toHaveBeenCalledWith('user-123');
  });

  // ═══════════════════════════════
  // 2. LOAD CART
  // ═══════════════════════════════

  it('devrait charger les items avec détails', () => {
    component.ngOnInit();

    expect(component.cartItemsWithDetails.length).toBe(2);
    expect(component.cartItemsWithDetails[0].courseDetails?.title).toBe('Angular');
  });

  it('devrait gérer une erreur de chargement panier', () => {
    mockCartService.getCart.and.returnValue(throwError(() => new Error()));

    component.loadCart();

    expect(component.cart?.items.length).toBe(0);
  });

  // ═══════════════════════════════
  // 3. REMOVE ITEM
  // ═══════════════════════════════

  it('devrait supprimer un item du panier', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.ngOnInit();
    component.removeItem(1);

    expect(mockCartService.removeItemFromCart).toHaveBeenCalledWith('user-123', 1);
  });

  it('ne doit pas supprimer si cancel', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.ngOnInit();

    // 🔥 reset du spy AVANT le test réel
    mockCartService.removeItemFromCart.calls.reset();

    component.removeItem(1);

    expect(mockCartService.removeItemFromCart).not.toHaveBeenCalled();
  });

  // ═══════════════════════════════
  // 4. CLEAR CART
  // ═══════════════════════════════

  it('devrait vider le panier', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.ngOnInit();
    component.clearCart();

    expect(mockCartService.removeItemFromCart).toHaveBeenCalled();
  });

  // ═══════════════════════════════
  // 5. HELPERS
  // ═══════════════════════════════

  it('devrait retourner le total', () => {
    component.cart = createMockCart();
    expect(component.getTotal()).toBe(100);
  });

  it('devrait retourner le nombre d’items', () => {
    component.cart = createMockCart(); // 🔥 IMPORTANT
    expect(component.getItemCount()).toBe(2);
  });

  it('devrait détecter panier vide', () => {
    component.cart = { learnerId: 'user-123', items: [] };
    expect(component.isEmpty()).toBeTrue();
  });

  it('devrait retourner une couleur selon le niveau', () => {
    expect(component.getLevelColor('beginner')).toBe('#10b981');
  });

  // ═══════════════════════════════
  // 6. NAVIGATION
  // ═══════════════════════════════

  it('devrait naviguer vers paiement', () => {
    component.proceedToPayment();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/cart/payment']);
  });

  it('devrait continuer shopping', () => {
    component.continueShopping();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/learner/courses']);
  });

  it('devrait voir un cours', () => {
    component.viewCourse(10);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/learner_course/detail', 10]);
  });

});