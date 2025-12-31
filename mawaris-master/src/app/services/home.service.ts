import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { FullInheritanceResponse, HeirResult, InheritanceRequest } from './models';

@Injectable({
  providedIn: 'root'
})
export class HomeService {

  private apiUrl = 'http://localhost:8087/api/v1/calculate';

  constructor(private http: HttpClient) {}

  /**
   * إرسال طلب حساب المواريث
   */
  calculate(request: InheritanceRequest): Observable<HeirResult[]> {
    // تحويل المفاتيح لتتطابق مع enum HeirType في Backend
    const formattedRequest = {
      ...request,
      heirs: this.formatHeirs(request.heirs)
    };

    console.log('Sending formatted request:', formattedRequest);

    return this.http
      .post<FullInheritanceResponse>(this.apiUrl, formattedRequest)
      .pipe(
        map(response => this.mapToHeirResults(response, request.heirs))
      );
  }

  /**
   * تنسيق المفاتيح لتتطابق مع enum HeirType في Backend
   */
  private formatHeirs(heirs: { [key: string]: number }): { [key: string]: number } {
    const formatted: { [key: string]: number } = {};

    const heirMapping: { [key: string]: string } = {
      // الأزواج
      'husband': 'HUSBAND',
      'wife': 'WIFE',

      // الأصول
      'father': 'FATHER',
      'mother': 'MOTHER',
      'paternalGrandfather': 'GRANDFATHER', // ملاحظة: في Backend هذا هو GRANDFATHER
      'paternalGrandmother': 'GRANDMOTHER_PATERNAL',
      'maternalGrandmother': 'GRANDMOTHER_MATERNAL',

      // الفروع
      'son': 'SON',
      'daughter': 'DAUGHTER',
      'grandson': 'SON_OF_SON', // Backend يسميه SON_OF_SON
      'granddaughter': 'DAUGHTER_OF_SON', // Backend يسميه DAUGHTER_OF_SON

      // الإخوة الأشقاء
      'fullBrother': 'FULL_BROTHER',
      'fullSister': 'FULL_SISTER',

      // الإخوة لأب
      'paternalBrother': 'PATERNAL_BROTHER',
      'paternalSister': 'PATERNAL_SISTER',

      // الإخوة لأم
      'maternalBrother': 'MATERNAL_BROTHER',
      'maternalSister': 'MATERNAL_SISTER'

      // ملاحظة: الأنواع الأخرى مثل أبناء الإخوة والأعمام غير مدعومة في Frontend حاليًا
    };

    Object.entries(heirs).forEach(([key, value]) => {
      if (heirMapping[key]) {
        formatted[heirMapping[key]] = value;
      }
    });

    return formatted;
  }

  /**
   * تحويل Response إلى شكل مناسب للعرض
   */
  private mapToHeirResults(
    response: FullInheritanceResponse,
    originalHeirs: { [key: string]: number }
  ): HeirResult[] {

    return response.shares.map(share => {
      // البحث عن المفتاح الأصلي في Frontend للحصول على العدد
      const reverseMapping: { [key: string]: string } = {
        'HUSBAND': 'husband',
        'WIFE': 'wife',
        'FATHER': 'father',
        'MOTHER': 'mother',
        'GRANDFATHER': 'paternalGrandfather',
        'GRANDMOTHER_PATERNAL': 'paternalGrandmother',
        'GRANDMOTHER_MATERNAL': 'maternalGrandmother',
        'SON': 'son',
        'DAUGHTER': 'daughter',
        'SON_OF_SON': 'grandson',
        'DAUGHTER_OF_SON': 'granddaughter',
        'FULL_BROTHER': 'fullBrother',
        'FULL_SISTER': 'fullSister',
        'PATERNAL_BROTHER': 'paternalBrother',
        'PATERNAL_SISTER': 'paternalSister',
        'MATERNAL_BROTHER': 'maternalBrother',
        'MATERNAL_SISTER': 'maternalSister'
      };

      const originalKey = reverseMapping[share.heirType];
      const count = originalKey ? (originalHeirs[originalKey] ?? 1) : 1;

      return {
        heir: this.getHeirArabicText(share.heirType),
        count: count,
        share: this.getShareText(share.shareType, share.fixedShare),
        amount: share.amount,
        reason: share.reason
      };
    });
  }

  /**
   * ترجمة نوع النصيب
   */
  private getShareText(shareType: string, fixedShare: string | null): string {
    if (fixedShare) {
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

    const shareTypeMap: { [key: string]: string } = {
      'FIXED': 'فرض',
      'TAASIB': 'تعصيب',
      'RADD': 'رد',
      'Mahgub': 'محجوب',
      'MALE_DOUBLE_FEMALE': 'للذكر مثل حظ الأنثيين'
    };

    return shareTypeMap[shareType] || shareType;
  }

  /**
   * ترجمة نوع الوارث
   */
  private getHeirArabicText(heirType: string): string {
    const heirMap: { [key: string]: string } = {
      'HUSBAND': 'الزوج',
      'WIFE': 'الزوجة',
      'FATHER': 'الأب',
      'MOTHER': 'الأم',
      'GRANDFATHER': 'الجد',
      'GRANDMOTHER_PATERNAL': 'الجدة لأب',
      'GRANDMOTHER_MATERNAL': 'الجدة لأم',
      'SON': 'الابن',
      'DAUGHTER': 'البنت',
      'SON_OF_SON': 'حفيد (ابن الابن)',
      'DAUGHTER_OF_SON': 'حفيدة (بنت الابن)',
      'FULL_BROTHER': 'الأخ الشقيق',
      'FULL_SISTER': 'الأخت الشقيقة',
      'PATERNAL_BROTHER': 'الأخ لأب',
      'PATERNAL_SISTER': 'الأخت لأب',
      'MATERNAL_BROTHER': 'الأخ لأم',
      'MATERNAL_SISTER': 'الأخت لأم',
      'SON_OF_FULL_BROTHER': 'ابن الأخ الشقيق',
      'SON_OF_PATERNAL_BROTHER': 'ابن الأخ لأب',
      'FULL_UNCLE': 'عم شقيق',
      'PATERNAL_UNCLE': 'عم لأب',
      'SON_OF_FULL_UNCLE': 'ابن العم الشقيق',
      'SON_OF_PATERNAL_UNCLE': 'ابن العم لأب'
    };

    return heirMap[heirType] || heirType;
  }
}
