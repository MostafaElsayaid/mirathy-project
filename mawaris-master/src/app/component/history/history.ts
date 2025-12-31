import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HistoryService, SavedCase } from '../../services/history.service';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './history.html',
  styleUrl: './history.css'
})
export class HistoryComponent implements OnInit {
  historyService = inject(HistoryService);
  languageService = inject(LanguageService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  activeTab = signal<'saved' | 'favorites'>('saved');
  savedCases = signal<SavedCase[]>([]);
  favoriteCases = signal<SavedCase[]>([]);
  isLoading = signal<boolean>(false);
  showDetailsModal = signal<boolean>(false);
  selectedCase = signal<SavedCase | null>(null);
  editingSummary = signal<boolean>(false);
  editedSummary = signal<string>('');

  isRtl = this.languageService.isRtl;

  ngOnInit() {
    // قراءة الـ query parameter لتحديد الـ tab النشط
    this.route.queryParams.subscribe(params => {
      const tab = params['tab'];
      if (tab === 'favorites') {
        this.activeTab.set('favorites');
      } else {
        this.activeTab.set('saved');
      }
      this.loadCases();
    });
  }

  loadCases() {
    this.isLoading.set(true);
    
    this.historyService.getSavedCases().subscribe({
      next: (cases) => {
        this.savedCases.set(cases);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading saved cases:', err);
        this.isLoading.set(false);
      }
    });

    this.historyService.getFavoriteCases().subscribe({
      next: (cases) => {
        this.favoriteCases.set(cases);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading favorite cases:', err);
        this.isLoading.set(false);
      }
    });
  }

  setActiveTab(tab: 'saved' | 'favorites') {
    this.activeTab.set(tab);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: tab },
      queryParamsHandling: 'merge'
    });
  }

  toggleFavorite(id: number, isFavorite: boolean) {
    this.historyService.toggleFavorite(id, !isFavorite).subscribe({
      next: () => {
        this.loadCases();
        
        // الانتقال التلقائي بين التبويبات
        setTimeout(() => {
          if (this.activeTab() === 'favorites' && !isFavorite) {
            this.setActiveTab('saved');
          } else if (this.activeTab() === 'saved' && isFavorite) {
            this.setActiveTab('favorites');
          }
        }, 300);
      },
      error: (err) => {
        console.error('Error toggling favorite:', err);
      }
    });
  }

  deleteCase(id: number) {
    if (confirm('هل أنت متأكد من حذف هذا الحساب؟')) {
      this.historyService.deleteCase(id).subscribe({
        next: () => {
          this.loadCases();
          if (this.selectedCase()?.id === id) {
            this.closeDetailsModal();
          }
        },
        error: (err) => {
          console.error('Error deleting case:', err);
        }
      });
    }
  }

  viewDetails(caseItem: SavedCase) {
    this.selectedCase.set(caseItem);
    this.editedSummary.set(caseItem.heirs);
    this.editingSummary.set(false);
    this.showDetailsModal.set(true);
  }

  closeDetailsModal() {
    this.showDetailsModal.set(false);
    this.selectedCase.set(null);
  }

  startEditingSummary() {
    this.editingSummary.set(true);
  }

  saveEditedSummary() {
    if (this.selectedCase() && this.editedSummary().trim()) {
      this.historyService.updateCase(this.selectedCase()!.id, { 
        heirs: this.editedSummary() 
      }).subscribe({
        next: () => {
          this.loadCases();
          this.selectedCase.set({ 
            ...this.selectedCase()!, 
            heirs: this.editedSummary() 
          });
          this.editingSummary.set(false);
        },
        error: (err) => {
          console.error('Error updating summary:', err);
        }
      });
    }
  }

  cancelEditingSummary() {
    this.editedSummary.set(this.selectedCase()?.heirs || '');
    this.editingSummary.set(false);
  }
}