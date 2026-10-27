import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Image } from 'react-native';
import { FB_THEME } from '../types/baggage';

interface OnboardingScreenProps {
  onGetStarted: () => void;
}

export const OnboardingScreen: React.FC<OnboardingScreenProps> = ({ onGetStarted }) => {
  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <View style={styles.iconCard}>
          <Image
            source={require('../../public/assets/aistudio/luggageicon_558fc4e6.png')}
            style={styles.heroIcon}
            resizeMode="contain"
          />
        </View>
        <Text style={styles.title}>Welcome to our{'\n'}Luggage Tracker App!</Text>
        <Text style={styles.subtitle}>
          Track your belongings with ease, anytime, anywhere on iOS and Android.
        </Text>
      </View>

      <TouchableOpacity style={styles.primaryBtn} onPress={onGetStarted} activeOpacity={0.85}>
        <Text style={styles.primaryBtnText}>Get Started</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: FB_THEME.cardWhite,
    paddingHorizontal: 24,
    paddingVertical: 48,
    justifyContent: 'space-between',
  },
  content: {
    marginTop: 32,
  },
  iconCard: {
    width: 88,
    height: 88,
    borderRadius: 22,
    backgroundColor: FB_THEME.surfaceLight,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 24,
  },
  heroIcon: {
    width: 60,
    height: 60,
  },
  title: {
    fontSize: 32,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
    lineHeight: 38,
    marginBottom: 12,
  },
  subtitle: {
    fontSize: 15,
    color: FB_THEME.textSecondary,
    lineHeight: 22,
  },
  primaryBtn: {
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 12,
    paddingVertical: 16,
    alignItems: 'center',
  },
  primaryBtnText: {
    color: FB_THEME.cardWhite,
    fontSize: 16,
    fontWeight: '700',
  },
});
