
import { Component, ChangeDetectionStrategy, inject, signal, computed, effect, OnInit, OnDestroy, WritableSignal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, FormArray, AbstractControl, FormGroup } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { HomeService } from '../../services/home.service';
import { HistoryService } from '../../services/history.service';
import { PendingCalculationService } from '../../services/pending-calculation.service';
import { LanguageService } from '../../services/language.service';
import { TranslationService } from '../../services/translation.service';
import { InheritanceRequest, HeirResult } from '../../services/models';

type HeirName = 'husband' | 'wife' | 'son' | 'daughter' | 'father' | 'mother' | 'paternalGrandfather' | 'maternalGrandmother' | 'paternalGrandmother' | 'grandson' | 'granddaughter' | 'fullBrother' | 'fullSister' | 'paternalBrother' | 'paternalSister' | 'maternalBrother' | 'maternalSister';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit, OnDestroy {
  authService = inject(AuthService);
  homeService = inject(HomeService);
  historyService = inject(HistoryService);
  pendingCalcService = inject(PendingCalculationService);
  http = inject(HttpClient);
  router = inject(Router);
  languageService = inject(LanguageService);
  translationService = inject(TranslationService);

  calculationResult = signal<HeirResult[] | null>(null);
  calculationDetails = signal<{
    totalEstate?: number;
    netEstate?: number;
    debts?: number;
    willAmount?: number;
    remainingAfterWill?: number;
  } | null>(null);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showLoginPrompt = signal(false);
  saveStatus = signal<'idle' | 'saved' | 'favorited'>('idle');
  isRtl = this.languageService.isRtl;

  heirsArray = ['paternalGrandfather', 'paternalGrandmother', 'maternalGrandmother', 'grandson', 'granddaughter', 'fullBrother', 'fullSister', 'paternalBrother', 'paternalSister', 'maternalBrother', 'maternalSister'] as const;

  verses = [
    { text: 'يُوصِيكُمُ اللَّهُ فِي أَوْلَادِكُمْ ۖ لِلذَّكَرِ مِثْلُ حَظِّ الْأُنثَيَيْنِ ۚ فَإِن كُنَّ نِسَاءً فَوْقَ اثْنَتَيْنِ فَلَهُنَّ ثُلُثَا مَا تَرَكَ ۖ وَإِن كَانَتْ وَاحِدَةً فَلَهَا النِّصْفُ...', sourceKey: 'سورة النساء 11' },
    { text: 'وَلَكُمْ نِصْفُ مَا تَرَكَ أَزْوَاجُكُمْ إِن لَّمْ يَكُن لَّهُنَّ وَلَدٌ ۚ فَإِن كَانَ لَّهُنَّ وَلَدٌ فَلَكُمُ الرُّبُعُ مِمَّا تَرَكْنَ ۚ مِن بَعْدِ وَصِيَّةٍ يُوصِينَ بِهَا أَوْ دَيْنٍ...', sourceKey: 'سورة النساء 12' },
    { text: 'وَلَهُنَّ الرُّبُعُ مِمَّا تَرَكْتُمْ إِن لَّمْ يَكُن لَّكُمْ وَلَدٌ ۚ فَإِن كَانَ لَكُمْ وَلَدٌ فَلَهُنَّ الثُّمُنُ مِمَّا تَرَكْتُم...', sourceKey: 'سورة النساء 12' },
    { text: 'يَسْتَفْتُونَكَ قُلِ اللَّهُ يُفْتِيكُمْ فِي الْكَلَالَةِ ۚ إِنِ امْرُؤٌ هَلَكَ لَيْسَ لَهُ وَلَدٌ وَلَهُ أُخْتٌ فَلَهَا نِصْفُ مَا تَرَكَ ۚ وَهُوَ يَرِثُهَا إِن لَّمْ يَكُن لَّهَا وَلَدٌ...', sourceKey: 'سورة النساء 176' }
  ];

  currentVerseIndex = signal(0);
  isVerseVisible = signal(true);
  private intervalId: any;

  form: FormGroup;
  heirs: WritableSignal<any>;


  isFatherPresent = computed(() => (this.heirs()?.father ?? 0) > 0);
  isMotherPresent = computed(() => (this.heirs()?.mother ?? 0) > 0);
  isSonPresent = computed(() => (this.heirs()?.son ?? 0) > 0);
  isDaughterPresent = computed(() => (this.heirs()?.daughter ?? 0) > 0);
  isGrandsonPresent = computed(() => (this.heirs()?.grandson ?? 0) > 0);
  isGranddaughterPresent = computed(() => (this.heirs()?.granddaughter ?? 0) > 0);
  isFullBrotherPresent = computed(() => (this.heirs()?.fullBrother ?? 0) > 0);
  isPaternalGrandfatherPresent = computed(() => (this.heirs()?.paternalGrandfather ?? 0) > 0);

  hasMaleDescendant = computed(() => this.isSonPresent() || this.isGrandsonPresent());
  hasAnyDescendant = computed(() => this.hasMaleDescendant() || this.isDaughterPresent() || this.isGranddaughterPresent());
  hasMaleAscendant = computed(() => this.isFatherPresent() || this.isPaternalGrandfatherPresent());

  isPaternalGrandfatherBlocked = computed(() => this.isFatherPresent());
  isGrandchildBlocked = computed(() => this.isSonPresent());
  isMaternalGrandmotherBlocked = computed(() => this.isMotherPresent());
  isPaternalGrandmotherBlocked = computed(() => this.isMotherPresent() || this.isFatherPresent());
  isFullSiblingBlocked = computed(() => this.hasMaleDescendant() || this.hasMaleAscendant());
  isPaternalSiblingBlocked = computed(() => this.isFullSiblingBlocked() || this.isFullBrotherPresent());
  isMaternalSiblingBlocked = computed(() => this.hasAnyDescendant() || this.hasMaleAscendant());

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      deceasedGender: ['male', Validators.required],
      estateAmount: [100000, [Validators.required, Validators.min(0)]],
      debts: [0, [Validators.required, Validators.min(0)]],
      hasWill: [false],
      wills: this.fb.array([]),
      heirs: this.fb.group({
        husband: [0, [Validators.min(0), Validators.max(1)]],
        wife: [0, [Validators.min(0), Validators.max(4)]],
        son: [0, [Validators.min(0)]],
        daughter: [0, [Validators.min(0)]],
        father: [0, [Validators.min(0), Validators.max(1)]],
        mother: [0, [Validators.min(0), Validators.max(1)]],
        paternalGrandfather: [0, [Validators.min(0), Validators.max(1)]],
        maternalGrandmother: [0, [Validators.min(0), Validators.max(1)]],
        paternalGrandmother: [0, [Validators.min(0), Validators.max(1)]],
        grandson: [0, [Validators.min(0)]],
        granddaughter: [0, [Validators.min(0)]],
        fullBrother: [0, [Validators.min(0)]],
        fullSister: [0, [Validators.min(0)]],
        paternalBrother: [0, [Validators.min(0)]],
        paternalSister: [0, [Validators.min(0)]],
        maternalBrother: [0, [Validators.min(0)]],
        maternalSister: [0, [Validators.min(0)]],
      }),
    });

    this.heirs = signal(this.form.get('heirs')?.getRawValue());
    this.form.get('heirs')?.valueChanges.subscribe(value => this.heirs.set(value));

    effect(() => {
      const setControlState = (control: AbstractControl | null, blocked: boolean) => {
        if (!control) return;
        if (blocked) {
          if (control.enabled) {
            control.setValue(0);
            control.disable();
          }
        } else {
          if (control.disabled) control.enable();
        }
      };

      setControlState(this.form.get('heirs.paternalGrandfather'), this.isPaternalGrandfatherBlocked());
      setControlState(this.form.get('heirs.maternalGrandmother'), this.isMaternalGrandmotherBlocked());
      setControlState(this.form.get('heirs.paternalGrandmother'), this.isPaternalGrandmotherBlocked());
      setControlState(this.form.get('heirs.grandson'), this.isGrandchildBlocked());
      setControlState(this.form.get('heirs.granddaughter'), this.isGrandchildBlocked());
      setControlState(this.form.get('heirs.fullBrother'), this.isFullSiblingBlocked());
      setControlState(this.form.get('heirs.fullSister'), this.isFullSiblingBlocked());
      setControlState(this.form.get('heirs.paternalBrother'), this.isPaternalSiblingBlocked());
      setControlState(this.form.get('heirs.paternalSister'), this.isPaternalSiblingBlocked());
      setControlState(this.form.get('heirs.maternalBrother'), this.isMaternalSiblingBlocked());
      setControlState(this.form.get('heirs.maternalSister'), this.isMaternalSiblingBlocked());
    }, { allowSignalWrites: true });

    this.form.get('deceasedGender')?.valueChanges.subscribe(gender => {
      const husbandControl = this.form.get('heirs.husband');
      const wifeControl = this.form.get('heirs.wife');
      if (gender === 'male') {
        husbandControl?.setValue(0); husbandControl?.disable();
        wifeControl?.enable();
      } else {
        wifeControl?.setValue(0); wifeControl?.disable();
        husbandControl?.enable();
      }
    });

    this.form.get('deceasedGender')?.setValue('male');
  }

  ngOnInit() {
    this.intervalId = setInterval(() => {
      this.isVerseVisible.set(false);
      setTimeout(() => {
        this.currentVerseIndex.update(i => (i + 1) % this.verses.length);
        this.isVerseVisible.set(true);
      }, 500);
    }, 60000);

    const pendingCalc = this.pendingCalcService.getAndConsumePendingCalculation();
    if (pendingCalc && this.authService.isLoggedIn$()) {
      this.form.patchValue(pendingCalc.formValue, { emitEvent: false });
      this.calculationResult.set(pendingCalc.result);
      this.saveCalculation(pendingCalc.action === 'favorite');
    }
  }

  ngOnDestroy() {
    if (this.intervalId) clearInterval(this.intervalId);
  }

  getHeirControl(name: string): AbstractControl | null {
    return this.form.get(`heirs.${String(name)}`);
  }

  toggleHeir(heir: string) {
    const control = this.getHeirControl(heir as HeirName);
    if (control?.enabled) {
      const newValue = control.value === 0 ? 1 : 0;
      control.setValue(newValue);
    }
  }

  incrementHeir(heir: string) {
    const control = this.getHeirControl(heir as HeirName);
    if (control?.enabled) {
      const currentValue = control.value;
      const max = this.getMaxForHeir(heir as HeirName);
      if (max === undefined || currentValue < max) {
        control.setValue(currentValue + 1);
      }
    }
  }

  decrementHeir(heir: string) {
    const control = this.getHeirControl(heir as HeirName);
    if (control?.enabled) {
      const currentValue = control.value;
      const min = this.isSingleHeir(heir as HeirName) ? 1 : 1;
      if (currentValue > min) {
        control.setValue(currentValue - 1);
      }
    }
  }

  private getMaxForHeir(heir: HeirName): number | undefined {
    const maxMap: Record<HeirName, number | undefined> = {
      husband: 1,
      wife: 4,
      son: undefined, // لا يوجد حد
      daughter: undefined,
      father: 1,
      mother: 1,
      paternalGrandfather: 1, // حد أقصى 1
      maternalGrandmother: 1, // حد أقصى 1
      paternalGrandmother: 1, // حد أقصى 1
      grandson: undefined,
      granddaughter: undefined,
      fullBrother: undefined,
      fullSister: undefined,
      paternalBrother: undefined,
      paternalSister: undefined,
      maternalBrother: undefined,
      maternalSister: undefined,
    };
    return maxMap[heir];
  }


  private isSingleHeir(heir: HeirName): boolean {
    const singleHeirs: HeirName[] = [
      'husband', 'wife', 'father', 'mother',
      'paternalGrandfather', 'maternalGrandmother', 'paternalGrandmother'
    ];
    return singleHeirs.includes(heir);
  }

  get wills() {
    return this.form.get('wills') as FormArray;
  }

  addWill() { if (this.wills.length < 5) this.wills.push(this.fb.group({ amount: [0, [Validators.required, Validators.min(1)]] })); }
  removeWill(index: number) { this.wills.removeAt(index); }

  calculate() {
    if (this.form.invalid) {
      this.errorMessage.set('يرجى ملء جميع الحقول المطلوبة بشكل صحيح');
      return;
    }

    const formValue = this.form.getRawValue();
    const heirs: { [key: string]: number } = {};

    Object.entries(formValue.heirs).forEach(([heir, count]) => {
      if (Number(count) > 0) {
        heirs[heir] = Number(count);
      }
    });

    console.log('Original heirs from form:', heirs);

    if (Object.keys(heirs).length === 0) {
      this.errorMessage.set('يجب اختيار وارث واحد على الأقل');
      return;
    }

    let totalWill = 0;
    if (formValue.hasWill && formValue.wills?.length) {
      totalWill = formValue.wills
        .map((w: any) => Number(w.amount) || 0)
        .reduce((a: number, b: number) => a + b, 0);
    }

    const requestBody: InheritanceRequest = {
      totalEstate: Number(formValue.estateAmount),
      debts: Number(formValue.debts),
      will: totalWill,
      heirs: heirs
    };

    console.log('Complete request body:', requestBody);

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.calculationResult.set(null);
    this.calculationDetails.set(null);

    this.homeService.calculate(requestBody).subscribe({
      next: (results) => {
        console.log('Calculation results:', results);
        this.isLoading.set(false);
        this.calculationResult.set(results);
        this.calculationDetails.set({
          totalEstate: requestBody.totalEstate,
          debts: requestBody.debts,
          willAmount: requestBody.will,
          netEstate: requestBody.totalEstate - requestBody.debts - requestBody.will
        });
      },
      error: (err) => {
        console.error('Full error details:', err);
        this.isLoading.set(false);

        // رسائل خطأ أكثر تفصيلاً
        if (err.error && err.error.message) {
          this.errorMessage.set(`خطأ في الخادم: ${err.error.message}`);
        } else if (err.status === 400) {
          if (err.error && err.error.includes('Cannot deserialize')) {
            this.errorMessage.set('خطأ في تنسيق البيانات المرسلة للخادم');
          } else {
            this.errorMessage.set('طلب غير صالح. يرجى التأكد من البيانات المدخلة');
          }
        } else if (err.status === 0) {
          this.errorMessage.set('تعذر الاتصال بالخادم. تأكد من تشغيل Backend');
        } else {
          this.errorMessage.set(`حدث خطأ غير متوقع: ${err.status} ${err.statusText}`);
        }
      }
    });
  }

  saveCalculation(isFavorite: boolean) {
    if (!this.authService.getIsLoggedIn()) {
      const results = this.calculationResult();
      if (results) {
        this.showLoginPrompt.set(true);
        setTimeout(() => {
          this.showLoginPrompt.set(false);
          const formValue = this.form.getRawValue();
          this.pendingCalcService.setPendingCalculation(formValue, results, isFavorite ? 'favorite' : 'save');
          this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
        }, 2000);
      } else {
        this.showLoginPrompt.set(true);
        setTimeout(() => this.showLoginPrompt.set(false), 3000);
      }
      return;
    }

    const results = this.calculationResult();
    const estate = this.form.value.estateAmount;
    if (!results || estate === undefined || estate === null) return;

    const separator = this.isRtl ? '، ' : ', ';
    const heirsSummary = results.filter(r => r.amount > 0).map(r => `${r.count} ${r.heir}`).join(separator);

    this.historyService.addCase(results, estate, heirsSummary || 'N/A', isFavorite);
    this.saveStatus.set(isFavorite ? 'favorited' : 'saved');
    setTimeout(() => {
      if(this.saveStatus() !== 'idle') this.saveStatus.set('idle');
      this.router.navigate(['/history']);
    }, 2500);
  }

  getCalculationSummary(): string {
    const details = this.calculationDetails();
    const results = this.calculationResult();
    if (!results || results.length === 0) return '';

    let summary = '';
    if (details) {
      if (details.totalEstate !== undefined) summary += `إجمالي التركة: ${details.totalEstate.toFixed(2)}<br>`;
      if (details.debts && details.debts > 0) summary += `الديون: ${details.debts.toFixed(2)}<br>`;
      if (details.willAmount && details.willAmount > 0) summary += `الوصايا: ${details.willAmount.toFixed(2)}<br>`;
      if (details.netEstate !== undefined) summary += `صافي التركة للتوزيع: ${details.netEstate.toFixed(2)}<br>`;
    }

    const totalDistributed = results.reduce((sum, r) => sum + (r.amount * r.count), 0);
    summary += `المجموع الموزع: ${totalDistributed.toFixed(2)}`;
    return summary;
  }

  get totalDistributed(): number {
    const results = this.calculationResult();
    return results ? results.reduce((sum, r) => sum + (r.amount * r.count), 0) : 0;
  }
}
