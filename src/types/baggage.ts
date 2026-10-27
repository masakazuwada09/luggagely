export type ItemSource = 'AI_SCANNED' | 'MANUAL_ENTRY';

export type StudioTab = 'DASHBOARD' | 'MANIFEST' | 'CAMERA' | 'BOARDING' | 'SETTINGS' | 'PROFILE';

export interface ContourPointNorm {
  x: number;
  y: number;
}

export interface BoundingBoxNorm {
  xMin: number;
  yMin: number;
  width: number;
  height: number;
}

export interface BaggageItem {
  id: string;
  name: string;
  category: string;
  weightKg: number;
  source: ItemSource;
  confidence?: number;
  boundingBox?: BoundingBoxNorm;
  contourPoints?: ContourPointNorm[];
  imageUri?: string;
  shiftableToPersonalItem: boolean;
}

export interface AirlineAllowancePreset {
  id: string;
  label: string;
  sublabel: string;
  limitKg: number;
}

export interface AirlineCarrierOption {
  id: 'cebu_pacific' | 'philippine_airlines' | 'airasia';
  carrierName: string;
  iataCode: string;
  logoBadgeText: string;
  brandPrimaryHex: string;
  brandSecondaryHex: string;
  defaultPnr: string;
  flightNumber: string;
  route: string;
  fareBundle: string;
  cabinInclusionKg: number;
  cabinInclusionDetails: string;
  checkedInclusionKg: number;
  checkedInclusionDetails: string;
}

export const PARTNER_AIRLINES: AirlineCarrierOption[] = [
  {
    id: 'cebu_pacific',
    carrierName: 'Cebu Pacific',
    iataCode: '5J',
    logoBadgeText: '5J • CEB',
    brandPrimaryHex: '#00AEEF',
    brandSecondaryHex: '#0072BC',
    defaultPnr: 'CEB5J89',
    flightNumber: '5J 567',
    route: 'MNL (Manila T3) → CEB (Mactan-Cebu)',
    fareBundle: 'Go Easy + CEB Prepaid Baggage',
    cabinInclusionKg: 7.0,
    cabinInclusionDetails: '1 Carry-on (56×36×23cm) + 1 under-seat item, max 7.0 kg',
    checkedInclusionKg: 20.0,
    checkedInclusionDetails: '1 pc × 20.0 kg CEB Prepaid Checked Baggage included',
  },
  {
    id: 'philippine_airlines',
    carrierName: 'Philippine Airlines',
    iataCode: 'PR',
    logoBadgeText: 'PR • PAL',
    brandPrimaryHex: '#002B7F',
    brandSecondaryHex: '#CE1126',
    defaultPnr: 'PAL42M',
    flightNumber: 'PR 432',
    route: 'MNL (Manila T1) → NRT (Tokyo Narita)',
    fareBundle: 'Economy Fiesta • Mabuhay FBA',
    cabinInclusionKg: 7.0,
    cabinInclusionDetails: '1 Cabin Carry-on (56×36×23cm) max 7.0 kg + 1 personal item',
    checkedInclusionKg: 23.0,
    checkedInclusionDetails: '2 pcs × 23.0 kg Free Baggage Allowance (FBA) included',
  },
  {
    id: 'airasia',
    carrierName: 'AirAsia',
    iataCode: 'Z2',
    logoBadgeText: 'Z2 • AIR',
    brandPrimaryHex: '#E60012',
    brandSecondaryHex: '#B8000E',
    defaultPnr: 'Z2AK77',
    flightNumber: 'Z2 711',
    route: 'MNL (Manila T3) → KIX (Osaka Kansai)',
    fareBundle: 'Value Pack + Checked 20kg',
    cabinInclusionKg: 7.0,
    cabinInclusionDetails: '1 Cabin bag + 1 small bag combined max 7.0 kg',
    checkedInclusionKg: 20.0,
    checkedInclusionDetails: '1 pc × 20.0 kg Checked Baggage Value Pack included',
  },
];

export const AIRLINE_ALLOWANCE_PRESETS: AirlineAllowancePreset[] = [
  { id: 'cabin_7', label: 'Cabin 7.0 kg', sublabel: 'Strict Carry-On', limitKg: 7.0 },
  { id: 'cabin_10', label: 'Priority 10.0 kg', sublabel: 'Priority Cabin', limitKg: 10.0 },
  { id: 'checked_20', label: 'Checked 20.0 kg', sublabel: 'Standard Hold', limitKg: 20.0 },
  { id: 'checked_23', label: 'Checked 23.0 kg', sublabel: 'Long-Haul Intl', limitKg: 23.0 },
];

export const FB_THEME = {
  feedGrayBg: '#F0F2F5',
  cardWhite: '#FFFFFF',
  surfaceLight: '#F7F8FA',
  chipGray: '#E4E6EB',
  borderLight: '#DADDE1',
  textPrimary: '#1C1E21',
  textSecondary: '#65676B',
  facebookBlue: '#1877F2',
  facebookBlueDark: '#166FE5',
  facebookBlueSoft: '#E7F3FF',
  emeraldSafe: '#10B981',
  emeraldSoft: '#ECFDF5',
  coralDanger: '#EF4444',
  coralSoft: '#FEF2F2',
} as const;
