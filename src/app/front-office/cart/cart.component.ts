import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CartService, Cart, CartItem } from '../services/cart.service';
import { CourseService } from '../services/course.service';
import { Course } from '../courses/modules/course.model';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { UserService } from '../services/user.service';
import { User } from '../../user';
import { FileUrlService } from '../services/file-url.service';

interface CartItemWithDetails extends CartItem {
  courseDetails?: Course;
}

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.scss']
})
export class CartComponent implements OnInit {
  cart: Cart | null = null;
  cartItemsWithDetails: CartItemWithDetails[] = [];
  loading = false;
  Currentuser: User | null = null;

  get learnerId(): string | null {
    return this.Currentuser?.id ?? null;
  }

  constructor(
    private cartService: CartService,
    private courseService: CourseService,
    private router: Router,
    private userService: UserService,
    private fileUrlService: FileUrlService,
  ) {}

  ngOnInit(): void {
        this.Currentuser = this.userService.getUser() || null;

    this.loadCart();
  }

  loadCart(): void {
    this.loading = true;
    const learnerId = this.learnerId;
    if (!learnerId) {
      this.cart = { learnerId: '', items: [] };
      this.cartItemsWithDetails = [];
      this.loading = false;
      return;
    }

    this.cartService.getCart(learnerId).subscribe({
      next: (cart) => {
        // S'assurer que le panier a toujours une structure valide
        if (!cart.items) {
          cart.items = [];
        }
        this.cart = cart;
        
        // Charger les détails complets de chaque cours
        if (cart.items.length > 0) {
          this.loadCourseDetails(cart.items);
        } else {
          this.cartItemsWithDetails = [];
          this.loading = false;
        }
      },
      error: (error) => {
        console.error('Erreur lors du chargement du panier:', error);
        // Créer un panier vide en cas d'erreur
        this.cart = { learnerId, items: [] };
        this.cartItemsWithDetails = [];
        this.loading = false;
      }
    });
  }

  loadCourseDetails(items: CartItem[]): void {
    const courseDetailRequests = items.map(item => 
      this.courseService.getCourseById(item.courseId).pipe(
        map(course => ({
          ...item,
          courseDetails: course
        }))
      )
    );

    forkJoin(courseDetailRequests).subscribe({
      next: (itemsWithDetails) => {
        this.cartItemsWithDetails = itemsWithDetails;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des détails des cours:', error);
        // En cas d'erreur, utiliser les items sans détails
        this.cartItemsWithDetails = items.map(item => ({ ...item }));
        this.loading = false;
      }
    });
  }

  removeItem(itemId: number): void {
    const learnerId = this.learnerId;
    if (!this.cart || !learnerId) return;
    
    // Confirmation améliorée
    if (confirm('Êtes-vous sûr de vouloir retirer ce cours du panier ?')) {
      // Optimistic update: retirer l'item de la liste immédiatement
      const itemToRemove = this.cartItemsWithDetails.find(item => item.id === itemId);
      this.cartItemsWithDetails = this.cartItemsWithDetails.filter(item => item.id !== itemId);
      
      // Mettre à jour le panier localement
      if (this.cart.items) {
        this.cart.items = this.cart.items.filter(item => item.id !== itemId);
      }
      
      // Appel API
      this.cartService.removeItemFromCart(learnerId, itemId).subscribe({
        next: () => {
          // Recharger le panier pour s'assurer de la synchronisation
          this.loadCart();
          // Afficher un message de succès (optionnel)
          console.log('Cours retiré du panier avec succès');
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          // Restaurer l'item en cas d'erreur
          if (itemToRemove && this.cart) {
            this.cartItemsWithDetails.push(itemToRemove);
            if (this.cart.items) {
              this.cart.items.push(itemToRemove);
            }
          }
          alert('Erreur lors de la suppression. Veuillez réessayer.');
        }
      });
    }
  }

  // Supprimer tous les items du panier
  clearCart(): void {
    const learnerId = this.learnerId;
    if (!this.cart || !learnerId || this.isEmpty()) return;
    
    if (confirm('Êtes-vous sûr de vouloir vider complètement votre panier ?')) {
      // Supprimer tous les items un par un
      const itemIds = this.cartItemsWithDetails.map(item => item.id).filter(id => id !== undefined) as number[];
      
      if (itemIds.length === 0) return;
      
      // Supprimer visuellement tous les items
      this.cartItemsWithDetails = [];
      this.cart.items = [];
      
      // Supprimer via l'API (on peut aussi créer un endpoint pour vider tout le panier)
      let completed = 0;
      itemIds.forEach(itemId => {
        this.cartService.removeItemFromCart(learnerId, itemId).subscribe({
          next: () => {
            completed++;
            if (completed === itemIds.length) {
              this.loadCart(); // Recharger une dernière fois
            }
          },
          error: (error) => {
            console.error('Erreur lors de la suppression:', error);
            this.loadCart(); // Recharger en cas d'erreur
          }
        });
      });
    }
  }

  getLevelClass(level: string): string {
    return level?.toLowerCase() || 'beginner';
  }

  getThumbnailUrl(thumbnailUrl?: string, coursId?: number): string {
    return this.fileUrlService.getThumbnailUrl(thumbnailUrl || '', coursId);
  }

  getLevelColor(level: string): string {
    const colors: { [key: string]: string } = {
      'beginner': '#10b981',
      'intermediate': '#f59e0b',
      'advanced': '#ef4444',
      'expert': '#8b5cf6'
    };
    return colors[level?.toLowerCase()] || colors['beginner'];
  }

  getTotal(): number {
    return this.cartService.getCartTotal(this.cart);
  }

  proceedToPayment(): void {
    this.router.navigate(['/cart/payment']);
  }

  continueShopping(): void {
    this.router.navigate(['/learner/courses']);
  }

  isEmpty(): boolean {
    return !this.cart || !this.cart.items || this.cart.items.length === 0;
  }

  viewCourse(courseId: number): void {
    this.router.navigate(['/learner_course/detail', courseId]);
  }

  getSubtotal(): number {
    return this.getTotal();
  }

  getItemCount(): number {
    return this.cart?.items?.length || 0;
  }
}
