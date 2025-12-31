import { Injectable } from '@angular/core';

export interface User {
  id: number;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN';
  isActive: boolean;
  createdAt: string;
}

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
export class MockDataService {

  private users: User[] = [
    {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      role: 'USER',
      isActive: true,
      createdAt: '2023-01-15T10:30:00Z'
    },
    {
      id: 2,
      name: 'Jane Smith',
      email: 'jane@example.com',
      role: 'USER',
      isActive: true,
      createdAt: '2023-02-20T14:45:00Z'
    },
    {
      id: 3,
      name: 'Bob Johnson',
      email: 'bob@example.com',
      role: 'ADMIN',
      isActive: false,
      createdAt: '2023-03-10T09:15:00Z'
    },
    {
      id: 4,
      name: 'Alice Brown',
      email: 'alice@example.com',
      role: 'USER',
      isActive: true,
      createdAt: '2023-04-05T16:20:00Z'
    }
  ];

  private contactMessages: ContactMessage[] = [
    {
      id: 1,
      name: 'John Doe',
      email: 'john@example.com',
      message: 'I have a question about your services.',
      date: '2023-05-01T11:00:00Z'
    },
    {
      id: 2,
      name: 'Jane Smith',
      email: 'jane@example.com',
      message: 'Great website! Keep up the good work.',
      date: '2023-05-02T13:30:00Z'
    },
    {
      id: 3,
      name: 'Bob Johnson',
      email: 'bob@example.com',
      message: 'I need help with my account.',
      date: '2023-05-03T15:45:00Z'
    },
    {
      id: 4,
      name: 'Alice Brown',
      email: 'alice@example.com',
      message: 'Thank you for the quick response!',
      date: '2023-05-04T17:20:00Z'
    }
  ];

  getUsers(): User[] {
    return this.users;
  }

  getContactMessages(): ContactMessage[] {
    return this.contactMessages;
  }
}
