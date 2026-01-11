export interface InheritanceRequest {
  totalEstate: number;
  debts: number;
  will: number;
  heirs: {
    [key: string]: number;
  };
}

export interface InheritanceShare {
  heirType: string;
  count: number;
  amountPerPerson: number;
  totalAmount: number;
  shareType: 'FIXED' | 'TAASIB' | 'RADD' | 'Mahgub' | 'MALE_DOUBLE_FEMALE';
  fixedShare: 'HALF' | 'QUARTER' | 'EIGHTH' | 'THIRD' | 'TWO_THIRDS' | 'SIXTH' | null;
  reason: string;
}

export interface FullInheritanceResponse {
  title: string;
  totalEstate: number;
  netEstate: number;
  shares: InheritanceShare[];
  remainingEstate: number;
  totalDistributed: number;
}

export interface HeirResult {
  heir: string;
  count: number;
  share: string;
  amount: number;
  reason: string;
}

export interface CalculationDetails {
  title?: string;
  totalEstate?: number;
  netEstate?: number;
  debts?: number;
  willAmount?: number;
}