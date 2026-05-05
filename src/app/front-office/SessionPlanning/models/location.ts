// src/app/features/sessions/models/location.ts

export enum LocationType {
  ROOM = 'ROOM',
  ONLINE_PLATFORM = 'ONLINE_PLATFORM',
  HYBRID = 'HYBRID'
}

// Labels for display
export const LocationTypeLabels: Record<LocationType, string> = {
  [LocationType.ROOM]: 'Salle',
  [LocationType.ONLINE_PLATFORM]: 'Plateforme en ligne',
  [LocationType.HYBRID]: 'Hybride'
};

// Icons for display
export const LocationTypeIcons: Record<LocationType, string> = {
  [LocationType.ROOM]: '📍',
  [LocationType.ONLINE_PLATFORM]: '🌐',
  [LocationType.HYBRID]: '🔄'
};

// Colors for display
export const LocationTypeColors: Record<LocationType, string> = {
  [LocationType.ROOM]: '#6366f1',
  [LocationType.ONLINE_PLATFORM]: '#10b981',
  [LocationType.HYBRID]: '#f59e0b'
};

export interface Location {
  id?: number;
  name: string;
  address: string;
  city: string;
  capacity: number;
  type: LocationType;
  platformUrl?: string; // URL de la plateforme pour les formations en ligne
}