import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './contact.html',
  styleUrls: ['./contact.css']
})
export class ContactComponent implements OnInit {
  contactForm: FormGroup;
  isSubmitted = false;
  isLoading = false;
  isLoggedIn = false;
  userFullName: string = '';
  
  // استخدام inject بدلاً من constructor injection
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  constructor() {
    this.contactForm = this.fb.group({});
  }

  ngOnInit() {
    // الحصول على حالة تسجيل الدخول من AuthService
    this.isLoggedIn = this.authService.getIsLoggedIn();
    
    if (this.isLoggedIn) {
      this.contactForm = this.fb.group({
        message: ['', [Validators.required, Validators.minLength(10)]],
      });
      
      // الحصول على اسم المستخدم مباشرة من AuthService
      // هذه هي الطريقة الصحيحة بدلاً من محاولة فك التوكن
      this.userFullName = this.authService.getUserFullName() || 'مستخدم';
    } else {
      this.contactForm = this.fb.group({
        name: ['', [Validators.required, Validators.minLength(3)]],
        email: ['', [Validators.required, Validators.email]],
        message: ['', [Validators.required, Validators.minLength(10)]],
      });
    }
  }

  get f() {
    return this.contactForm.controls;
  }

  onSubmit() {
    this.isSubmitted = true;

    if (this.contactForm.invalid) {
      return;
    }

    this.isLoading = true;

    // تحضير البيانات للإرسال
    const formData = this.isLoggedIn 
      ? {
          isLoggedIn: true,
          userId: this.getUserIdFromToken(),
          userFullName: this.userFullName, // إضافة اسم المستخدم
          message: this.contactForm.value.message,
          timestamp: new Date().toISOString()
        }
      : {
          isLoggedIn: false,
          name: this.contactForm.value.name,
          email: this.contactForm.value.email,
          message: this.contactForm.value.message,
          timestamp: new Date().toISOString()
        };

    console.log('بيانات الاتصال المرسلة:', formData);

    setTimeout(() => {
      console.log('تم إرسال رسالتك بنجاح ✅');
      
      alert('تم إرسال رسالتك بنجاح ✅');
      
      this.contactForm.reset();
      this.isSubmitted = false;
      this.isLoading = false;
    }, 1500);
  }

  private getUserIdFromToken(): string | null {
    const token = localStorage.getItem('token');
    if (!token) return null;
    
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.userId || payload.sub || null;
    } catch (e) {
      return null;
    }
  }
}