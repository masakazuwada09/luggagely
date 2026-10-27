import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { FB_THEME, StudioTab } from '../types/baggage';

interface StickyBottomNavBarProps {
  selectedTab: StudioTab;
  isScanning: boolean;
  onSelectTab: (tab: StudioTab) => void;
  onCameraClick: () => void;
}

export const StickyBottomNavBar: React.FC<StickyBottomNavBarProps> = ({
  selectedTab,
  isScanning,
  onSelectTab,
  onCameraClick,
}) => {
  const renderNavItem = (tab: StudioTab, label: string, badgeSymbol: string) => {
    const isSelected = selectedTab === tab;
    return (
      <TouchableOpacity
        key={tab}
        style={styles.navItem}
        activeOpacity={0.75}
        onPress={() => onSelectTab(tab)}
      >
        <Text
          style={[
            styles.navIcon,
            { color: isSelected ? FB_THEME.facebookBlue : FB_THEME.textSecondary },
          ]}
        >
          {badgeSymbol}
        </Text>
        <Text
          style={[
            styles.navLabel,
            {
              color: isSelected ? FB_THEME.facebookBlue : FB_THEME.textSecondary,
              fontWeight: isSelected ? '700' : '500',
            },
          ]}
        >
          {label}
        </Text>
      </TouchableOpacity>
    );
  };

  return (
    <View style={styles.outerContainer}>
      <View style={styles.barSurface}>
        <View style={styles.sideGroup}>
          {renderNavItem('DASHBOARD', 'Dashboard', '⊞')}
          {renderNavItem('MANIFEST', 'Manifest', '☰')}
        </View>

        <View style={styles.centerSlot} />

        <View style={styles.sideGroup}>
          {renderNavItem('SETTINGS', 'Settings', '⚙')}
          {renderNavItem('PROFILE', 'Profile', '●')}
        </View>
      </View>

      <TouchableOpacity
        style={[
          styles.cameraFab,
          {
            backgroundColor: isScanning ? FB_THEME.emeraldSafe : FB_THEME.facebookBlue,
          },
        ]}
        activeOpacity={0.85}
        onPress={onCameraClick}
      >
        <Text style={styles.cameraFabIcon}>📷</Text>
        <Text style={styles.cameraFabLabel}>{isScanning ? 'SCAN' : 'CAM'}</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  outerContainer: {
    position: 'relative',
    backgroundColor: FB_THEME.cardWhite,
    borderTopWidth: 1,
    borderTopColor: FB_THEME.borderLight,
    paddingBottom: 12,
  },
  barSurface: {
    height: 64,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
    backgroundColor: FB_THEME.cardWhite,
  },
  sideGroup: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
  },
  centerSlot: {
    width: 76,
  },
  navItem: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 6,
    paddingHorizontal: 10,
  },
  navIcon: {
    fontSize: 18,
    marginBottom: 2,
  },
  navLabel: {
    fontSize: 11,
  },
  cameraFab: {
    position: 'absolute',
    top: -18,
    alignSelf: 'center',
    width: 64,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
    borderColor: FB_THEME.cardWhite,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.18,
    shadowRadius: 8,
    elevation: 6,
  },
  cameraFabIcon: {
    fontSize: 20,
    color: FB_THEME.cardWhite,
  },
  cameraFabLabel: {
    fontSize: 9,
    fontWeight: '800',
    color: FB_THEME.cardWhite,
    letterSpacing: 0.5,
  },
});
