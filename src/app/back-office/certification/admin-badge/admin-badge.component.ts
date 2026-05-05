import { Component, OnInit } from '@angular/core';
import { AdminGamificationService } from '../../services/admin-gamification.service';

@Component({
  selector: 'app-admin-badge',
  templateUrl: './admin-badge.component.html',
  styleUrl: './admin-badge.component.scss',
})
export class AdminBadgeComponent implements OnInit {
  badges: any[] = [];
  isEditMode = false;
  badgeForm: any = this.initForm();

  constructor(private badgeService: AdminGamificationService) {}

  ngOnInit(): void {
    this.loadBadges();
  }

  initForm() {
    return {
      name: '',
      description: '',
      iconBase64: '',
      minScoreRequired: 0,
      threshold: 0,
      badgeType: 'SCORE_EXCELLENCE',
      category: 'GENERAL',
    };
  }

  loadBadges() {
    this.badgeService.getBadges().subscribe({
      next: (data) => (this.badges = data),
      error: (err) => console.error('Error loading badges', err),
    });
  }

  handleImage(event: any) {
    // Assure-toi que le nom correspond à (change) dans le HTML
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => {
        this.badgeForm.iconBase64 = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  onSubmit() {
    if (this.isEditMode) {
      this.badgeService
        .updateBadge(this.badgeForm.id, this.badgeForm)
        .subscribe(() => this.handleSuccess());
    } else {
      this.badgeService
        .createBadge(this.badgeForm)
        .subscribe(() => this.handleSuccess());
    }
  }

  // CETTE MÉTHODE MANQUAIT :
  prepareUpdate(badge: any) {
    this.badgeForm = { ...badge };
    this.isEditMode = true;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // CELLE-CI AUSSI :
  onDelete(id: number) {
    if (confirm('Are you sure you want to delete this badge?')) {
      this.badgeService.deleteBadge(id).subscribe(() => this.loadBadges());
    }
  }

  handleSuccess() {
    this.loadBadges();
    this.resetForm();
  }

  resetForm() {
    this.badgeForm = this.initForm();
    this.isEditMode = false;
  }
}
