import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
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
  const handleGoogleLogin = () => {
    onSignInSuccess({
      uid: 'ios-google-user',
      displayName: 'Masakazu Wada',
      email: 'masakazuwada09@gmail.com',
      photoUrl: 'https://lh3.googleusercontent.com/a/default-user=s96-c',
    });
  };

  return (
    <View style={styles.container}>
      <View style={styles.card}>
        <Text style={styles.title}>Ready to keep track of your luggage?</Text>
        <Text style={styles.subtitle}>
          Sign in with Google on iOS or Android to sync your baggage manifest across devices.
        </Text>

        <TouchableOpacity
          style={styles.googleBtn}
          onPress={handleGoogleLogin}
          activeOpacity={0.85}
        >
          <Text style={styles.googleBtnText}>Continue with Google</Text>
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
    justifyContent: 'center',
    padding: 20,
  },
  card: {
    backgroundColor: FB_THEME.cardWhite,
    borderRadius: 16,
    padding: 24,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  title: {
    fontSize: 26,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 14,
    color: FB_THEME.textSecondary,
    lineHeight: 20,
    marginBottom: 24,
  },
  googleBtn: {
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 10,
    paddingVertical: 15,
    alignItems: 'center',
    marginBottom: 12,
  },
  googleBtnText: {
    color: FB_THEME.cardWhite,
    fontSize: 15,
    fontWeight: '700',
  },
  guestBtn: {
    backgroundColor: FB_THEME.chipGray,
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
  },
  guestBtnText: {
    color: FB_THEME.textPrimary,
    fontSize: 15,
    fontWeight: '600',
  },
});
