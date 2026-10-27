import React, { useState } from 'react';
import { View, Text, TouchableOpacity, Image, StyleSheet, ActivityIndicator } from 'react-native';
import { GoogleSignin } from '@react-native-google-signin/google-signin';
import { getAuth, GoogleAuthProvider, signInWithCredential } from 'firebase/auth';
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

const CIRCLING_OFFSETS = [
  { top: 0, left: 108 },
  { top: 54, left: 200 },
  { top: 162, left: 200 },
  { top: 216, left: 108 },
  { top: 162, left: 16 },
  { top: 54, left: 16 },
];

export const AuthScreen: React.FC<AuthScreenProps> = ({
  onSignInSuccess,
  onContinueAsGuest,
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  const performRealOAuthSignIn = async () => {
    setIsLoading(true);
    setAuthError(null);
    try {
      await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });
      const signInResponse = await GoogleSignin.signIn();
      const idToken = (signInResponse as any)?.data?.idToken ?? (signInResponse as any)?.idToken;
      if (idToken) {
        const auth = getAuth();
        const credential = GoogleAuthProvider.credential(idToken);
        const userCredential = await signInWithCredential(auth, credential);
        const fbUser = userCredential.user;
        onSignInSuccess({
          uid: fbUser.uid,
          displayName:
            fbUser.displayName ||
            (fbUser.email ? fbUser.email.split('@')[0] : 'Signed-In Traveler'),
          email: fbUser.email || '',
          photoUrl: fbUser.photoURL,
        });
        return;
      }

      const profileUser = (signInResponse as any)?.data?.user ?? (signInResponse as any)?.user;
      if (profileUser?.email) {
        onSignInSuccess({
          uid: profileUser.id || `oauth-${Date.now()}`,
          displayName: profileUser.name || profileUser.email.split('@')[0],
          email: profileUser.email,
          photoUrl: profileUser.photo || null,
        });
      } else {
        setAuthError('Sign-in was cancelled or returned no account profile.');
      }
    } catch (err: any) {
      setAuthError(err?.message || 'Unable to complete sign-in. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.heroCenter}>
        <View style={styles.orbitContainer}>
          {CIRCLING_OFFSETS.map((pos, idx) => (
            <View
              key={idx}
              style={[styles.orbitItem, { top: pos.top, left: pos.left }]}
            />
          ))}
          <Image
            source={require('../../public/assets/aistudio/Luggagely_cc4b88fe.png')}
            style={styles.centeredLogo}
            resizeMode="contain"
          />
        </View>

        <View style={styles.cloudSyncBlock}>
          <Text style={styles.cloudSyncTitle}>
            Cloud Sync & Real-Time SAM 3.1 Manifest
          </Text>
          <Text style={styles.cloudSyncBody}>
            Your scanned gear weights, live airline PNR bookings, and personal bag shifts stay synced securely with Google Sign-In.
          </Text>
          {authError ? <Text style={styles.errorText}>{authError}</Text> : null}
        </View>
      </View>

      <View style={styles.actionsColumn}>
        <TouchableOpacity
          style={styles.googleBtn}
          onPress={performRealOAuthSignIn}
          disabled={isLoading}
          activeOpacity={0.85}
        >
          {isLoading ? (
            <ActivityIndicator color={FB_THEME.facebookBlue} />
          ) : (
            <>
              <Icon name="google-plus" height="22" width="22" />
              <Text style={styles.googleBtnText}>Google Sign in</Text>
            </>
          )}
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.facebookBtn}
          onPress={performRealOAuthSignIn}
          disabled={isLoading}
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
    paddingVertical: 32,
  },
  heroCenter: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  orbitContainer: {
    width: 260,
    height: 260,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
  },
  orbitItem: {
    position: 'absolute',
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: FB_THEME.cardWhite,
    opacity: 0.35,
  },
  centeredLogo: {
    height: 124,
    width: 88,
  },
  cloudSyncBlock: {
    alignItems: 'center',
    paddingHorizontal: 12,
  },
  cloudSyncTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: FB_THEME.textPrimary,
    textAlign: 'center',
    marginBottom: 8,
  },
  cloudSyncBody: {
    fontSize: 14,
    color: FB_THEME.textSecondary,
    lineHeight: 20,
    textAlign: 'center',
  },
  errorText: {
    marginTop: 10,
    fontSize: 13,
    color: FB_THEME.crimsonAlert,
    textAlign: 'center',
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
  },
  guestBtnText: {
    color: FB_THEME.textSecondary,
    fontSize: 15,
    fontWeight: '600',
  },
});
