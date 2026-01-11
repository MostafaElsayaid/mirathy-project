import { Component, ChangeDetectionStrategy, inject, signal, computed, effect, OnInit, OnDestroy, WritableSignal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, FormArray, AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../services/auth.service';
import { HomeService } from '../../services/home.service';
import { HistoryService } from '../../services/history.service';
import { PendingCalculationService } from '../../services/pending-calculation.service';
import { LanguageService } from '../../services/language.service';
import { TranslationService } from '../../services/translation.service';
import { InheritanceRequest, FullInheritanceResponse, HeirResult, CalculationDetails } from '../../services/models';

type HeirName = 'husband' | 'wife' | 'son' | 'daughter' | 'father' | 'mother' |
  'paternalGrandfather' | 'maternalGrandfather' | 'maternalGrandmother' |
  'paternalGrandmother' | 'sonOfSon' | 'daughterOfSon' |
  'fullBrother' | 'fullSister' | 'paternalBrother' |
  'paternalSister' | 'maternalBrother' | 'maternalSister';

// Custom validator for will amount - must not exceed 1/3 of (estate - debts)
function willAmountValidator(form: FormGroup): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null;
    }

    const estateAmount = form.get('estateAmount')?.value || 0;
    const debts = form.get('debts')?.value || 0;
    const willAmount = Number(control.value);
    const netEstate = estateAmount - debts;
    const maxWillAmount = netEstate / 3;

    if (willAmount > maxWillAmount) {
      return { 'willExceedsLimit': { max: maxWillAmount, value: willAmount } };
    }

    return null;
  };
}

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
  toastr = inject(ToastrService);

  calculationResult = signal<HeirResult[] | null>(null);
  calculationDetails = signal<CalculationDetails | null>(null);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showLoginPrompt = signal(false);
  saveStatus = signal<'idle' | 'saved' | 'favorited'>('idle');
  isRtl = this.languageService.isRtl;

  heirsArray = ['paternalGrandfather', 'maternalGrandfather', 'paternalGrandmother',
    'maternalGrandmother', 'sonOfSon', 'daughterOfSon',
    'fullBrother', 'fullSister', 'paternalBrother',
    'paternalSister', 'maternalBrother', 'maternalSister'] as const;

  verses = [
    { text: 'يُوصِيكُمُ اللَّهُ فِي أَوْلَادِكُمْ ۖ لِلذَّكَرِ مِثْلُ حَظِّ الْأُنثَيَيْنِ ۚ فَإِن كُنَّ نِسَاءً فَوْقَ اثْنَتَيْنِ فَلَهُنَّ ثُلُثَا مَا تَرَكَ ۖ وَإِن كَانَتْ وَاحِدَةً فَلَهَا النِّصْفُ...', sourceKey: 'سورة النساء 11' },
    { text: 'وَلَكُمْ نِصْفُ مَا تَرَكَ أَزْوَاجُكُمْ إِن لَّمْ يَكُن لَّهُنَّ وَلَدٌ ۚ فَإِن كَانَ لَّهُنَّ وَلَدٌ فَلَكُمُ الرُّبُعُ مِمَّا تَرَكْنَ ۚ مِن بَعْدِ وَصِيَّةٍ يُوصِينَ بِهَا أَوْ دَيْنٍ...', sourceKey: 'سورة النساء 12' },
    { text: 'وَلَهُنَّ الرُّبُعُ مِمَّا تَرَكْتُمْ إِن لَّمْ يَكُن لَّكُمْ وَلَدٌ ۚ فَإِن كَانَ لَّكُمْ وَلَدٌ فَلَهُنَّ الثُّمُنُ مِمَّا تَرَكْتُم...', sourceKey: 'سورة النساء 12' },
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
  isSonOfSonPresent = computed(() => (this.heirs()?.sonOfSon ?? 0) > 0);
  isDaughterOfSonPresent = computed(() => (this.heirs()?.daughterOfSon ?? 0) > 0);
  isFullBrotherPresent = computed(() => (this.heirs()?.fullBrother ?? 0) > 0);
  isPaternalGrandfatherPresent = computed(() => (this.heirs()?.paternalGrandfather ?? 0) > 0);
  isMaternalGrandfatherPresent = computed(() => (this.heirs()?.maternalGrandfather ?? 0) > 0);

  hasMaleDescendant = computed(() =>
    this.isSonPresent() ||
    this.isSonOfSonPresent()
  );
  hasAnyDescendant = computed(() =>
    this.hasMaleDescendant() ||
    this.isDaughterPresent() ||
    this.isDaughterOfSonPresent()
  );
  hasMaleAscendant = computed(() => this.isFatherPresent() || this.isPaternalGrandfatherPresent() || this.isMaternalGrandfatherPresent());

  isPaternalGrandfatherBlocked = computed(() => this.isFatherPresent());
  isMaternalGrandfatherBlocked = computed(() => this.isFatherPresent() || this.isMotherPresent());
  isSonOfSonBlocked = computed(() => this.isSonPresent());
  isDaughterOfSonBlocked = computed(() => this.isSonPresent());
  isMaternalGrandmotherBlocked = computed(() => this.isMotherPresent());
  isPaternalGrandmotherBlocked = computed(() => this.isMotherPresent() || this.isFatherPresent());
  isFullSiblingBlocked = computed(() => this.hasMaleDescendant() || this.hasMaleAscendant());
  isPaternalSiblingBlocked = computed(() => this.isFullSiblingBlocked() || this.isFullBrotherPresent());
  isMaternalSiblingBlocked = computed(() => this.hasAnyDescendant() || this.hasMaleAscendant());

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      deceasedGender: ['male', Validators.required],
      estateAmount: [0, [Validators.required, Validators.min(0)]],
      debts: [0, [Validators.min(0)]],
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
        maternalGrandfather: [0, [Validators.min(0), Validators.max(1)]],
        maternalGrandmother: [0, [Validators.min(0), Validators.max(1)]],
        paternalGrandmother: [0, [Validators.min(0), Validators.max(1)]],
        sonOfSon: [0, [Validators.min(0)]],
        daughterOfSon: [0, [Validators.min(0)]],
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
      setControlState(this.form.get('heirs.maternalGrandfather'), this.isMaternalGrandfatherBlocked());
      setControlState(this.form.get('heirs.maternalGrandmother'), this.isMaternalGrandmotherBlocked());
      setControlState(this.form.get('heirs.paternalGrandmother'), this.isPaternalGrandmotherBlocked());
      setControlState(this.form.get('heirs.sonOfSon'), this.isSonOfSonBlocked());
      setControlState(this.form.get('heirs.daughterOfSon'), this.isDaughterOfSonBlocked());
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

    // Re-validate wills when estate amount or debts change
    this.form.get('estateAmount')?.valueChanges.subscribe(() => {
      this.wills.controls.forEach(will => {
        will.get('amount')?.updateValueAndValidity();
      });
    });

    this.form.get('debts')?.valueChanges.subscribe(() => {
      this.wills.controls.forEach(will => {
        will.get('amount')?.updateValueAndValidity();
      });
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
      son: undefined,
      daughter: undefined,
      father: 1,
      mother: 1,
      paternalGrandfather: 1,
      maternalGrandfather: 1,
      maternalGrandmother: 1,
      paternalGrandmother: 1,
      sonOfSon: undefined,
      daughterOfSon: undefined,
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
      'paternalGrandfather', 'maternalGrandfather', 'maternalGrandmother', 'paternalGrandmother'
    ];
    return singleHeirs.includes(heir);
  }

  get wills() {
    return this.form.get('wills') as FormArray;
  }

  addWill() {
    if (this.wills.length < 5) {
      const willGroup = this.fb.group({
        amount: [0, [Validators.min(0), (control: AbstractControl) => willAmountValidator(this.form)(control)]]
      });
      this.wills.push(willGroup);
    }
  }

  removeWill(index: number) { this.wills.removeAt(index); }

  getMaxWillAmount(): number {
    const estateAmount = this.form.get('estateAmount')?.value || 0;
    const debts = this.form.get('debts')?.value || 0;
    const netEstate = estateAmount - debts;
    return Math.max(0, netEstate / 3);
  }

  getEstateError(): string | null {
    const control = this.form.get('estateAmount');
    if (control?.invalid && control?.touched) {
      if (control.errors?.['required']) return 'قيمة التركة مطلوبة';
      if (control.errors?.['min']) return 'قيمة التركة يجب أن تكون موجبة';
    }
    return null;
  }

  getDebtsError(): string | null {
    const control = this.form.get('debts');
    if (control?.invalid && control?.touched) {
      if (control.errors?.['min']) return 'قيمة الديون لا يمكن أن تكون سالبة';
    }
    return null;
  }

  getWillError(index: number): string | null {
    const control = this.wills.at(index)?.get('amount');
    if (control?.invalid && control?.touched) {
      if (control.errors?.['min']) return 'قيمة الوصية لا يمكن أن تكون سالبة';
      if (control.errors?.['willExceedsLimit']) {
        const maxAmount = control.errors['willExceedsLimit'].max;
        return `قيمة الوصية لا يمكن أن تتجاوز الثلث من الميراث الصافي (${maxAmount.toFixed(2)})`;
      }
    }
    return null;
  }

  calculate() {
    const estateValue = this.form.get('estateAmount')?.value;
    const estateControl = this.form.get('estateAmount');

    if (!estateValue || estateValue === '' || estateControl?.invalid) {
      if (!estateValue || estateValue === '') {
        this.toastr.error('قيمة التركة مطلوبة', 'خطأ', {
          positionClass: this.isRtl ? 'toast-top-left' : 'toast-top-right',
          timeOut: 5000
        });
      }

      setTimeout(() => {
        const estateInput = document.getElementById('estateAmount');
        if (estateInput) estateInput.focus();
      }, 100);

      estateControl?.markAsTouched();
      return;
    }

    this.markFormGroupTouched(this.form);

    if (this.form.get('debts')?.invalid) {
      return;
    }

    let willsValid = true;
    let firstInvalidWillIndex = -1;
    this.wills.controls.forEach((will, index) => {
      will.get('amount')?.markAsTouched();
      if (will.get('amount')?.invalid) {
        willsValid = false;
        if (firstInvalidWillIndex === -1) {
          firstInvalidWillIndex = index;
        }
      }
    });

    if (!willsValid) {
      if (firstInvalidWillIndex !== -1) {
        setTimeout(() => {
          const willInput = document.querySelector(`[formgroupname="${firstInvalidWillIndex}"] input[formcontrolname="amount"]`);
          if (willInput) (willInput as HTMLElement).focus();
        }, 100);
      }
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
      this.toastr.error('يجب اختيار وارث واحد على الأقل', 'خطأ', {
        positionClass: this.isRtl ? 'toast-top-left' : 'toast-top-right',
        timeOut: 5000
      });
      return;
    }

    const requestBody: InheritanceRequest = {
      totalEstate: Number(formValue.estateAmount) || 0,
      debts: Number(formValue.debts) || 0,
      will: formValue.hasWill ? this.wills.controls.reduce((sum, will) => sum + (Number(will.get('amount')?.value) || 0), 0) : 0,
      heirs: heirs
    };

    console.log('Complete request body:', requestBody);

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.calculationResult.set(null);
    this.calculationDetails.set(null);

    this.homeService.calculate(requestBody).subscribe({
      next: (response: FullInheritanceResponse) => {
        console.log('Full calculation response:', response);

        const results = this.homeService.mapResponseToHeirResults(response);

        console.log('Mapped results:', results);

        this.isLoading.set(false);
        this.calculationResult.set(results);
        this.calculationDetails.set({
          title: response.title,
          totalEstate: response.totalEstate || requestBody.totalEstate,
          debts: requestBody.debts,
          willAmount: requestBody.will,
          netEstate: response.netEstate || (requestBody.totalEstate - requestBody.debts - requestBody.will)
        });

        this.toastr.success('تم حساب الميراث بنجاح', 'نجاح', {
          positionClass: this.isRtl ? 'toast-top-left' : 'toast-top-right',
          timeOut: 3000
        });

        // Auto-scroll to results section
        setTimeout(() => {
          const resultsSection = document.getElementById('resultsSection');
          if (resultsSection) {
            resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
          }
        }, 300);
      },
      error: (err) => {
        console.error('Full error details:', err);
        this.isLoading.set(false);

        let errorMsg = 'حدث خطأ غير متوقع';

        if (err.error && err.error.message) {
          errorMsg = `خطأ في الخادم: ${err.error.message}`;
        } else if (err.status === 400) {
          if (err.error && err.error.includes('Cannot deserialize')) {
            errorMsg = 'خطأ في تنسيق البيانات المرسلة للخادم';
          } else {
            errorMsg = 'طلب غير صالح. يرجى التأكد من البيانات المدخلة';
          }
        } else if (err.status === 0) {
          errorMsg = 'تعذر الاتصال بالخادم. تأكد من تشغيل Backend';
        } else {
          errorMsg = `حدث خطأ غير متوقع: ${err.status} ${err.statusText}`;
        }

        this.errorMessage.set(errorMsg);
        this.toastr.error(errorMsg, 'خطأ', {
          positionClass: this.isRtl ? 'toast-top-left' : 'toast-top-right',
          timeOut: 5000
        });
      }
    });
  }

  private markFormGroupTouched(formGroup: FormGroup | FormArray) {
    Object.values(formGroup.controls).forEach(control => {
      if (control instanceof FormGroup || control instanceof FormArray) {
        this.markFormGroupTouched(control);
      } else {
        control.markAsTouched();
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

    //this.historyService.addCase(results, estate, heirsSummary || 'N/A', isFavorite);
    this.saveStatus.set(isFavorite ? 'favorited' : 'saved');

    const message = isFavorite ? 'تم إضافة الحساب إلى المفضلة' : 'تم حفظ الحساب بنجاح';
    this.toastr.success(message, 'نجاح', {
      positionClass: this.isRtl ? 'toast-top-left' : 'toast-top-right',
      timeOut: 3000
    });

    setTimeout(() => {
      if(this.saveStatus() !== 'idle') this.saveStatus.set('idle');
    }, 2500);
  }

  get totalDistributed(): number {
    const results = this.calculationResult();
    return results ? results.reduce((sum, r) => sum + r.amount, 0) : 0;
  }

  resetForm() {
    this.form.reset({
      deceasedGender: 'male',
      estateAmount: 0,
      debts: 0,
      hasWill: false,
      wills: [],
      heirs: {
        husband: 0,
        wife: 0,
        son: 0,
        daughter: 0,
        father: 0,
        mother: 0,
        paternalGrandfather: 0,
        maternalGrandfather: 0,
        maternalGrandmother: 0,
        paternalGrandmother: 0,
        sonOfSon: 0,
        daughterOfSon: 0,
        fullBrother: 0,
        fullSister: 0,
        paternalBrother: 0,
        paternalSister: 0,
        maternalBrother: 0,
        maternalSister: 0,
      }
    });

    this.calculationResult.set(null);
    this.calculationDetails.set(null);
    this.errorMessage.set(null);
    this.saveStatus.set('idle');
    this.showLoginPrompt.set(false);

    this.toastr.info('تم إعادة تعيين النموذج بنجاح', 'تم', {
      positionClass: this.isRtl ? 'toast-top-left' : 'toast-top-right',
      timeOut: 3000
    });

    // Auto-scroll to top of page
    setTimeout(() => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }, 200);
  }
}
