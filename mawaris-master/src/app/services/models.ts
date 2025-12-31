// models.ts

export interface InheritanceRequest {
  totalEstate: number;
  debts: number;
  will: number;
  heirs: {
    [key: string]: number; // SON, DAUGHTER, GRANDFATHER ...
  };
}

export interface InheritanceShare {
  amount: number;
  heirType: string;
  shareType: 'FIXED' | 'TAASIB' | 'RADD';
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
