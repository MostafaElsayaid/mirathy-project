
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './contact.html',
  styleUrl: './contact.css',

})

export class Contact {

  contactForm: FormGroup;
  isSubmitted = false;
  isLoading = false;

  constructor(private fb: FormBuilder) {
    this.contactForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      message: ['', [Validators.required, Validators.minLength(10)]],
    });
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

    
    // setTimeout(() => {
    //   console.log(this.contactForm.value);

    //   alert('تم إرسال رسالتك بنجاح ✅');

    //   this.contactForm.reset();
    //   this.isSubmitted = false;
    //   this.isLoading = false;
    // }, 1500);
  }
}
