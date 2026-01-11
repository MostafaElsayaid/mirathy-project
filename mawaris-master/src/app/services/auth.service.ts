// import { Injectable, signal } from '@angular/core';
// import { HttpClient } from '@angular/common/http';
// import { Observable, tap } from 'rxjs';

// interface AuthResponse {
//   accessToken: string;
//   fullName?: string;
//   user?: any;
// }

// @Injectable({
//   providedIn: 'root'
// })
// export class AuthService {
//   private isLoggedInSignal = signal(false);
//   private userFullNameSignal = signal<string | null>(null);
  
//   isLoggedIn$ = this.isLoggedInSignal.asReadonly();
//   userFullName$ = this.userFullNameSignal.asReadonly();

//   private BASE_URL = 'http://localhost:8087/api/v1/auth';

//   constructor(private http: HttpClient) {
//     this.checkStoredLogin();
//   }

//   login(email: string, password: string): Observable<AuthResponse> {
//     return this.http.post<AuthResponse>(`${this.BASE_URL}/login`, { email, password })
//       .pipe(
//         tap(res => {
//           localStorage.setItem('token', res.accessToken);
          
//           // تخزين بيانات المستخدم إذا كانت موجودة في الرد
//           if (res.fullName) {
//             localStorage.setItem('userFullName', res.fullName);
//             this.userFullNameSignal.set(res.fullName);
//           }
          
//           // تخزين بيانات أخرى من الرد
//           if (res.user) {
//             localStorage.setItem('userData', JSON.stringify(res.user));
//           }
          
//           this.isLoggedInSignal.set(true);
//         })
//       );
//   }

//   register(fullName: string, email: string, password: string): Observable<AuthResponse> {
//     return this.http.post<AuthResponse>(`${this.BASE_URL}/register`, { fullName, email, password })
//       .pipe(
//         tap(res => {
//           if (res.accessToken) {
//             localStorage.setItem('token', res.accessToken);
//             localStorage.setItem('userFullName', fullName);
//             this.userFullNameSignal.set(fullName);
//             this.isLoggedInSignal.set(true);
//           }
//         })
//       );
//   }

//   logout(): void {
//     localStorage.removeItem('token');
//     localStorage.removeItem('userFullName');
//     localStorage.removeItem('userData');
//     this.isLoggedInSignal.set(false);
//     this.userFullNameSignal.set(null);
//   }

//   private checkStoredLogin(): void {
//     const token = localStorage.getItem('token');
//     if (token) {
//       this.isLoggedInSignal.set(true);
      
//       // محاولة استخراج اسم المستخدم من التوكن
//       try {
//         const payload = JSON.parse(atob(token.split('.')[1]));
//         const storedName = localStorage.getItem('userFullName') || payload.fullName || payload.name;
//         if (storedName) {
//           this.userFullNameSignal.set(storedName);
//         }
//       } catch (e) {
//         console.warn('Cannot parse token:', e);
//       }
//     }
//   }

//   getIsLoggedIn(): boolean {
//     return this.isLoggedInSignal();
//   }

//   getUserFullName(): string | null {
//     return this.userFullNameSignal();
//   }

//   getUserData(): any {
//     const userData = localStorage.getItem('userData');
//     return userData ? JSON.parse(userData) : null;
//   }
// }
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

interface AuthResponse {
  accessToken: string;
  fullName?: string;
  user?: any;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private isLoggedInSignal = signal(false);
  private userFullNameSignal = signal<string | null>(null);
  private userDataSignal = signal<any | null>(null);
  
  isLoggedIn$ = this.isLoggedInSignal.asReadonly();
  userFullName$ = this.userFullNameSignal.asReadonly();
  userData$ = this.userDataSignal.asReadonly();

  private BASE_URL = 'http://localhost:8087/api/v1/auth';

  constructor(private http: HttpClient) {
    this.checkStoredLogin();
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.BASE_URL}/login`, { email, password })
      .pipe(
        tap(res => {
          localStorage.setItem('token', res.accessToken);
          
          // تخزين بيانات المستخدم
          if (res.fullName) {
            localStorage.setItem('userFullName', res.fullName);
            this.userFullNameSignal.set(res.fullName);
          }
          
          // تخزين بيانات المستخدم الكاملة
          if (res.user) {
            localStorage.setItem('userData', JSON.stringify(res.user));
            this.userDataSignal.set(res.user);
          }
          
          this.isLoggedInSignal.set(true);
        })
      );
  }

  register(fullName: string, email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.BASE_URL}/register`, { fullName, email, password })
      .pipe(
        tap(res => {
          if (res.accessToken) {
            localStorage.setItem('token', res.accessToken);
            
            // تخزين اسم المستخدم من التسجيل مباشرة
            localStorage.setItem('userFullName', fullName);
            this.userFullNameSignal.set(fullName);
            
            // تخزين بيانات المستخدم إذا كانت موجودة في الرد
            if (res.user) {
              localStorage.setItem('userData', JSON.stringify(res.user));
              this.userDataSignal.set(res.user);
            }
            
            this.isLoggedInSignal.set(true);
          }
        })
      );
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userFullName');
    localStorage.removeItem('userData');
    this.isLoggedInSignal.set(false);
    this.userFullNameSignal.set(null);
    this.userDataSignal.set(null);
  }

  private checkStoredLogin(): void {
    const token = localStorage.getItem('token');
    if (token) {
      this.isLoggedInSignal.set(true);
      
      // استعادة اسم المستخدم من localStorage مباشرة
      const storedName = localStorage.getItem('userFullName');
      if (storedName) {
        this.userFullNameSignal.set(storedName);
      }
      
      // استعادة بيانات المستخدم من localStorage
      const userData = localStorage.getItem('userData');
      if (userData) {
        try {
          this.userDataSignal.set(JSON.parse(userData));
        } catch (e) {
          console.warn('Cannot parse user data:', e);
        }
      }
    }
  }

  getIsLoggedIn(): boolean {
    return this.isLoggedInSignal();
  }

  getUserFullName(): string | null {
    return this.userFullNameSignal();
  }

  getUserData(): any {
    return this.userDataSignal();
  }
}