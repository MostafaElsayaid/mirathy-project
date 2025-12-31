import { Injectable } from '@angular/core';
import { HeirResult } from './models';

interface PendingCalculation {
  formValue: any;
  result: HeirResult[];
  action: 'save' | 'favorite';
}

@Injectable({
  providedIn: 'root'
})
export class PendingCalculationService {
  private pendingCalculation: PendingCalculation | null = null;

  constructor() { }

  setPendingCalculation(formValue: any, result: HeirResult[], action: 'save' | 'favorite'): void {
    this.pendingCalculation = { formValue, result, action };
  }

  getAndConsumePendingCalculation(): PendingCalculation | null {
    const calc = this.pendingCalculation;
    this.pendingCalculation = null;
    return calc;
  }
}
