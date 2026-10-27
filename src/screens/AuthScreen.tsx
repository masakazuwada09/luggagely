import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import Icon from 'react-native-ico-social-media';
import { FB_THEME } from '../types/baggage';

export interface SignedInUser {
  uid: string;
  displayName: string;
  email: string;
  photoUrl: string | null;
}

interface AuthScreenProps {
  onSignInSuccess: (user: SignedInUser) => void;
  onContinueAsGuest: () => void;
}

export const AuthScreen: React.FC<AuthScreenProps> = ({
  onSignInSuccess,
  onContinueAsGuest,
}) => {
  const [isSignUpMode, setIsSignUpMode] = useState(false);

  const handleGoogleLogin = () => {
    onSignInSuccess({
      uid: 'ios-google-user',
      displayName: 'Masakazu Wada',
      email: 'masakazuwada09@gmail.com',
      photoUrl: 'https://lh3.googleusercontent.com/a/default-user=s96-c',
    });
  };

  const handleFacebookLogin = () => {
    onSignInSuccess({
      uid: 'ios-facebook-user',
      displayName: 'Masakazu Wada',
      email: 'masakazuwada09@gmail.com',
      photoUrl: 'https://lh3.googleusercontent.com/a/default-user=s96-c',
    });
  };

  return (
    <View style={styles.container}>
      <View style={styles.topRow}>
        <View style={styles.modeTogglePill}>
          <TouchableOpacity
            style={[styles.modeTab, !isSignUpMode && styles.modeTabActive]}
            onPress={() => setIsSignUpMode(false)}
          >
            <Text style={[styles.modeTabText, !isSignUpMode && styles.modeTabTextActive]}>
              Log In
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.modeTab, isSignUpMode && styles.modeTabActive]}
            onPress={() => setIsSignUpMode(true)}
          >
            <Text style={[styles.modeTabText, isSignUpMode && styles.modeTabTextActive]}>
              Sign Up
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.card}>
        <Text style={styles.kicker}>
          {isSignUpMode ? 'CREATE ACCOUNT' : 'WELCOME BACK'}
        </Text>
        <Text style={styles.title}>
          {isSignUpMode
            ? 'Create your Luggagely account'
            : 'Ready to keep track of your luggage?'}
        </Text>
        <Text style={styles.subtitle}>
          {isSignUpMode
            ? 'Sign up with your Google account for automatic cloud manifest backup and airline booking lookup.'
            : 'Please log in to access your account and synced airline baggage allowance.'}
        </Text>

        <View style={styles.featureSurface}>
          <Text style={styles.featureTitle}>
            {isSignUpMode
              ? 'Instant One-Tap Google Account Registration'
              : 'Cloud Sync & Real-Time SAM 3.1 Manifest'}
          </Text>
          <Text style={styles.featureBody}>
            Your scanned gear weights, live airline PNR bookings, and personal bag shifts stay synced securely with Google Sign-In.
          </Text>
        </View>
      </View>

      <View style={styles.actionsColumn}>
        <TouchableOpacity
          style={styles.googleBtn}
          onPress={handleGoogleLogin}
          activeOpacity={0.85}
        >
          <Icon name="google-plus" height="22" width="22" />
          <Text style={styles.googleBtnText}>Google Sign in</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.facebookBtn}
          onPress={handleFacebookLogin}
          activeOpacity={0.85}
        >
          <Icon name="facebook" height="22" width="22" color="#FFFFFF" />
          <Text style={styles.facebookBtnText}>Facebook Sign in</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.guestBtn}
          onPress={onContinueAsGuest}
          activeOpacity={0.85}
        >
          <Text style={styles.guestBtnText}>Continue As Guest</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: FB_THEME.feedGrayBg,
    justifyContent: 'space-between',
    paddingHorizontal: 24,
    paddingVertical: 24,
  },
  topRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  modeTogglePill: {
    flexDirection: 'row',
    backgroundColor: FB_THEME.cardWhite,
    borderRadius: 999,
    padding: 4,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  modeTab: {
    paddingHorizontal: 18,
    paddingVertical: 8,
    borderRadius: 999,
  },
  modeTabActive: {
    backgroundColor: FB_THEME.facebookBlue,
  },
  modeTabText: {
    fontSize: 13,
    fontWeight: '700',
    color: FB_THEME.textSecondary,
  },
  modeTabTextActive: {
    color: FB_THEME.cardWhite,
  },
  card: {
    backgroundColor: FB_THEME.cardWhite,
    borderRadius: 20,
    padding: 24,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  kicker: {
    fontSize: 11,
    fontWeight: '700',
    color: FB_THEME.facebookBlue,
    marginBottom: 8,
  },
  title: {
    fontSize: 28,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
    lineHeight: 36,
    marginBottom: 10,
  },
  subtitle: {
    fontSize: 14,
    color: FB_THEME.textSecondary,
    lineHeight: 20,
    marginBottom: 18,
  },
  featureSurface: {
    backgroundColor: FB_THEME.surfaceLight,
    borderRadius: 14,
    padding: 16,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  featureTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: FB_THEME.textPrimary,
    marginBottom: 6,
  },
  featureBody: {
    fontSize: 13,
    color: FB_THEME.textSecondary,
    lineHeight: 19,
  },
  actionsColumn: {
    gap: 12,
  },
  googleBtn: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 10,
    backgroundColor: FB_THEME.cardWhite,
    borderRadius: 14,
    paddingVertical: 16,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  googleBtnText: {
    color: FB_THEME.textPrimary,
    fontSize: 16,
    fontWeight: '700',
  },
  facebookBtn: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 10,
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 14,
    paddingVertical: 16,
  },
  facebookBtnText: {
    color: FB_THEME.cardWhite,
    fontSize: 16,
    fontWeight: '700',
  },
  guestBtn: {
    backgroundColor: FB_THEME.surfaceLight,
    borderRadius: 14,
    paddingVertical: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  guestBtnText: {
    color: FB_THEME.textSecondary,
    fontSize: 15,
    fontWeight: '600',
  },
});
