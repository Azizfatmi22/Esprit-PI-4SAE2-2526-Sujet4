// src/app/components/location-list/location-list.component.ts

import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { LocationService } from '../../../../front-office/SessionPlanning/services/location.service';
import { Location, LocationType, LocationTypeLabels, LocationTypeIcons, LocationTypeColors } from '../../../../front-office/SessionPlanning/models/location';

@Component({
  selector: 'app-location-list',
  templateUrl: './location-list.component.html',
  styleUrls: ['./location-list.component.scss']
})
export class LocationListComponent implements OnInit {
  @ViewChild('searchInput') searchInput!: ElementRef;
  
  locations: Location[] = [];
  filteredLocations: Location[] = [];
  
  // Filtres
  searchText: string = '';
  selectedType: string = '';
  
  // Exposer l'enum au template
  LocationType = LocationType;
  
  // Types de lieux pour les filtres
  locationTypes = [
  { value: LocationType.ROOM, label: 'Salle', icon: '📍' },
  { value: LocationType.ONLINE_PLATFORM, label: 'Plateforme en ligne', icon: '🌐' },
  { value: LocationType.HYBRID, label: 'Hybride', icon: '🔄' }
];
  
  // Message
  message: { show: boolean; type: 'success' | 'error'; text: string } = {
    show: false,
    type: 'success',
    text: ''
  };

  constructor(
    private locationService: LocationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadLocations();
  }

  loadLocations(): void {
    this.locationService.getAllLocations().subscribe({
      next: (data) => {
        this.locations = data;
        this.applyFilters();
      },
      error: (error) => {
        this.showMessage('Erreur lors du chargement des lieux', 'error');
        console.error('Error loading locations:', error);
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.locations];

    // Filtre par recherche textuelle
    if (this.searchText) {
      const searchLower = this.searchText.toLowerCase();
      filtered = filtered.filter(location => 
        location.name.toLowerCase().includes(searchLower) ||
        location.address.toLowerCase().includes(searchLower) ||
        location.city.toLowerCase().includes(searchLower) ||
        location.capacity.toString().includes(searchLower)
      );
    }

    // Filtre par type - convertir le string en LocationType
    if (this.selectedType) {
      filtered = filtered.filter(location => location.type === this.selectedType as LocationType);
    }

    // Tri par nom
    filtered.sort((a, b) => a.name.localeCompare(b.name));

    this.filteredLocations = filtered;
  }

  clearSearch(): void {
    this.searchText = '';
    this.applyFilters();
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedType = '';
    this.applyFilters();
    setTimeout(() => this.searchInput?.nativeElement.focus(), 100);
  }

  getCountByType(type: LocationType): number {
    return this.locations.filter(l => l.type === type).length;
  }

  // Méthode helper pour convertir string en LocationType dans le template
  getTypeFromString(type: string): LocationType {
    return type as LocationType;
  }

  getTypeIcon(type: LocationType): string {
    return LocationTypeIcons[type] || '📍';
  }

  getTypeLabel(type: LocationType): string {
    return LocationTypeLabels[type] || type;
  }

  getTypeColor(type: LocationType): string {
    return LocationTypeColors[type] || '#6366f1';
  }

  deleteLocation(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce lieu ?')) {
      this.locationService.deleteLocation(id).subscribe({
        next: () => {
          this.showMessage('Lieu supprimé avec succès', 'success');
          this.loadLocations();
        },
        error: (error) => {
          this.showMessage('Erreur lors de la suppression', 'error');
          console.error('Error deleting location:', error);
        }
      });
    }
  }

  showMessage(text: string, type: 'success' | 'error'): void {
    this.message = { show: true, type, text };
    
    if (type === 'success') {
      setTimeout(() => {
        this.message.show = false;
      }, 3000);
    }
  }
}