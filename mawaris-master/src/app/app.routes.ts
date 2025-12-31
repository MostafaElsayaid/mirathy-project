import { Routes } from '@angular/router';

import {HomeComponent} from './component/home/home';

import { About } from './component/about/about';
import { Contact } from './component/contact/contact';
import { Login } from './component/login/login';
import { Register } from './component/register/register';
import { HistoryComponent } from './component/history/history';
import { Dashboard } from './component/dashboard/dashboard';
import { authGuard } from './guards/auth.guard';
import { NotFoundPage } from './component/not-found-page/not-found-page';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent   },
  { path: 'about', component: About },
  { path: 'contact', component: Contact },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'history', component: HistoryComponent, canActivate: [authGuard] },
  { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
  { path: '**', component: NotFoundPage },
  { path: '**', redirectTo: '/home' }
];
