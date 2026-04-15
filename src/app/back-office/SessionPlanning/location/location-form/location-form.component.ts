// src/app/components/location-form/location-form.component.ts

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LocationService } from '../../../../front-office/SessionPlanning/services/location.service';
import { Location, LocationType, LocationTypeLabels, LocationTypeIcons, LocationTypeColors } from '../../../../front-office/SessionPlanning/models/location';

@Component({
  selector: 'app-location-form',
  templateUrl: './location-form.component.html',
  styleUrls: ['./location-form.component.scss']
})
export class LocationFormComponent implements OnInit {
  locationForm!: FormGroup;
  isEditMode: boolean = false;
  locationId: number | null = null;
  isSubmitting: boolean = false;
  statusMessage: string = '';
  statusType: 'success' | 'error' = 'success';
  
  // Types de lieux
 locationTypes = [
  { value: LocationType.ROOM, label: 'Salle', icon: '📍' },
  { value: LocationType.ONLINE_PLATFORM, label: 'Plateforme en ligne', icon: '🌐' },
  { value: LocationType.HYBRID, label: 'Hybride', icon: '🔄' }
];

  constructor(
    private fb: FormBuilder,
    private locationService: LocationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.checkEditMode();
  }

  initForm(): void {
    this.locationForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      address: ['', Validators.required],
      city: ['', Validators.required],
      capacity: ['', [Validators.required, Validators.min(1)]],
      type: ['', Validators.required]
    });
  }

  checkEditMode(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.locationId = +id;
      this.loadLocationData(this.locationId);
    }
  }

  loadLocationData(id: number): void {
    this.locationService.getLocationById(id).subscribe({
      next: (location) => {
        this.locationForm.patchValue({
          name: location.name,
          address: location.address,
          city: location.city,
          capacity: location.capacity,
          type: location.type
        });
      },
      error: (error) => {
        this.showStatus('Erreur lors du chargement du lieu', 'error');
        console.error('Error loading location:', error);
      }
    });
  }

  getSelectedType(): LocationType | null {
    return this.locationForm.get('type')?.value;
  }

  getSelectedTypeLabel(): string {
    const type = this.getSelectedType();
    if (!type) return 'Type non sélectionné';
    return LocationTypeLabels[type];
  }

  getSelectedTypeIcon(): string {
    const type = this.getSelectedType();
    if (!type) return '📍';
    return LocationTypeIcons[type];
  }

  getSelectedTypeColor(): string {
    const type = this.getSelectedType();
    if (!type) return '#6366f1';
    return LocationTypeColors[type];
  }

  hasAnyData(): boolean {
    return !!(this.locationForm.get('name')?.value ||
              this.locationForm.get('address')?.value ||
              this.locationForm.get('city')?.value ||
              this.locationForm.get('capacity')?.value ||
              this.locationForm.get('type')?.value);
  }

  onSubmit(): void {
    if (this.locationForm.invalid) {
      this.showStatus('Veuillez corriger les erreurs dans le formulaire', 'error');
      return;
    }

    this.isSubmitting = true;
    const locationData: Location = this.locationForm.value;

    if (this.isEditMode && this.locationId) {
      this.locationService.updateLocation(this.locationId, locationData).subscribe({
        next: () => {
          this.showStatus('Lieu mis à jour avec succès', 'success');
          setTimeout(() => this.goBack(), 1500);
        },
        error: (error) => {
          this.showStatus('Erreur lors de la mise à jour', 'error');
          console.error('Error updating location:', error);
          this.isSubmitting = false;
        }
      });
    } else {
      this.locationService.createLocation(locationData).subscribe({
        next: () => {
          this.showStatus('Lieu créé avec succès', 'success');
          setTimeout(() => this.goBack(), 1500);
        },
        error: (error) => {
          this.showStatus('Erreur lors de la création', 'error');
          console.error('Error creating location:', error);
          this.isSubmitting = false;
        }
      });
    }
  }

  showStatus(message: string, type: 'success' | 'error'): void {
    this.statusMessage = message;
    this.statusType = type;
    
    if (type === 'success') {
      setTimeout(() => {
        this.statusMessage = '';
      }, 3000);
    }
  }

  goBack(): void {
    const returnUrl = this.route.snapshot.queryParams['returnUrl'];
    if (returnUrl) {
      this.router.navigateByUrl(returnUrl);
    } else {
      this.router.navigate(['/admin/locations']);
    }
  }
}