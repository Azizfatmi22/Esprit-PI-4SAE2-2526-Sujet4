// src/app/services/planning.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { Planning } from '../models/planning';

@Injectable({
  providedIn: 'root'
})
export class PlanningService {
  private apiUrl = 'http://localhost:8085/api/plannings';

  constructor(private http: HttpClient) { }

   // ==================== BASIC CRUD OPERATIONS ====================

  /**
   * POST /api/plannings
   * Create a new planning
   */
  createPlanning(planning: Planning, sessionId: number, locationId?: number): Observable<Planning> {
    let params = new HttpParams()
      .set('sessionId', sessionId.toString());
    
    if (locationId) {
      params = params.set('locationId', locationId.toString());
    }

    return this.http.post<Planning>(this.apiUrl, planning, { params });
  }

  /**
 * GET /api/plannings/by-session/{sessionId}
 * Get planning for a specific session (returns array)
 */
getPlanningBySessionId(sessionId: number): Observable<Planning[]> {
  return this.http.get<Planning[]>(`${this.apiUrl}/by-session/${sessionId}`);
}

  /**
   * GET /api/plannings/session/{sessionId}
   * Get all plannings for a specific session
   */
  getPlanningsBySession(sessionId: number): Observable<Planning[]> {
    return this.http.get<Planning[]>(`${this.apiUrl}/session/${sessionId}`);
  }

  /**
   * GET /api/plannings/{id}
   * Get a planning by its ID
   */
  getPlanningById(id: number): Observable<Planning> {
    return this.http.get<Planning>(`${this.apiUrl}/${id}`);
  }

  /**
   * PUT /api/plannings/{id}
   * Update an existing planning
   */
  updatePlanning(id: number, planning: Planning): Observable<Planning> {
    return this.http.put<Planning>(`${this.apiUrl}/${id}`, planning);
  }

  /**
   * DELETE /api/plannings/{id}
   * Delete a planning
   */
  deletePlanning(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ==================== EXISTING ADVANCED FUNCTIONS ====================

  /**
   * POST /api/plannings/generate
   * Generate planning automatically
   */
  // ==================== EXISTING ADVANCED FUNCTIONS ====================

/**
 * POST /api/plannings/generate
 * Generate planning automatically
 * @param sessionId The session ID
 * @param mode Optional planning mode (ONSITE or REMOTE)
 */
generatePlanning(sessionId: number, mode?: string): Observable<Planning> {
  let params = new HttpParams()
    .set('sessionId', sessionId.toString());
  
  // Add mode to params if provided
  if (mode) {
    params = params.set('mode', mode);
  }
  
  return this.http.post<Planning>(`${this.apiUrl}/generate`, null, { params });
}

/**
 * Overloaded method for backward compatibility
 * Generate planning automatically with default mode
 */
generatePlanningDefault(sessionId: number): Observable<Planning> {
  return this.generatePlanning(sessionId);
  }

  /**
   * POST /api/plannings/distribute
   * Distribute planning across days
   */
  distributePlanning(sessionId: number, locationId: number, numberOfDays: number): Observable<Planning> {
    const params = new HttpParams()
      .set('sessionId', sessionId.toString())
      .set('locationId', locationId.toString())
      .set('numberOfDays', numberOfDays.toString());
    
    return this.http.post<Planning>(`${this.apiUrl}/distribute`, null, { params });
  }

  /**
   * GET /api/plannings/conflict
   * Check if there's a scheduling conflict
   */
  checkConflict(locationId: number, startDate: string, endDate: string): Observable<boolean> {
    const params = new HttpParams()
      .set('locationId', locationId.toString())
      .set('startDate', startDate)
      .set('endDate', endDate);
    
    return this.http.get<boolean>(`${this.apiUrl}/conflict`, { params });
  }

  /**
   * GET /api/plannings/suggest-date
   * Suggest next available date
   */
  suggestNextAvailableDate(locationId: number, startDate: string): Observable<string> {
    const params = new HttpParams()
      .set('locationId', locationId.toString())
      .set('startDate', startDate);
    
    return this.http.get<string>(`${this.apiUrl}/suggest-date`, { params });
  }

  /**
   * POST /api/plannings/optimize
   * Optimize planning for a session
   */
  optimizePlanning(sessionId: number): Observable<Planning> {
    const params = new HttpParams().set('sessionId', sessionId.toString());
    
    return this.http.post<Planning>(`${this.apiUrl}/optimize`, null, { params });
  }

  /**
   * GET /api/plannings/count-by-location
   * Count plannings by location
   */
  countPlanningsByLocation(locationId: number): Observable<number> {
    const params = new HttpParams().set('locationId', locationId.toString());
    
    return this.http.get<number>(`${this.apiUrl}/count-by-location`, { params });
  }

  // ==================== NEW ADVANCED FUNCTIONS (Using existing types) ====================

  /**
   * POST /api/plannings/fill-gaps
   * Fill gaps between sessions - Returns Planning array
   */
  fillGaps(sessionId: number, locationId: number): Observable<Planning> {
    const params = new HttpParams()
      .set('sessionId', sessionId.toString())
      .set('locationId', locationId.toString());
    
    return this.http.post<Planning>(`${this.apiUrl}/fill-gaps`, null, { params });
  }

  /**
   * POST /api/plannings/rolling
   * Maintain rolling planning - Returns Planning array
   */
  maintainRollingPlanning(sessionId: number, locationId: number, daysAhead: number): Observable<Planning> {
    const params = new HttpParams()
      .set('sessionId', sessionId.toString())
      .set('locationId', locationId.toString())
      .set('daysAhead', daysAhead.toString());
    
    return this.http.post<Planning>(`${this.apiUrl}/rolling`, null, { params });
  }

  /**
   * GET /api/plannings/best-location
   * Suggest best location (least busy) - Returns any (location object)
   */
  suggestBestLocation(date: string): Observable<any> {
    const params = new HttpParams().set('date', date);
    
    return this.http.get<any>(`${this.apiUrl}/best-location`, { params });
  }

  /**
   * GET /api/plannings/busy-days
   * Get busy days analytics - Returns any (array of busy day objects)
   */
  getBusyDays(locationId: number): Observable<any> {
  const params = new HttpParams().set('locationId', locationId.toString());
  return this.http.get<any>(`${this.apiUrl}/busy-days`, { params }).pipe(
    map(response => {
      // Normaliser la réponse pour toujours retourner un tableau
      if (response && typeof response === 'object') {
        if (response.days && Array.isArray(response.days)) {
          return response.days;
        } else if (Array.isArray(response)) {
          return response;
        } else {
          // Chercher un tableau dans l'objet
          const arrayProp = Object.values(response).find(val => Array.isArray(val));
          return arrayProp || [];
        }
      }
      return Array.isArray(response) ? response : [];
    })
  );
}



  /**
   * GET /api/plannings/high-risk
   * Detect risky planning - Returns any (risk assessment object)
   */
  isHighRisk(sessionId: number): Observable<any> {
    const params = new HttpParams().set('sessionId', sessionId.toString());
    
    return this.http.get<any>(`${this.apiUrl}/high-risk`, { params });
  }

  /**
   * GET /api/plannings/smart-date
   * Smart date suggestion (skip weekends + conflicts) - Returns string date
   */
  smartSuggestDate(locationId: number, startDate: string): Observable<string> {
    const params = new HttpParams()
      .set('locationId', locationId.toString())
      .set('startDate', startDate);
    
    return this.http.get<string>(`${this.apiUrl}/smart-date`, { params });
  }
}