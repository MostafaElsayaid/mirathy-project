import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { UserService, User } from '../../services/user.service';
import { MessageService, ContactMessage } from '../../services/message.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatSidenavModule,
    MatCardModule,
    MatListModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private userService = inject(UserService);
  private messageService = inject(MessageService);
  private authService = inject(AuthService);
  private router = inject(Router);

  users: User[] = [];
  contactMessages: ContactMessage[] = [];
  selectedUser: User | null = null;
  sidePaneOpen = false;

  userColumns: string[] = ['id', 'name', 'email', 'role', 'isActive', 'createdAt', 'actions'];
  messageColumns: string[] = ['id', 'name', 'email', 'message', 'date'];

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    this.userService.getUsers().subscribe({
      next: (users) => this.users = users,
      error: (error) => console.error('Error loading users:', error)
    });

    this.messageService.getMessages().subscribe({
      next: (messages) => this.contactMessages = messages,
      error: (error) => console.error('Error loading messages:', error)
    });
  }

  selectUser(user: User): void {
    this.selectedUser = user;
    this.sidePaneOpen = true;
  }

  closeSidePane(): void {
    this.sidePaneOpen = false;
    this.selectedUser = null;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
