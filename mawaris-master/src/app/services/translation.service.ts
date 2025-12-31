import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TranslationService {

  constructor() { }

  // Basic implementation
  translate(key: string): string {
    // Mock translation
    return key;
  }

  getTranslations() {
    return {};
  }
}
