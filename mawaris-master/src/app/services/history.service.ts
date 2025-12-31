import { Injectable } from '@angular/core';
import { HeirResult } from './models';
import { Observable, of } from 'rxjs';

export interface SavedCase {
  id: number;
  date: string;
  estate: number;
  results: HeirResult[];
  heirs: string;  // تغيير من heirsSummary إلى heirs لتتوافق مع HTML
  isFavorite: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class HistoryService {
  
  addCase(results: HeirResult[], estate: number, heirs: string, isFavorite: boolean) {
    // تحويل النتائج إلى تفاصيل لتتوافق مع الواجهة
    const details = results.map(result => ({
      heir: result.heir,
      count: result.count,
      share: result.share,
      amount: result.amount
    }));

    const savedCase: SavedCase = {
      id: Date.now(),
      date: new Date().toISOString(),
      estate: estate,
      results: results,
      heirs: heirs,  // استخدام heirs بدلاً من heirsSummary
      isFavorite: isFavorite
    };

    // حفظ في localStorage
    if (localStorage) {
      const savedCases = JSON.parse(localStorage.getItem('inheritanceCases') || '[]');
      savedCases.push(savedCase);
      localStorage.setItem('inheritanceCases', JSON.stringify(savedCases));
    }

    console.log('Case saved:', savedCase);
    return of(savedCase);
  }

  getCases(): SavedCase[] {
    if (localStorage) {
      const cases = JSON.parse(localStorage.getItem('inheritanceCases') || '[]');
      // تحويل التاريخ من نص إلى كائن تاريخ لعرضه بشكل صحيح
      return cases.map((c: SavedCase) => ({
        ...c,
        date: c.date // حافظ على النص كما هو
      }));
    }
    return [];
  }

  getSavedCases(): Observable<SavedCase[]> {
    const cases = this.getCases().filter(c => !c.isFavorite);
    return of(cases);
  }

  getFavoriteCases(): Observable<SavedCase[]> {
    const cases = this.getCases().filter(c => c.isFavorite);
    return of(cases);
  }

  updateCase(id: number, updates: Partial<SavedCase>) {
    if (localStorage) {
      const savedCases = JSON.parse(localStorage.getItem('inheritanceCases') || '[]');
      const index = savedCases.findIndex((c: SavedCase) => c.id === id);
      if (index !== -1) {
        savedCases[index] = { ...savedCases[index], ...updates };
        localStorage.setItem('inheritanceCases', JSON.stringify(savedCases));
      }
    }
    return of(null);
  }

  deleteCase(id: number) {
    if (localStorage) {
      const savedCases = JSON.parse(localStorage.getItem('inheritanceCases') || '[]');
      const filtered = savedCases.filter((c: SavedCase) => c.id !== id);
      localStorage.setItem('inheritanceCases', JSON.stringify(filtered));
    }
    return of(null);
  }

  toggleFavorite(id: number, isFavorite: boolean) {
    return this.updateCase(id, { isFavorite });
  }
}