import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LocationService } from '../../../../front-office/SessionPlanning/services/location.service';
import {
  Location,
  LocationType,
  LocationTypeLabels,
  LocationTypeIcons,
  LocationTypeColors
} from '../../../../front-office/SessionPlanning/models/location';

@Component({
  selector: 'app-location-detail',
  templateUrl: './location-detail.component.html',
  styleUrls: ['./location-detail.component.scss']
})
export class LocationDetailComponent implements OnInit {
  location: Location | null = null;
  isLoading = true;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private locationService: LocationService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const locationId = idParam ? Number(idParam) : NaN;

    if (!Number.isFinite(locationId)) {
      this.isLoading = false;
      this.errorMessage = 'Identifiant du lieu invalide.';
      return;
    }

    this.locationService.getLocationById(locationId).subscribe({
      next: (location) => {
        this.location = location;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les details du lieu.';
        this.isLoading = false;
      }
    });
  }

  getTypeLabel(type: LocationType): string {
    return LocationTypeLabels[type] || type;
  }

  getTypeIcon(type: LocationType): string {
    return LocationTypeIcons[type] || '📍';
  }

  getTypeColor(type: LocationType): string {
    return LocationTypeColors[type] || '#6366f1';
  }

  backToList(): void {
    this.router.navigate(['/admin/locations']);
  }
}
