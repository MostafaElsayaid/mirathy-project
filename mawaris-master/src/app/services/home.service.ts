import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import { Observable } from 'rxjs';
import { InheritanceRequest, FullInheritanceResponse, HeirResult } from './models';

@Injectable({
  providedIn: 'root'
})
export class HomeService {
  private apiUrl = 'http://localhost:8087/api/v1/calculate';

  constructor(private http: HttpClient) {}

  private authHeader(){
    const  token = localStorage.getItem('token');
    return{
      headers:new HttpHeaders({
        Authorization:`Bearer ${token}`
      }),
    }
  }
  calculate(request: InheritanceRequest): Observable<FullInheritanceResponse> {
    const formattedRequest = {
      ...request,
      heirs: this.formatHeirs(request.heirs)
    };
   const token=localStorage.getItem("token");
   if (token===null)
    return this.http.post<FullInheritanceResponse>(this.apiUrl, formattedRequest);
    else
     return this.http.post<FullInheritanceResponse>(this.apiUrl, formattedRequest,this.authHeader());
  }

  private formatHeirs(heirs: { [key: string]: number }): { [key: string]: number } {
    const formatted: { [key: string]: number } = {};

    const heirMapping: { [key: string]: string } = {
      'husband': 'HUSBAND',
      'wife': 'WIFE',
      'father': 'FATHER',
      'mother': 'MOTHER',
      'paternalGrandfather': 'GRANDFATHER',
      'maternalGrandfather': 'GRANDFATHER_MATERNAL',
      'paternalGrandmother': 'GRANDMOTHER_PATERNAL',
      'maternalGrandmother': 'GRANDMOTHER_MATERNAL',
      'son': 'SON',
      'daughter': 'DAUGHTER',
      'sonOfSon': 'SON_OF_SON',
      'daughterOfSon': 'DAUGHTER_OF_SON',
      'fullBrother': 'FULL_BROTHER',
      'fullSister': 'FULL_SISTER',
      'paternalBrother': 'PATERNAL_BROTHER',
      'paternalSister': 'PATERNAL_SISTER',
      'maternalBrother': 'MATERNAL_BROTHER',
      'maternalSister': 'MATERNAL_SISTER'
    };

    Object.entries(heirs).forEach(([key, value]) => {
      if (heirMapping[key] && value > 0) {
        formatted[heirMapping[key]] = value;
      }
    });

    return formatted;
  }

  getShareText(shareType: string, fixedShare: string | null): string {
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

  mapResponseToHeirResults(response: FullInheritanceResponse): HeirResult[] {
    return response.shares.map(share => ({
      heir: this.getHeirArabicText(share.heirType),
      count: share.count,
      share: this.getShareText(share.shareType, share.fixedShare),
      amount: share.totalAmount,
      reason: share.reason
    }));
  }
}
