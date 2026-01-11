// import { Component, inject } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { RouterModule } from '@angular/router';
// import { AuthService } from '../../services/auth.service';

// @Component({
//   selector: 'app-navbar',
//   standalone: true,
//   imports: [CommonModule, RouterModule],
//   templateUrl: './navbar.html',
//   styleUrl: './navbar.css'
// })
// export class Navbar {
//   authService = inject(AuthService);

//   logout() {
//     this.authService.logout();
//   }
// }

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css'],
})
export class Navbar {
  constructor(private router:Router){}
  authService = inject(AuthService);
  menuOpen = false; // للتحكم في قائمة الموبايل

  logout() {
    this.authService.logout();
    this.menuOpen = false;
    //localStorage.clear()
    this.router.navigate(['/home']); // إعادة التوجيه إلى الصفحة الرئيسية بعد تسجيل الخروج
  }
}
