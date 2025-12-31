import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private isLoggedInSignal = signal(false);
  isLoggedIn$ = this.isLoggedInSignal.asReadonly();

  
  private BASE_URL = 'http://localhost:8087/api/v1/auth';

  constructor(private http: HttpClient) {
    this.checkStoredLogin();
  }

  login(email: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.BASE_URL}/login`, { email, password })
      .pipe(
        tap(res => {
          localStorage.setItem('token', res.token); 
          this.isLoggedInSignal.set(true);
        })
      );
  }

  register(fullName: string, email: string, password: string): Observable<any> {
    return this.http.post(`${this.BASE_URL}/register`, { fullName, email, password });
  }

  logout(): void {
    localStorage.removeItem('token');
    this.isLoggedInSignal.set(false);
  }

  private checkStoredLogin(): void {
    const token = localStorage.getItem('token');
    if (token) this.isLoggedInSignal.set(true);
  }

  getIsLoggedIn(): boolean {
    return this.isLoggedInSignal();
  }
}
