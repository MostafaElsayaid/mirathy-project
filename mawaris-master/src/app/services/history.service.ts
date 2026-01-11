import { Injectable } from '@angular/core';
import { HeirResult } from './models';
import { Observable, of } from 'rxjs';
import {HttpClient, HttpHeaders} from '@angular/common/http';

export interface SavedCase {
  id: number;
  date: string;
  estate: number;
  results: HeirResult[];
  heirs: string;
  isFavorite: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class HistoryService {

  private readonly baseUrl =
    'http://localhost:8087/api/v1/api/v1/auth';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');

    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }

  getAllProblems(): Observable<HistoryProblem[]> {
    return this.http.get<HistoryProblem[]>(
      `${this.baseUrl}/getAllProblem`,
      { headers: this.getAuthHeaders() }
    );
  }

  getFavoriteProblems(): Observable<HistoryProblem[]> {
    return this.http.get<HistoryProblem[]>(
      `${this.baseUrl}/getAllFavoriteProblem`,
      { headers: this.getAuthHeaders() }
    );
  }

  toggleFavorite(problemId: number): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/isFavorite/${problemId}`,
      {},
      { headers: this.getAuthHeaders() }
    );
  }

  getProblemDetails(id: number): Observable<ProblemResult[]> {
    return this.http.get<ProblemResult[]>(
      `${this.baseUrl}/problem/${id}`,
      { headers: this.getAuthHeaders() }
    );
  }

  deleteProblem(problemId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/problem/${problemId}`,
      { headers: this.getAuthHeaders() }
    );
  }
}
export interface HistoryProblem {
  id: number;
  title: string;
  createdAt: string;
  isFavorite: boolean;
}
export interface ProblemResult {
  heirType: string;
  shareType: string;
  fixedShare: string;
  shareValue: number;
  memberCount: number;
  reason: string;
}
export interface HistoryProblemDetails {
  id: number;
  title: string;
  createdAt: string;
  isFavorite: boolean;
  estate?: number;      // optional لو مش راجع
  heirs?: string;       // optional
  results: ProblemResult[];
}


