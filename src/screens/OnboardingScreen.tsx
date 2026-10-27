import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Image } from 'react-native';
import { FB_THEME } from '../types/baggage';

interface OnboardingScreenProps {
  onGetStarted: () => void;
}

export const OnboardingScreen: React.FC<OnboardingScreenProps> = ({ onGetStarted }) => {
  return (
    <View style={styles.container}>
      <View style={styles.topContent}>
        <View style={styles.brandRow}>
          <Image
            source={require('../../public/assets/aistudio/Luggagely_cc4b88fe.png')}
            style={styles.luggagelyMark}
            resizeMode="contain"
          />
          <Text style={styles.title}>Luggagely</Text>
        </View>

        <Text style={styles.subtitle}>
          Track your belongings with ease,{'\n'}anytime, anywhere.
        </Text>
      </View>

      <View style={styles.bottomSection}>
        <TouchableOpacity style={styles.primaryBtn} onPress={onGetStarted} activeOpacity={0.85}>
          <Text style={styles.primaryBtnText}>Get Started</Text>
        </TouchableOpacity>

        <View style={styles.metaAttributionRow}>
          <Image
            source={require('../../public/assets/aistudio/meta_logo_clean.png')}
            style={styles.metaLogoImage}
            resizeMode="contain"
          />
          <Text style={styles.metaAttributionText} numberOfLines={1}>
            Brought to you by MUSE SPARK 1.3
          </Text>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: FB_THEME.feedGrayBg,
    paddingHorizontal: 28,
    paddingVertical: 32,
    justifyContent: 'space-between',
  },
  topContent: {
    marginTop: 28,
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 18,
  },
  luggagelyMark: {
    width: 48,
    height: 68,
    marginRight: 12,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: FB_THEME.textPrimary,
    lineHeight: 30,
  },
  subtitle: {
    fontSize: 28,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
    lineHeight: 36,
  },
  bottomSection: {
    width: '100%',
    alignItems: 'center',
  },
  primaryBtn: {
    width: '100%',
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 14,
    paddingVertical: 16,
    alignItems: 'center',
    marginBottom: 14,
  },
  primaryBtnText: {
    color: FB_THEME.cardWhite,
    fontSize: 16,
    fontWeight: '700',
  },
  metaAttributionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  metaLogoImage: {
    width: 58,
    height: 14,
    marginRight: 6,
  },
  metaAttributionText: {
    color: FB_THEME.facebookBlue,
    fontSize: 11.5,
    fontWeight: '600',
    letterSpacing: 0.2,
  },
});
