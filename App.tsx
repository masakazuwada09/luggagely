import React, { useState } from 'react';
import { SafeAreaView, StatusBar, StyleSheet, View } from 'react-native';
import { BaggageItem, FB_THEME, StudioTab } from './src/types/baggage';
import { OnboardingScreen } from './src/screens/OnboardingScreen';
import { AuthScreen, SignedInUser } from './src/screens/AuthScreen';
import { DashboardScreen } from './src/screens/DashboardScreen';
import { StickyBottomNavBar } from './src/components/StickyBottomNavBar';
import { SegmentationCameraModal } from './src/components/SegmentationCameraModal';

type AppStep = 'ONBOARDING' | 'AUTH' | 'STUDIO';

export default function App() {
  const [step, setStep] = useState<AppStep>('ONBOARDING');
  const [user, setUser] = useState<SignedInUser | null>(null);
  // Starts empty (0.0 kg) with no default MacBook Pro or pre-filled items on login
  const [items, setItems] = useState<BaggageItem[]>([]);
  const [airlineLimitKg, setAirlineLimitKg] = useState<number>(7.0);
  const [selectedTab, setSelectedTab] = useState<StudioTab>('DASHBOARD');
  const [isCameraOpen, setIsCameraOpen] = useState<boolean>(false);

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
        <OnboardingScreen onGetStarted={() => setStep('AUTH')} />
      )}

      {step === 'AUTH' && (
        <AuthScreen
          onSignInSuccess={(signedInUser) => {
            setUser(signedInUser);
            setItems([]);
            setStep('STUDIO');
          }}
          onContinueAsGuest={() => {
            setUser(null);
            setItems([]);
            setStep('STUDIO');
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
