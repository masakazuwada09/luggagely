import React, { useEffect, useState } from 'react';
import { AppState, SafeAreaView, StatusBar, StyleSheet, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { BaggageItem, FB_THEME, StudioTab } from './src/types/baggage';
import { OnboardingScreen } from './src/screens/OnboardingScreen';
import { AuthScreen, SignedInUser } from './src/screens/AuthScreen';
import { DashboardScreen } from './src/screens/DashboardScreen';
import { StickyBottomNavBar } from './src/components/StickyBottomNavBar';
import {
  ASYNC_STORAGE_ITEMS_KEY,
  SegmentationCameraModal,
} from './src/components/SegmentationCameraModal';

type AppStep = 'ONBOARDING' | 'AUTH' | 'STUDIO';
const SESSION_USER_KEY = '@luggagely_signed_in_user';
const SESSION_STEP_KEY = '@luggagely_active_step';

export default function App() {
  const [step, setStep] = useState<AppStep>('ONBOARDING');
  const [user, setUser] = useState<SignedInUser | null>(null);
  const [items, setItems] = useState<BaggageItem[]>([]);
  const [airlineLimitKg, setAirlineLimitKg] = useState<number>(7.0);
  const [selectedTab, setSelectedTab] = useState<StudioTab>('DASHBOARD');
  const [isCameraOpen, setIsCameraOpen] = useState<boolean>(false);

  // Restore signed-in user, segmented items from AsyncStorage, and redirect to STUDIO (Dashboard)
  useEffect(() => {
    const restoreSessionAndItems = async () => {
      try {
        const [savedUserJson, savedStep, savedItemsJson] = await Promise.all([
          AsyncStorage.getItem(SESSION_USER_KEY),
          AsyncStorage.getItem(SESSION_STEP_KEY),
          AsyncStorage.getItem(ASYNC_STORAGE_ITEMS_KEY),
        ]);
        if (savedItemsJson) {
          const parsedItems: BaggageItem[] = JSON.parse(savedItemsJson);
          if (Array.isArray(parsedItems)) {
            setItems(parsedItems);
          }
        }
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
    restoreSessionAndItems();

    const sub = AppState.addEventListener('change', (nextState) => {
      if (nextState === 'active') {
        restoreSessionAndItems();
      }
    });
    return () => sub.remove();
  }, []);

  const persistItemsToAsyncStorage = (nextItems: BaggageItem[]) => {
    setItems(nextItems);
    AsyncStorage.setItem(ASYNC_STORAGE_ITEMS_KEY, JSON.stringify(nextItems)).catch(() => {});
  };

  const handleAddManualItem = (name: string, weightKg: number, category: string) => {
    const newItem: BaggageItem = {
      id: `manual-${Date.now()}`,
      name,
      category,
      weightKg,
      source: 'MANUAL_ENTRY',
      shiftableToPersonalItem: weightKg <= 1.5,
    };
    persistItemsToAsyncStorage([newItem, ...items]);
  };

  const handleItemsDetected = (detected: BaggageItem[]) => {
    const singleItem = detected[0];
    if (!singleItem) return;
    const nextItems = [
      singleItem,
      ...items.filter((existing) => existing.id !== singleItem.id),
    ];
    persistItemsToAsyncStorage(nextItems);
  };

  const handleRemoveItem = (id: string) => {
    persistItemsToAsyncStorage(items.filter((item) => item.id !== id));
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
            setStep('STUDIO');
            AsyncStorage.multiSet([
              [SESSION_USER_KEY, JSON.stringify(signedInUser)],
              [SESSION_STEP_KEY, 'STUDIO'],
            ]).catch(() => {});
          }}
          onContinueAsGuest={() => {
            setUser(null);
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
                onClearItems={() => persistItemsToAsyncStorage([])}
                onOpenCamera={() => setIsCameraOpen(true)}
                onSignOut={() => {
                  setUser(null);
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
