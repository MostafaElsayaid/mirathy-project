import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import {HistoryProblem, HistoryProblemDetails, HistoryService, SavedCase} from '../../services/history.service';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './history.html',
  styleUrl: './history.css'
})
export class HistoryComponent implements OnInit {

  private historyService = inject(HistoryService);
  private languageService = inject(LanguageService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  activeTab = signal<'saved' | 'favorites'>('saved');

  savedCases = signal<HistoryProblem[]>([]);
  favoriteCases = signal<HistoryProblem[]>([]);

  isLoading = signal<boolean>(false);
  showDetailsModal = signal<boolean>(false);
  selectedCase = signal<HistoryProblemDetails | null>(null);
  isDetailsLoading = signal<boolean>(false);

  isRtl = this.languageService.isRtl;

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.activeTab.set(params['tab'] === 'favorites' ? 'favorites' : 'saved');
      this.loadCases();
    });
  }

  loadCases(): void {
    this.isLoading.set(true);

    // 📜 كل المسائل
    this.historyService.getAllProblems().subscribe({
      next: problems => {
        this.savedCases.set(problems);
        this.favoriteCases.set(problems.filter(p => p.isFavorite));
        this.isLoading.set(false);
      },
      error: err => {
        console.error('Error loading history:', err);
        this.isLoading.set(false);
      }
    });
  }

  setActiveTab(tab: 'saved' | 'favorites'): void {
    this.activeTab.set(tab);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab },
      queryParamsHandling: 'merge'
    });
  }

  toggleFavorite(problem: HistoryProblem): void {

    this.historyService.toggleFavorite(problem.id).subscribe({
      next: () => this.loadCases(),
      error: err => console.error('Error toggling favorite:', err)
    });
  }

  deleteCase(id: number): void {
    if (!confirm('هل أنت متأكد من حذف هذه المسألة؟')) return;

    this.historyService.deleteProblem(id).subscribe({
      next: () => {
        this.loadCases();
        if (this.selectedCase()?.id === id) {
          this.closeDetailsModal();
        }
      },
      error: err => console.error('Error deleting case:', err)
    });
  }

  viewDetails(problem: HistoryProblem) {
    this.isDetailsLoading.set(true);

    this.historyService.getProblemDetails(problem.id).subscribe({
      next: results => {
        this.selectedCase.set({
          ...problem,
          results
        });
        this.isDetailsLoading.set(false);
        this.showDetailsModal.set(true);
      },
      error: err => {
        console.error(err);
        this.isDetailsLoading.set(false);
      }
    });
  }

  closeDetailsModal(): void {
    this.showDetailsModal.set(false);
    this.selectedCase.set(null);
  }

  getHeirArabicText(heirType: string): string {
    const heirMap: { [key: string]: string } = {
      'HUSBAND': 'الزوج',
      'WIFE': 'الزوجة',
      'FATHER': 'الأب',
      'MOTHER': 'الأم',
      'GRANDFATHER': 'الجد لأب',
      'GRANDFATHER_MATERNAL': 'الجد لأم',
      'GRANDMOTHER_PATERNAL': 'الجدة لأب',
      'GRANDMOTHER_MATERNAL': 'الجدة لأم',
      'SON': 'الابن',
      'DAUGHTER': 'البنت',
      'SON_OF_SON': 'ابن الابن',
      'DAUGHTER_OF_SON': 'بنت الابن',
      'FULL_BROTHER': 'الأخ الشقيق',
      'FULL_SISTER': 'الأخت الشقيقة',
      'PATERNAL_BROTHER': 'الأخ لأب',
      'PATERNAL_SISTER': 'الأخت لأب',
      'MATERNAL_BROTHER': 'الأخ لأم',
      'MATERNAL_SISTER': 'الأخت لأم'
    };
    return heirMap[heirType] || heirType;
  }

  getFixedShareArabicText(fixedShare: string | null): string {
    if (!fixedShare) return '';
    const fixedShareMap: { [key: string]: string } = {
      'HALF': 'النصف',
      'QUARTER': 'الربع',
      'EIGHTH': 'الثمن',
      'THIRD': 'الثلث',
      'TWO_THIRDS': 'الثلثين',
      'SIXTH': 'السدس'
    };
    return fixedShareMap[fixedShare] || fixedShare;
  }

  getShareTypeArabicText(shareType: string): string {
    const shareTypeMap: { [key: string]: string } = {
      'FIXED': 'فرض',
      'TAASIB': 'تعصيب',
      'RADD': 'رد',
      'Mahgub': 'محجوب',
      'MALE_DOUBLE_FEMALE': 'للذكر مثل حظ الأنثيين'
    };
    return shareTypeMap[shareType] || shareType;
  }

}
