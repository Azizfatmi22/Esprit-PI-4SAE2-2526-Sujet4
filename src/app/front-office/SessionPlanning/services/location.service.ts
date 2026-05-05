// src/app/services/location.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Location, LocationType } from  '../models/location';

@Injectable({
  providedIn: 'root'
})
export class LocationService {
  private apiUrl = 'http://localhost:8085/api/locations';

  constructor(private http: HttpClient) { }

  // POST /api/locations
  createLocation(location: Location): Observable<Location> {
    return this.http.post<Location>(this.apiUrl, location);
  }

  // PUT /api/locations/{id}
  updateLocation(id: number, location: Location): Observable<Location> {
    return this.http.put<Location>(`${this.apiUrl}/${id}`, location);
  }

  // DELETE /api/locations/{id}
  deleteLocation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // GET /api/locations/{id}
  getLocationById(id: number): Observable<Location> {
    return this.http.get<Location>(`${this.apiUrl}/${id}`);
  }

  // GET /api/locations
  getAllLocations(): Observable<Location[]> {
    return this.http.get<Location[]>(this.apiUrl);
  }

  // GET /api/locations/type/{type}
  getLocationsByType(type: LocationType): Observable<Location[]> {
    return this.http.get<Location[]>(`${this.apiUrl}/type/${type}`);
  }
  // ==================== VALIDATION ====================

  /**
   * POST /api/locations/validate
   * Validate a location
   */
  validateLocation(location: Location): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/validate`, location);
  }

  // ==================== ONLINE LOCATIONS ====================

  /**
   * GET /api/locations/online
   * Get all online locations
   */
  getOnlineLocations(): Observable<Location[]> {
    return this.http.get<Location[]>(`${this.apiUrl}/online`);
  }

  // ==================== SEARCH ====================

  /**
   * GET /api/locations/search
   * Search locations by keyword
   */
  searchLocations(keyword: string): Observable<Location[]> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<Location[]>(`${this.apiUrl}/search`, { params });
  }

  // ==================== OVERLOADED LOCATIONS ====================

  /**
   * GET /api/locations/overloaded
   * Find overloaded locations above threshold
   */
  findOverloadedLocations(threshold: number): Observable<Location[]> {
    const params = new HttpParams().set('threshold', threshold.toString());
    return this.http.get<Location[]>(`${this.apiUrl}/overloaded`, { params });
  }

  // ==================== LOCATION SUGGESTION ====================

  /**
   * GET /api/locations/suggest
   * Suggest best location based on capacity and type
   */
  suggestBestLocation(capacity: number, type: LocationType): Observable<Location> {
    const params = new HttpParams()
      .set('capacity', capacity.toString())
      .set('type', type.toString());
    return this.http.get<Location>(`${this.apiUrl}/suggest`, { params });
  }

  // ==================== LEAST USED LOCATION ====================

  /**
   * GET /api/locations/least-used
   * Get the least used location
   */
  getLeastUsedLocation(): Observable<Location> {
    return this.http.get<Location>(`${this.apiUrl}/least-used`);
  }

  // ==================== AVAILABLE LOCATIONS ====================

  /**
   * GET /api/locations/available
   * Get locations available for given capacity
   */
  getAvailableLocations(capacity: number): Observable<Location[]> {
    const params = new HttpParams().set('capacity', capacity.toString());
    return this.http.get<Location[]>(`${this.apiUrl}/available`, { params });
  }

  // ==================== CONVENIENCE METHODS ====================

  /**
   * Search online locations by keyword
   */
  searchOnlineLocations(keyword: string): Observable<Location[]> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<Location[]>(`${this.apiUrl}/online/search`, { params });
  }

  /**
   * Check if location name already exists
   */
  checkLocationNameExists(name: string): Observable<boolean> {
    const params = new HttpParams().set('name', name);
    return this.http.get<boolean>(`${this.apiUrl}/check-name`, { params });
  }

  /**
   * Get location statistics
   */
  getLocationStats(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/stats`);
  }

  /**
   * Get locations by city
   */
  getLocationsByCity(city: string): Observable<Location[]> {
    const params = new HttpParams().set('city', city);
    return this.http.get<Location[]>(`${this.apiUrl}/city`, { params });
  }

  /**
   * Get locations with capacity greater than minimum
   */
  getLocationsWithMinCapacity(minCapacity: number): Observable<Location[]> {
    const params = new HttpParams().set('minCapacity', minCapacity.toString());
    return this.http.get<Location[]>(`${this.apiUrl}/capacity/min`, { params });
  }

  /**
   * Get locations with capacity less than maximum
   */
  getLocationsWithMaxCapacity(maxCapacity: number): Observable<Location[]> {
    const params = new HttpParams().set('maxCapacity', maxCapacity.toString());
    return this.http.get<Location[]>(`${this.apiUrl}/capacity/max`, { params });
  }

  /**
   * Get locations by capacity range
   */
  getLocationsByCapacityRange(min: number, max: number): Observable<Location[]> {
    const params = new HttpParams()
      .set('min', min.toString())
      .set('max', max.toString());
    return this.http.get<Location[]>(`${this.apiUrl}/capacity/range`, { params });
  }

  /**
   * Get location usage history
   */
  getLocationUsageHistory(locationId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${locationId}/usage`);
  }

  /**
   * Get location availability for a specific date
   */
  checkLocationAvailability(locationId: number, date: string): Observable<boolean> {
    const params = new HttpParams()
      .set('locationId', locationId.toString())
      .set('date', date);
    return this.http.get<boolean>(`${this.apiUrl}/availability`, { params });
  }

  /**
   * Get similar locations (same type, nearby capacity)
   */
  getSimilarLocations(locationId: number): Observable<Location[]> {
    return this.http.get<Location[]>(`${this.apiUrl}/${locationId}/similar`);
  }

  /**
   * Get location recommendations based on session type
   */
  getLocationRecommendations(sessionType: string, requiredCapacity: number): Observable<Location[]> {
    const params = new HttpParams()
      .set('sessionType', sessionType)
      .set('requiredCapacity', requiredCapacity.toString());
    return this.http.get<Location[]>(`${this.apiUrl}/recommendations`, { params });
  }

  /**
   * Get popular locations (most used)
   */
  getPopularLocations(limit: number = 5): Observable<Location[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<Location[]>(`${this.apiUrl}/popular`, { params });
  }

  /**
   * Get recently added locations
   */
  getRecentLocations(limit: number = 5): Observable<Location[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<Location[]>(`${this.apiUrl}/recent`, { params });
  }

  /**
   * Bulk create locations
   */
  bulkCreateLocations(locations: Location[]): Observable<Location[]> {
    return this.http.post<Location[]>(`${this.apiUrl}/bulk`, locations);
  }

  /**
   * Export locations to CSV/Excel
   */
  exportLocations(format: 'csv' | 'excel'): Observable<Blob> {
    const params = new HttpParams().set('format', format);
    return this.http.get(`${this.apiUrl}/export`, { 
      params, 
      responseType: 'blob' 
    });
  }

  /**
   * Get location types with counts
   */
  getLocationTypeStats(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/stats/types`);
  }

  /**
   * Get cities with location counts
   */
  getCityStats(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/stats/cities`);
  }

  /**
   * Get total capacity across all locations
   */
  getTotalCapacity(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/stats/total-capacity`);
  }

  /**
   * Get average capacity across all locations
   */
  getAverageCapacity(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/stats/average-capacity`);
  }
}
