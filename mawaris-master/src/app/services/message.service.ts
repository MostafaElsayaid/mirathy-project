import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ContactMessage {
  id: number;
  name: string;
  email: string;
  message: string;
  date: string;
}

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private apiUrl = '/api/messages'; // Adjust to your backend URL

  constructor(private http: HttpClient) {}

  getMessages(): Observable<ContactMessage[]> {
    return this.http.get<ContactMessage[]>(this.apiUrl);
  }
}
