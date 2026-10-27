import React, { useEffect, useState } from 'react';
import { AppState, SafeAreaView, StatusBar, StyleSheet, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { BaggageItem, FB_THEME, StudioTab } from './src/types/baggage';
import { OnboardingScreen } from './src/screens/OnboardingScreen';
import { AuthScreen, SignedInUser } from './src/screens/AuthScreen';
import { DashboardScreen } from './src/screens/DashboardScreen';
import { StickyBottomNavBar } from './src/components/StickyBottomNavBar';
import { SegmentationCameraModal } from './src/components/SegmentationCameraModal';

type AppStep = 'ONBOARDING' | 'AUTH' | 'STUDIO';
const SESSION_USER_KEY = '@luggagely_signed_in_user';
const SESSION_STEP_KEY = '@luggagely_active_step';

const DEFAULT_LUGGAGE_ITEMS: BaggageItem[] = [
  {
    id: 'ai_mbp16',
    name: 'MacBook Pro 16" M3 Max',
    category: 'Compute & Optics',
    weightKg: 2.15,
    source: 'AI_SCANNED',
    confidence: 0.99,
    shiftableToPersonalItem: true,
  },
  {
    id: 'ai_sony_a7',
    name: 'Sony A7IV + 24-70mm GM',
    category: 'Compute & Optics',
    weightKg: 1.35,
    source: 'AI_SCANNED',
    confidence: 0.98,
    shiftableToPersonalItem: true,
  },
  {
    id: 'ai_anker_27k',
    name: 'Anker Prime 27,650mAh Bank',
    category: 'Lithium & Power',
    weightKg: 0.65,
    source: 'AI_SCANNED',
    confidence: 0.99,
    shiftableToPersonalItem: true,
  },
  {
    id: 'ai_dopp_kit',
    name: 'Aero TSA Liquids Pouch',
    category: 'Liquids & Toiletries',
    weightKg: 0.8,
    source: 'AI_SCANNED',
    confidence: 0.97,
    shiftableToPersonalItem: false,
  },
  {
    id: 'man_chelsea_boots',
    name: 'Packed Weatherproof Boots',
    category: 'Footwear & Apparel',
    weightKg: 1.4,
    source: 'MANUAL_ENTRY',
    shiftableToPersonalItem: false,
  },
];

export default function App() {
  const [step, setStep] = useState<AppStep>('ONBOARDING');
  const [user, setUser] = useState<SignedInUser | null>(null);
  const [items, setItems] = useState<BaggageItem[]>(DEFAULT_LUGGAGE_ITEMS);
  const [airlineLimitKg, setAirlineLimitKg] = useState<number>(7.0);
  const [selectedTab, setSelectedTab] = useState<StudioTab>('DASHBOARD');
  const [isCameraOpen, setIsCameraOpen] = useState<boolean>(false);

  // Restore signed-in user & redirect directly to STUDIO (Dashboard) on launch or return from background
  useEffect(() => {
    const restoreSession = async () => {
      try {
        const [savedUserJson, savedStep] = await Promise.all([
          AsyncStorage.getItem(SESSION_USER_KEY),
          AsyncStorage.getItem(SESSION_STEP_KEY),
        ]);
        if (savedUserJson) {
          const parsedUser: SignedInUser = JSON.parse(savedUserJson);
          setUser(parsedUser);
          setStep('STUDIO');
        } else if (savedStep === 'STUDIO' || savedStep === 'AUTH') {
          setStep(savedStep);
        }
      } catch {
        // Ignore storage read failure
      }
    };
    restoreSession();

    const sub = AppState.addEventListener('change', (nextState) => {
      if (nextState === 'active') {
        restoreSession();
      }
    });
    return () => sub.remove();
  }, []);

  const handleAddManualItem = (name: string, weightKg: number, category: string) => {
    const newItem: BaggageItem = {
      id: `manual-${Date.now()}`,
      name,
      category,
      weightKg,
      source: 'MANUAL_ENTRY',
      shiftableToPersonalItem: weightKg <= 1.5,
    };
    setItems((prev) => [newItem, ...prev]);
  };

  const handleItemsDetected = (detected: BaggageItem[]) => {
    setItems((prev) => [...detected, ...prev]);
  };

  const handleRemoveItem = (id: string) => {
    setItems((prev) => prev.filter((item) => item.id !== id));
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="dark-content" backgroundColor={FB_THEME.cardWhite} />
      {step === 'ONBOARDING' && (
        <OnboardingScreen
          onGetStarted={() => {
            AsyncStorage.setItem(SESSION_STEP_KEY, 'AUTH').catch(() => {});
            setStep('AUTH');
          }}
        />
      )}

      {step === 'AUTH' && (
        <AuthScreen
          onSignInSuccess={(signedInUser) => {
            setUser(signedInUser);
            setItems((prev) => (prev.length > 0 ? prev : DEFAULT_LUGGAGE_ITEMS));
            setStep('STUDIO');
            AsyncStorage.multiSet([
              [SESSION_USER_KEY, JSON.stringify(signedInUser)],
              [SESSION_STEP_KEY, 'STUDIO'],
            ]).catch(() => {});
          }}
          onContinueAsGuest={() => {
            setUser(null);
            setItems((prev) => (prev.length > 0 ? prev : DEFAULT_LUGGAGE_ITEMS));
            setStep('STUDIO');
            AsyncStorage.setItem(SESSION_STEP_KEY, 'STUDIO').catch(() => {});
          }}
        />
      )}

      {step === 'STUDIO' && (
        <View style={styles.studioContainer}>
          {isCameraOpen ? (
            <SegmentationCameraModal
              items={items}
              onClose={() => setIsCameraOpen(false)}
              onItemsDetected={handleItemsDetected}
            />
          ) : (
            <>
              <DashboardScreen
                user={user}
                items={items}
                selectedTab={selectedTab}
                airlineLimitKg={airlineLimitKg}
                onSelectLimit={setAirlineLimitKg}
                onAddManualItem={handleAddManualItem}
                onRemoveItem={handleRemoveItem}
                onClearItems={() => setItems([])}
                onOpenCamera={() => setIsCameraOpen(true)}
                onSignOut={() => {
                  setUser(null);
                  setItems([]);
                  setStep('AUTH');
                  AsyncStorage.multiRemove([SESSION_USER_KEY]).catch(() => {});
                  AsyncStorage.setItem(SESSION_STEP_KEY, 'AUTH').catch(() => {});
                }}
              />
              <StickyBottomNavBar
                selectedTab={selectedTab}
                isScanning={isCameraOpen}
                onSelectTab={(tab) => {
                  if (tab === 'CAMERA') {
                    setIsCameraOpen(true);
                  } else {
                    setSelectedTab(tab);
                  }
                }}
                onCameraClick={() => setIsCameraOpen(true)}
              />
            </>
          )}
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: FB_THEME.cardWhite,
  },
  studioContainer: {
    flex: 1,
    backgroundColor: FB_THEME.feedGrayBg,
  },
});
