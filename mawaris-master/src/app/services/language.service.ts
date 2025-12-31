import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LanguageService {

  constructor() { }

  // Basic implementation
  getCurrentLanguage(): string {
    return 'ar';
  }

  setLanguage(lang: string) {
    // Mock set
    console.log('Setting language:', lang);
  }

  get isRtl(): boolean {
    return this.getCurrentLanguage() === 'ar';
  }
}
