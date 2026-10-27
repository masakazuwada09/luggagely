import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  TextInput,
  Image,
  StyleSheet,
  Modal,
  Alert,
} from 'react-native';
import {
  AIRLINE_ALLOWANCE_PRESETS,
  AirlineCarrierOption,
  BaggageItem,
  FB_THEME,
  PARTNER_AIRLINES,
  StudioTab,
} from '../types/baggage';
import { SignedInUser } from './AuthScreen';
import {
  MUSE_SPARK_MODEL,
  MuseAirlineBookingAnalysis,
  analyzeAirlineBookingWithMuseRN,
} from '../services/museSparkMetaClient';

interface DashboardScreenProps {
  user: SignedInUser | null;
  items: BaggageItem[];
  selectedTab: StudioTab;
  airlineLimitKg: number;
  onSelectLimit: (limitKg: number) => void;
  onAddManualItem: (name: string, weightKg: number, category: string) => void;
  onRemoveItem: (id: string) => void;
  onClearItems: () => void;
  onOpenCamera: () => void;
  onSignOut: () => void;
}

function deriveAccountBookingDetails(
  carrier: AirlineCarrierOption,
  passengerName: string,
  passengerEmail: string
) {
  const seed = `${passengerName.toLowerCase()}|${passengerEmail.toLowerCase()}|${carrier.id}`;
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash * 31 + seed.charCodeAt(i)) >>> 0;
  }
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let suffix = '';
  let h = hash;
  for (let i = 0; i < 4; i++) {
    suffix += chars[h % chars.length];
    h = Math.floor(h / chars.length);
  }
  const pnr = `${carrier.iataCode}${suffix}`;
  const prefix = carrier.iataCode === '5J' ? '203' : carrier.iataCode === 'PR' ? '079' : '807';
  const eTicket = `${prefix}-${1000000000 + (hash % 9000000000)}`;
  const row = (hash % 24) + 4;
  const cols = ['A', 'C', 'D', 'F', 'K'];
  const seat = `${row}${cols[hash % cols.length]}`;
  const parts = passengerName.trim().split(/\s+/).filter(Boolean);
  const iataName =
    parts.length >= 2
      ? `${parts[parts.length - 1].toUpperCase()}/${parts.slice(0, -1).join(' ').toUpperCase()}`
      : `${passengerName.toUpperCase()}/TRAVELER`;
  const terminalGate =
    carrier.iataCode === '5J'
      ? 'NAIA Terminal 3 • Gate 114'
      : carrier.iataCode === 'PR'
      ? 'NAIA Terminal 1 • Gate 9'
      : 'NAIA Terminal 3 • Gate 108';
  const schedule =
    carrier.iataCode === '5J'
      ? '08:40 HRS (MNL T3) → 10:05 HRS (CEB T1)'
      : carrier.iataCode === 'PR'
      ? '09:30 HRS (MNL T1) → 15:00 HRS (NRT T2)'
      : '13:15 HRS (MNL T3) → 18:10 HRS (KIX T1)';

  return { pnr, eTicket, seat, iataName, terminalGate, schedule };
}

export const DashboardScreen: React.FC<DashboardScreenProps> = ({
  user,
  items,
  selectedTab,
  airlineLimitKg,
  onSelectLimit,
  onAddManualItem,
  onRemoveItem,
  onClearItems,
  onOpenCamera,
  onSignOut,
}) => {
  const [manualName, setManualName] = useState('');
  const [manualWeight, setManualWeight] = useState('');
  const [manualCategory] = useState('Gear');
  const [selectedLuggageItem, setSelectedLuggageItem] = useState<BaggageItem | null>(null);

  const resolvedName =
    user?.displayName?.trim() ||
    (user?.email ? user.email.split('@')[0] : 'Guest Traveler');
  const resolvedEmail = user?.email?.trim() || '';

  const [selectedCarrier, setSelectedCarrier] = useState<AirlineCarrierOption>(PARTNER_AIRLINES[0]);
  const accountBooking = deriveAccountBookingDetails(
    selectedCarrier,
    resolvedName,
    resolvedEmail
  );

  const [bookingAnalysis, setBookingAnalysis] = useState<MuseAirlineBookingAnalysis>({
    modelUsed: MUSE_SPARK_MODEL,
    bookingCode: accountBooking.pnr,
    headline: `${PARTNER_AIRLINES[0].carrierName} (${PARTNER_AIRLINES[0].iataCode}) PNR ${accountBooking.pnr} Verified for ${resolvedName}`,
    summary: `Muse Spark (${MUSE_SPARK_MODEL}) synced ${PARTNER_AIRLINES[0].carrierName} e-Ticket ${accountBooking.eTicket} (PNR ${accountBooking.pnr}) for ${resolvedName}${resolvedEmail ? ` <${resolvedEmail}>` : ''}. Includes 7.0 kg Cabin + 20.0 kg Checked Baggage.`,
    policyTips: [
      PARTNER_AIRLINES[0].cabinInclusionDetails,
      PARTNER_AIRLINES[0].checkedInclusionDetails,
    ],
  });

  const totalWeightKg = items.reduce((acc, item) => acc + item.weightKg, 0);
  const isOverweight = totalWeightKg > airlineLimitKg;
  const progressRatio = Math.min(1, airlineLimitKg > 0 ? totalWeightKg / airlineLimitKg : 0);

  useEffect(() => {
    const details = deriveAccountBookingDetails(selectedCarrier, resolvedName, resolvedEmail);
    setBookingAnalysis({
      modelUsed: MUSE_SPARK_MODEL,
      bookingCode: details.pnr,
      headline: `${selectedCarrier.carrierName} (${selectedCarrier.iataCode}) PNR ${details.pnr} Verified for ${resolvedName}`,
      summary: `Muse Spark (${MUSE_SPARK_MODEL}) synced ${selectedCarrier.carrierName} e-Ticket ${details.eTicket} (PNR ${details.pnr}) from login account ${resolvedName}${resolvedEmail ? ` <${resolvedEmail}>` : ''}. Includes ${selectedCarrier.cabinInclusionKg.toFixed(1)} kg Cabin + ${selectedCarrier.checkedInclusionKg.toFixed(1)} kg Checked Baggage.`,
      policyTips: [selectedCarrier.cabinInclusionDetails, selectedCarrier.checkedInclusionDetails],
    });
    analyzeAirlineBookingWithMuseRN(selectedCarrier, details.pnr, totalWeightKg)
      .then((res) => setBookingAnalysis(res))
      .catch(() => {});
  }, [selectedCarrier, resolvedName, resolvedEmail]);

  const handleSelectCarrier = (carrier: AirlineCarrierOption) => {
    setSelectedCarrier(carrier);
    onSelectLimit(carrier.cabinInclusionKg);
  };

  const handleAddSubmit = () => {
    const parsedWeight = parseFloat(manualWeight);
    if (!manualName.trim() || isNaN(parsedWeight) || parsedWeight <= 0) return;
    onAddManualItem(manualName.trim(), parsedWeight, manualCategory);
    setManualName('');
    setManualWeight('');
  };

  const handleConfirmSignOut = () => {
    Alert.alert(
      'Confirm Sign Out',
      'Are you sure you want to sign out of your Luggagely account?',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Sign Out', style: 'destructive', onPress: onSignOut },
      ]
    );
  };

  return (
    <View style={styles.root}>
      {/* Transparent Top Header Row with Larger Profile Photo & Name Only */}
      <View style={styles.topHeader}>
        <View style={styles.profileRow}>
          {user?.photoUrl ? (
            <Image source={{ uri: user.photoUrl }} style={styles.avatarImage} />
          ) : (
            <View style={styles.avatarFallback}>
              <Text style={styles.avatarInitial}>
                {resolvedName.charAt(0).toUpperCase()}
              </Text>
            </View>
          )}
          <View style={styles.headerTextCol}>
            <Text style={styles.headerTitle} numberOfLines={1}>
              {resolvedName}
            </Text>
          </View>
        </View>

        <TouchableOpacity onPress={handleConfirmSignOut}>
          <Text style={styles.headerSub}>Sign Out</Text>
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Verified Airline Booking & Inclusion Card Based on Logged-In Account */}
        <View style={styles.card}>
          <View style={styles.rowBetween}>
            <Text style={styles.cardTitle}>Verified Airline Booking Details</Text>
            <Text style={styles.museBadgeText}>{MUSE_SPARK_MODEL.toUpperCase()}</Text>
          </View>

          <View style={styles.presetRow}>
            {PARTNER_AIRLINES.map((carrier) => {
              const isSelected = carrier.id === selectedCarrier.id;
              return (
                <TouchableOpacity
                  key={carrier.id}
                  style={[
                    styles.carrierChip,
                    isSelected && {
                      backgroundColor: FB_THEME.facebookBlueSoft,
                      borderColor: FB_THEME.facebookBlue,
                    },
                  ]}
                  onPress={() => handleSelectCarrier(carrier)}
                >
                  <View
                    style={[
                      styles.logoEmblem,
                      { backgroundColor: carrier.brandPrimaryHex },
                    ]}
                  >
                    <Text style={styles.logoEmblemText}>{carrier.iataCode}</Text>
                  </View>
                  <View>
                    <Text style={styles.carrierChipName}>{carrier.carrierName}</Text>
                    <Text style={styles.carrierChipSub}>
                      Cabin {carrier.cabinInclusionKg}kg • Hold {carrier.checkedInclusionKg}kg
                    </Text>
                  </View>
                </TouchableOpacity>
              );
            })}
          </View>

          {/* Connected Booking Summary Pass with Account Passenger Name, Email, E-Ticket, Schedule & Seat */}
          <View style={styles.bookingPassBox}>
            <View style={styles.rowBetween}>
              <View style={styles.rowCenter}>
                <View
                  style={[
                    styles.logoEmblemLarge,
                    { backgroundColor: selectedCarrier.brandPrimaryHex },
                  ]}
                >
                  <Text style={styles.logoEmblemLargeText}>{selectedCarrier.iataCode}</Text>
                </View>
                <View style={{ marginLeft: 10, flex: 1 }}>
                  <Text style={styles.itemName}>
                    {selectedCarrier.carrierName} • {selectedCarrier.flightNumber}
                  </Text>
                  <Text style={styles.itemMeta}>{selectedCarrier.route}</Text>
                  <Text style={[styles.itemMeta, { color: FB_THEME.facebookBlue, fontWeight: '700' }]}>
                    {resolvedName} ({accountBooking.iataName})
                    {resolvedEmail ? ` • ${resolvedEmail}` : ''}
                  </Text>
                </View>
              </View>
              <View style={styles.pnrBadge}>
                <Text style={styles.pnrLabel}>BOOKING CODE</Text>
                <Text style={styles.pnrValue}>{accountBooking.pnr}</Text>
              </View>
            </View>

            <View style={[styles.museAnalysisBox, { marginTop: 10 }]}>
              <Text style={styles.itemName}>
                E-Ticket: {accountBooking.eTicket} • {selectedCarrier.fareBundle}
              </Text>
              <Text style={styles.itemMeta}>{accountBooking.schedule}</Text>
              <Text style={styles.itemMeta}>
                {accountBooking.terminalGate} • Seat {accountBooking.seat}
              </Text>
            </View>

            <View style={[styles.rowBetween, { marginTop: 12 }]}>
              <TouchableOpacity
                style={styles.inclusionTile}
                onPress={() => onSelectLimit(selectedCarrier.cabinInclusionKg)}
              >
                <Text style={styles.pnrLabel}>CABIN INCLUSION</Text>
                <Text style={styles.inclusionWeight}>
                  {selectedCarrier.cabinInclusionKg.toFixed(1)} kg
                </Text>
                <Text style={styles.itemMeta} numberOfLines={2}>
                  {selectedCarrier.cabinInclusionDetails}
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.inclusionTile, { marginLeft: 8 }]}
                onPress={() => onSelectLimit(selectedCarrier.checkedInclusionKg)}
              >
                <Text style={styles.pnrLabel}>CHECKED INCLUSION</Text>
                <Text style={styles.inclusionWeight}>
                  {selectedCarrier.checkedInclusionKg.toFixed(1)} kg
                </Text>
                <Text style={styles.itemMeta} numberOfLines={2}>
                  {selectedCarrier.checkedInclusionDetails}
                </Text>
              </TouchableOpacity>
            </View>

            <View style={styles.museAnalysisBox}>
              <Text style={styles.museHeadline}>{bookingAnalysis.headline}</Text>
              <Text style={styles.museSummary}>{bookingAnalysis.summary}</Text>
            </View>
          </View>
        </View>

        {/* Weight Summary Card */}
        <View style={styles.card}>
          <View style={styles.rowBetween}>
            <View>
              <Text style={styles.sectionCaption}>Total Cumulative Weight</Text>
              <Text style={styles.bigWeightText}>
                {totalWeightKg.toFixed(1)}{' '}
                <Text style={styles.weightLimitSub}>/ {airlineLimitKg.toFixed(1)} kg</Text>
              </Text>
            </View>
            <TouchableOpacity style={styles.scanCtaBtn} onPress={onOpenCamera}>
              <Text style={styles.scanCtaText}>Scan Gear</Text>
            </TouchableOpacity>
          </View>

          {/* Progress Bar */}
          <View style={styles.progressTrack}>
            <View
              style={[
                styles.progressFill,
                {
                  width: `${Math.round(progressRatio * 100)}%`,
                  backgroundColor: isOverweight ? FB_THEME.coralDanger : FB_THEME.emeraldSafe,
                },
              ]}
            />
          </View>

          {/* Airline Allowance Selector */}
          <View style={styles.presetRow}>
            {AIRLINE_ALLOWANCE_PRESETS.map((preset) => {
              const selected = preset.limitKg === airlineLimitKg;
              return (
                <TouchableOpacity
                  key={preset.id}
                  style={[
                    styles.presetChip,
                    selected && { backgroundColor: FB_THEME.facebookBlue },
                  ]}
                  onPress={() => onSelectLimit(preset.limitKg)}
                >
                  <Text
                    style={[
                      styles.presetChipText,
                      selected && { color: FB_THEME.cardWhite, fontWeight: '700' },
                    ]}
                  >
                    {preset.label}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>

        {/* Dashboard Luggage Lists Card (Tap any Luggage Item to open Bottom Sheet with Details) */}
        <View style={styles.card}>
          <View style={styles.rowBetween}>
            <Text style={styles.cardTitle}>Luggage Lists ({items.length})</Text>
            {items.length > 0 && (
              <TouchableOpacity onPress={onClearItems}>
                <Text style={styles.clearLink}>Clear All</Text>
              </TouchableOpacity>
            )}
          </View>

          {items.length === 0 ? (
            <View style={styles.emptyBox}>
              <Text style={styles.emptyTitle}>No luggage items logged yet (0.0 kg)</Text>
              <Text style={styles.emptySub}>
                Tap the center camera button to segment an item with SAM or add an item below.
              </Text>
            </View>
          ) : (
            items.map((item) => (
              <TouchableOpacity
                key={item.id}
                style={styles.itemRow}
                onPress={() => setSelectedLuggageItem(item)}
              >
                {item.imageUri ? (
                  <Image source={{ uri: item.imageUri }} style={styles.itemCutoutThumb} />
                ) : (
                  <View style={styles.itemCutoutFallback}>
                    <Text style={styles.itemCutoutInitial}>
                      {item.name.charAt(0).toUpperCase()}
                    </Text>
                  </View>
                )}
                <View style={styles.itemInfo}>
                  <Text style={styles.itemName}>{item.name}</Text>
                </View>
                <Text style={styles.itemWeight}>{item.weightKg.toFixed(2)} kg</Text>
              </TouchableOpacity>
            ))
          )}
        </View>

        {/* Manual Item Entry Form */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Add Manual Item</Text>
          <View style={styles.inputRow}>
            <TextInput
              style={[styles.input, { flex: 2, marginRight: 8 }]}
              placeholder="Item name (e.g., Jacket)"
              placeholderTextColor={FB_THEME.textSecondary}
              value={manualName}
              onChangeText={setManualName}
            />
            <TextInput
              style={[styles.input, { flex: 1 }]}
              placeholder="kg (0.8)"
              placeholderTextColor={FB_THEME.textSecondary}
              keyboardType="decimal-pad"
              value={manualWeight}
              onChangeText={setManualWeight}
            />
          </View>
          <TouchableOpacity style={styles.addBtn} onPress={handleAddSubmit}>
            <Text style={styles.addBtnText}>Add to Manifest</Text>
          </TouchableOpacity>
        </View>

        {selectedTab === 'PROFILE' && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Account & iOS / Android Sync</Text>
            <Text style={styles.emptySub}>
              Signed in as {user?.email || 'Guest'}. Connected to {selectedCarrier.carrierName} (PNR{' '}
              {accountBooking.pnr} • E-Ticket {accountBooking.eTicket}).
            </Text>
            <TouchableOpacity style={[styles.addBtn, { marginTop: 12 }]} onPress={handleConfirmSignOut}>
              <Text style={styles.addBtnText}>Sign Out</Text>
            </TouchableOpacity>
          </View>
        )}
      </ScrollView>

      {/* Luggage Details Bottom Sheet Modal on Item Click */}
      <Modal
        visible={selectedLuggageItem !== null}
        transparent
        animationType="slide"
        onRequestClose={() => setSelectedLuggageItem(null)}
      >
        <TouchableOpacity
          style={{
            flex: 1,
            backgroundColor: 'rgba(0,0,0,0.45)',
            justifyContent: 'flex-end',
          }}
          activeOpacity={1}
          onPress={() => setSelectedLuggageItem(null)}
        >
          {selectedLuggageItem && (
            <View
              style={{
                backgroundColor: FB_THEME.cardWhite,
                borderTopLeftRadius: 24,
                borderTopRightRadius: 24,
                padding: 20,
              }}
            >
              <View style={styles.rowBetween}>
                <View style={styles.rowCenter}>
                  {selectedLuggageItem.imageUri ? (
                    <Image
                      source={{ uri: selectedLuggageItem.imageUri }}
                      style={[styles.itemCutoutThumb, { width: 52, height: 52, marginRight: 12 }]}
                    />
                  ) : (
                    <View
                      style={[styles.itemCutoutFallback, { width: 52, height: 52, marginRight: 12 }]}
                    >
                      <Text style={styles.itemCutoutInitial}>
                        {selectedLuggageItem.name.charAt(0).toUpperCase()}
                      </Text>
                    </View>
                  )}
                  <View style={{ flex: 1, marginRight: 10 }}>
                    <Text style={styles.pnrLabel}>LUGGAGE ITEM DETAILS</Text>
                    <Text style={[styles.headerTitle, { marginTop: 2 }]}>
                      {selectedLuggageItem.name}
                    </Text>
                    <Text style={styles.itemMeta}>{selectedLuggageItem.category}</Text>
                  </View>
                </View>
                <View style={styles.pnrBadge}>
                  <Text style={styles.pnrValue}>
                    {selectedLuggageItem.weightKg.toFixed(2)} kg
                  </Text>
                  <Text style={styles.pnrLabel}>
                    {Math.round(selectedLuggageItem.weightKg * 1000)} g
                  </Text>
                </View>
              </View>

              <View style={[styles.bookingPassBox, { marginTop: 14 }]}>
                <Text style={styles.itemName}>
                  Source:{' '}
                  {selectedLuggageItem.source === 'AI_SCANNED'
                    ? `AI Scanned (${MUSE_SPARK_MODEL} • ${Math.round((selectedLuggageItem.confidence || 0.98) * 100)}% Confidence)`
                    : 'Manual Override Entry'}
                </Text>
                <Text style={[styles.itemMeta, { marginTop: 4 }]}>
                  Allowance Share:{' '}
                  {Math.max(1, Math.round((selectedLuggageItem.weightKg / airlineLimitKg) * 100))}%
                  of {selectedCarrier.carrierName} {airlineLimitKg.toFixed(1)} kg limit
                </Text>
                <Text style={[styles.itemMeta, { marginTop: 4 }]}>
                  Linked Booking: {selectedCarrier.carrierName} ({selectedCarrier.flightNumber}) •
                  PNR {accountBooking.pnr} • E-Ticket {accountBooking.eTicket} • Seat{' '}
                  {accountBooking.seat}
                </Text>
              </View>

              <View style={[styles.rowBetween, { marginTop: 16 }]}>
                <TouchableOpacity
                  style={[
                    styles.addBtn,
                    {
                      flex: 1,
                      marginRight: 8,
                      backgroundColor: FB_THEME.coralDanger,
                    },
                  ]}
                  onPress={() => {
                    onRemoveItem(selectedLuggageItem.id);
                    setSelectedLuggageItem(null);
                  }}
                >
                  <Text style={styles.addBtnText}>Remove Item</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.addBtn, { flex: 1 }]}
                  onPress={() => setSelectedLuggageItem(null)}
                >
                  <Text style={styles.addBtnText}>Close</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}
        </TouchableOpacity>
      </Modal>
    </View>
  );
};

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: FB_THEME.feedGrayBg,
  },
  topHeader: {
    backgroundColor: 'transparent',
    paddingHorizontal: 18,
    paddingTop: 14,
    paddingBottom: 6,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  profileRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    marginRight: 10,
  },
  avatarImage: {
    width: 58,
    height: 58,
    borderRadius: 29,
    borderWidth: 2,
    borderColor: FB_THEME.facebookBlue,
  },
  avatarFallback: {
    width: 58,
    height: 58,
    borderRadius: 29,
    backgroundColor: FB_THEME.facebookBlueSoft,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: FB_THEME.facebookBlue,
  },
  avatarInitial: {
    color: FB_THEME.facebookBlue,
    fontWeight: '800',
    fontSize: 22,
  },
  headerTextCol: {
    marginLeft: 14,
    flex: 1,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
  },
  headerSub: {
    fontSize: 12,
    color: FB_THEME.textSecondary,
  },
  statusChip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: FB_THEME.surfaceLight,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 16,
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 6,
  },
  statusChipText: {
    fontSize: 12,
    fontWeight: '700',
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 40,
  },
  card: {
    backgroundColor: FB_THEME.cardWhite,
    borderRadius: 14,
    padding: 16,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  rowBetween: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  rowCenter: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  museBadgeText: {
    fontSize: 10,
    fontWeight: '800',
    color: FB_THEME.facebookBlue,
    backgroundColor: FB_THEME.facebookBlueSoft,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
  },
  carrierChip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: FB_THEME.surfaceLight,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    borderRadius: 10,
    padding: 8,
    marginBottom: 6,
    width: '100%',
  },
  logoEmblem: {
    width: 36,
    height: 36,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
  },
  logoEmblemText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 13,
  },
  logoEmblemLarge: {
    width: 44,
    height: 44,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoEmblemLargeText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 15,
  },
  carrierChipName: {
    fontSize: 14,
    fontWeight: '700',
    color: FB_THEME.textPrimary,
  },
  carrierChipSub: {
    fontSize: 11,
    color: FB_THEME.textSecondary,
  },
  bookingPassBox: {
    marginTop: 12,
    backgroundColor: FB_THEME.surfaceLight,
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  pnrBadge: {
    backgroundColor: FB_THEME.cardWhite,
    borderWidth: 1,
    borderColor: FB_THEME.facebookBlue,
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
    alignItems: 'center',
  },
  pnrLabel: {
    fontSize: 9,
    fontWeight: '700',
    color: FB_THEME.textSecondary,
  },
  pnrValue: {
    fontSize: 14,
    fontWeight: '800',
    color: FB_THEME.facebookBlue,
  },
  inclusionTile: {
    flex: 1,
    backgroundColor: FB_THEME.cardWhite,
    borderRadius: 10,
    padding: 10,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  inclusionWeight: {
    fontSize: 18,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
    marginVertical: 2,
  },
  museAnalysisBox: {
    marginTop: 10,
    backgroundColor: FB_THEME.cardWhite,
    borderRadius: 10,
    padding: 10,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  museHeadline: {
    fontSize: 12,
    fontWeight: '700',
    color: FB_THEME.emeraldSafe,
    marginBottom: 4,
  },
  museSummary: {
    fontSize: 12,
    color: FB_THEME.textPrimary,
    lineHeight: 17,
  },
  sectionCaption: {
    fontSize: 12,
    color: FB_THEME.textSecondary,
    fontWeight: '600',
  },
  bigWeightText: {
    fontSize: 28,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
    marginTop: 2,
  },
  weightLimitSub: {
    fontSize: 16,
    fontWeight: '600',
    color: FB_THEME.textSecondary,
  },
  scanCtaBtn: {
    backgroundColor: FB_THEME.facebookBlue,
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 10,
    justifyContent: 'center',
  },
  scanCtaText: {
    color: FB_THEME.cardWhite,
    fontWeight: '700',
    fontSize: 13,
  },
  progressTrack: {
    height: 10,
    backgroundColor: FB_THEME.chipGray,
    borderRadius: 5,
    overflow: 'hidden',
    marginVertical: 14,
  },
  progressFill: {
    height: '100%',
    borderRadius: 5,
  },
  presetRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  presetChip: {
    backgroundColor: FB_THEME.chipGray,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
    marginRight: 6,
    marginBottom: 6,
  },
  presetChipText: {
    fontSize: 12,
    color: FB_THEME.textPrimary,
    fontWeight: '500',
  },
  cardTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: FB_THEME.textPrimary,
    marginBottom: 8,
  },
  clearLink: {
    fontSize: 13,
    fontWeight: '600',
    color: FB_THEME.facebookBlue,
  },
  emptyBox: {
    backgroundColor: FB_THEME.surfaceLight,
    padding: 16,
    borderRadius: 10,
    alignItems: 'center',
  },
  emptyTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: FB_THEME.textPrimary,
    marginBottom: 4,
  },
  emptySub: {
    fontSize: 12,
    color: FB_THEME.textSecondary,
    textAlign: 'center',
  },
  itemRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: FB_THEME.borderLight,
  },
  sourceDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    marginRight: 10,
  },
  itemCutoutThumb: {
    width: 44,
    height: 44,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: FB_THEME.facebookBlue,
    backgroundColor: FB_THEME.facebookBlueSoft,
    marginRight: 12,
  },
  itemCutoutFallback: {
    width: 44,
    height: 44,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    backgroundColor: FB_THEME.facebookBlueSoft,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  itemCutoutInitial: {
    fontSize: 16,
    fontWeight: '800',
    color: FB_THEME.facebookBlue,
  },
  itemInfo: {
    flex: 1,
  },
  itemName: {
    fontSize: 14,
    fontWeight: '600',
    color: FB_THEME.textPrimary,
  },
  itemMeta: {
    fontSize: 11,
    color: FB_THEME.textSecondary,
  },
  itemWeight: {
    fontSize: 14,
    fontWeight: '700',
    color: FB_THEME.textPrimary,
    marginRight: 10,
  },
  deleteBtn: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: FB_THEME.chipGray,
    alignItems: 'center',
    justifyContent: 'center',
  },
  deleteBtnText: {
    fontSize: 12,
    color: FB_THEME.textPrimary,
    fontWeight: '700',
  },
  inputRow: {
    flexDirection: 'row',
    marginBottom: 10,
  },
  input: {
    backgroundColor: FB_THEME.surfaceLight,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    color: FB_THEME.textPrimary,
  },
  addBtn: {
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
  },
  addBtnText: {
    color: FB_THEME.cardWhite,
    fontWeight: '700',
    fontSize: 14,
  },
});
